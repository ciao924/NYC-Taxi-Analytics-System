package com.taxi.realtime

import com.taxi.realtime.config.ConfigManager
import com.taxi.realtime.model.{ErrorRecordJava, FeeComposition, GreenTripRecord, HotspotResult, OrderMetric, Trip}
import com.taxi.realtime.quality._
import com.taxi.realtime.sink.{AlertSinkBuilder, DeadLetterSinkBuilder, MysqlSinkFactory, QualitySinkBuilder}
import com.taxi.realtime.source.KafkaSourceBuilder
import com.taxi.realtime.utils.LoggerUtil
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.api.java.functions.KeySelector
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

import java.lang.Iterable
import java.util.Calendar

object RealtimeMetricsJob {

  private val CITY = "NYC"

  def main(args: Array[String]): Unit = {
    try {
      ConfigManager.init(args)
      
      val topics = ConfigManager.getKafkaTopics
      println("=== Topics from config: " + topics.mkString(","))
      LoggerUtil.info("=== Topics from config: " + topics.mkString(","))
      
      val env = StreamExecutionEnvironment.getExecutionEnvironment

      configureEnvironment(env)

      val source = env.fromSource(
        KafkaSourceBuilder.build(ConfigManager.getKafkaConsumerGroupId),
        WatermarkStrategy.noWatermarks(),
        "Kafka Source"
      )

      val jsonParser = new JsonParseFunction()
      val dataCleaner = new DataCleanFunction()

      val parsedStream = source
        .process(jsonParser)
        .name("JSON Parsing")
        .uid("json-parsing")

      val cleaned = parsedStream
        .filter(_ != null)
        .process(dataCleaner)
        .name("Data Cleaning")
        .uid("data-cleaning")

      val parsingErrors = parsedStream.getSideOutput(jsonParser.parsingErrors)
      val cleaningErrors = cleaned.getSideOutput(dataCleaner.cleaningErrors)

      val invalidTimestampErrors = new OutputTag[ErrorRecordJava]("invalid-timestamp") {
        override def getTypeInfo: TypeInformation[ErrorRecordJava] =
          TypeInformation.of(classOf[ErrorRecordJava])
      }
      val validatedStream = cleaned
        .process(new ProcessFunction[GreenTripRecord, GreenTripRecord] {
          override def processElement(record: GreenTripRecord, ctx: ProcessFunction[GreenTripRecord, GreenTripRecord]#Context, out: Collector[GreenTripRecord]): Unit = {
            record.pickupDatetime match {
              case Some(ts) =>
                val cal = Calendar.getInstance()
                cal.setTimeInMillis(ts.getTime)
                val year = cal.get(Calendar.YEAR)
                if (year >= 2000 && year <= 2100) out.collect(record)
                else ctx.output(invalidTimestampErrors, new ErrorRecordJava(record.toString, "Invalid year: " + year))
              case None =>
                ctx.output(invalidTimestampErrors, new ErrorRecordJava(record.toString, "Missing pickupDatetime"))
            }
          }
        })
        .name("Timestamp Validation")
        .uid("timestamp-validation")

      parsingErrors.union(cleaningErrors).union(validatedStream.getSideOutput(invalidTimestampErrors)).addSink(DeadLetterSinkBuilder.build())
        .name("Dead Letter Sink")
        .uid("dead-letter-sink")

      val tripStream = validatedStream
        .map(record => Trip(
          vendorId = record.vendorId.getOrElse(0).toString,
          pickupTime = record.pickupDatetime.map(_.getTime).getOrElse(System.currentTimeMillis()),
          dropoffTime = record.dropoffDatetime.map(_.getTime).getOrElse(System.currentTimeMillis()),
          passengerCount = record.passengerCount.getOrElse(0),
          tripDistance = record.tripDistance.getOrElse(0.0),
          pickupZone = record.puLocationId.getOrElse(0).toString,
          dropoffZone = record.doLocationId.getOrElse(0).toString,
          fareAmount = record.fareAmount.getOrElse(0.0),
          paymentType = record.paymentType.getOrElse(1).toString,
          taxiType = record.taxiType.getOrElse("green")
        ))
        .name("Convert to Trip Model")
        .uid("convert-trip")

      val orderMetrics = tripStream
        .keyBy(new KeySelector[Trip, (String, String)] {
          override def getKey(t: Trip): (String, String) = (t.pickupZone, t.paymentType)
        })
        .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
        .aggregate(
          new OrderMetricsAggregateFunction(),
          new OrderMetricsWindowFunction()
        )
        .name("Order Metrics Aggregation")
        .uid("order-metrics-agg")

      val hotspotTopN = tripStream
        .keyBy( (t: Trip) => t.pickupZone )
        .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
        .aggregate(new ZoneCountAggregateFunction(), new ZoneCountWindowFunction())
        .name("Zone Count Aggregation")
        .uid("zone-count-agg")
        .filter(_._2.nonEmpty)
        .keyBy(new KeySelector[(Long, String, Long), Long] {
          override def getKey(tuple: (Long, String, Long)): Long = tuple._1
        })
        .process(new HotspotTopNProcessFunction(10))
        .name("Hotspot TopN")
        .uid("hotspot-topn")

      val feeComposition = tripStream
        .keyBy( (t: Trip) => t.paymentType )
        .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
        .aggregate(new FeeAmountAggregateFunction(), new FeeCompositionWindowFunction())
        .name("Fee Composition")
        .uid("fee-composition")

      orderMetrics.addSink(MysqlSinkFactory.buildOrderMetricsSink())
        .name("Order Metrics MySQL Sink")
        .uid("order-metrics-sink")

      hotspotTopN.addSink(MysqlSinkFactory.buildHotspotSink())
        .name("Hotspot MySQL Sink")
        .uid("hotspot-sink")

      feeComposition.addSink(MysqlSinkFactory.buildFeeCompositionSink())
        .name("Fee Composition MySQL Sink")
        .uid("fee-composition-sink")

      if (ConfigManager.isDataQualityEnabled) {
        val normalStream = validatedStream.map((_: GreenTripRecord) => NormalEvent(): QualityEvent)
        val parseErrorStream = parsingErrors.map((_: ErrorRecordJava) => ParseErrorEvent(): QualityEvent)
        val cleanErrorStream = cleaningErrors.map((_: ErrorRecordJava) => CleanErrorEvent(): QualityEvent)
        val timestampErrorStream = validatedStream.getSideOutput(invalidTimestampErrors).map((_: ErrorRecordJava) => TimestampErrorEvent(): QualityEvent)

        val allEvents = normalStream.union(parseErrorStream, cleanErrorStream, timestampErrorStream)
          .map(event => ("quality_key", event))
          .returns(new TypeHint[(String, QualityEvent)] {})

        val qualityResults = allEvents
          .keyBy(_._1)
          .window(TumblingProcessingTimeWindows.of(Time.minutes(5)))
          .aggregate(new QualityAggregateFunction(), new QualityWindowFunction())
          .name("Quality Metrics Aggregation")
          .uid("quality-metrics-agg")

        qualityResults.addSink(QualitySinkBuilder.build())
          .name("Quality Metrics Sink")
          .uid("quality-metrics-sink")

        val alertStream = qualityResults
          .keyBy(_ => "alert_key")
          .process(new AlertDetector())
          .name("Alert Detection")
          .uid("alert-detection")

        alertStream.addSink(AlertSinkBuilder.build())
          .name("Alert History Sink")
          .uid("alert-history-sink")
      }

      LoggerUtil.info("Starting RealtimeMetricsJob")
      env.execute("RealtimeMetricsJob")

    } catch {
      case e: Exception =>
        LoggerUtil.error("Failed to execute RealtimeMetricsJob", e)
        throw new RuntimeException("Failed to execute job", e)
    }
  }

  private def configureEnvironment(env: StreamExecutionEnvironment): Unit = {
    val parallelism = ConfigManager.getFlinkParallelism
    env.setParallelism(parallelism)

    val checkpointPath = ConfigManager.getFlinkCheckpointStoragePath
    env.setStateBackend(new EmbeddedRocksDBStateBackend())
    env.getCheckpointConfig.setCheckpointStorage(checkpointPath)

    val checkpointInterval = ConfigManager.getFlinkCheckpointInterval
    val checkpointTimeout = ConfigManager.getFlinkCheckpointTimeout
    env.enableCheckpointing(checkpointInterval, CheckpointingMode.EXACTLY_ONCE)
    env.getCheckpointConfig.setCheckpointTimeout(checkpointTimeout)
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(30000)
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1)
    env.getCheckpointConfig.setTolerableCheckpointFailureNumber(3)
    env.getCheckpointConfig.setExternalizedCheckpointCleanup(
      org.apache.flink.streaming.api.environment.CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION
    )

    import org.apache.flink.api.common.restartstrategy.RestartStrategies
    import org.apache.flink.api.common.time.Time
    env.setRestartStrategy(
      RestartStrategies.fixedDelayRestart(
        3,
        Time.seconds(10)
      )
    )

    LoggerUtil.info(s"Environment configured: parallelism=$parallelism, checkpointInterval=${checkpointInterval}ms, path=$checkpointPath")
  }
}

class OrderMetricsAggregateFunction extends AggregateFunction[Trip, (Long, Double), (Long, Double)] {
  override def createAccumulator(): (Long, Double) = (0L, 0.0)
  override def add(t: Trip, acc: (Long, Double)): (Long, Double) =
    (acc._1 + 1, acc._2 + t.fareAmount)
  override def getResult(acc: (Long, Double)): (Long, Double) = acc
  override def merge(a: (Long, Double), b: (Long, Double)): (Long, Double) =
    (a._1 + b._1, a._2 + b._2)
}

class OrderMetricsWindowFunction extends ProcessWindowFunction[(Long, Double), OrderMetric, (String, String), TimeWindow] {
  override def process(key: (String, String), context: ProcessWindowFunction[(Long, Double), OrderMetric, (String, String), TimeWindow]#Context, elements: java.lang.Iterable[(Long, Double)], out: Collector[OrderMetric]): Unit = {
    val (cnt, sum) = elements.iterator().next()
    out.collect(OrderMetric(
      windowStart = context.window.getStart,
      windowEnd = context.window.getEnd,
      city = "NYC",
      orderCount = cnt,
      totalFare = sum,
      avgFare = if (cnt > 0) sum / cnt else 0.0
    ))
  }
}

class ZoneCountAggregateFunction extends AggregateFunction[Trip, (String, Long), (String, Long)] {
  override def createAccumulator(): (String, Long) = ("", 0L)
  override def add(t: Trip, acc: (String, Long)): (String, Long) = (t.pickupZone, acc._2 + 1)
  override def getResult(acc: (String, Long)): (String, Long) = acc
  override def merge(a: (String, Long), b: (String, Long)): (String, Long) = (a._1, a._2 + b._2)
}

class ZoneCountWindowFunction extends ProcessWindowFunction[(String, Long), (Long, String, Long), String, TimeWindow] {
  override def process(key: String, context: ProcessWindowFunction[(String, Long), (Long, String, Long), String, TimeWindow]#Context, elements: java.lang.Iterable[(String, Long)], out: Collector[(Long, String, Long)]): Unit = {
    val iter = elements.iterator()
    while (iter.hasNext) {
      val (zone, cnt) = iter.next()
      out.collect((context.window.getEnd, zone, cnt))
    }
  }
}

class HotspotTopNProcessFunction(n: Int) extends KeyedProcessFunction[Long, (Long, String, Long), HotspotResult] {
  private var zoneCounts: ListState[(String, Long)] = _
  
  override def open(parameters: org.apache.flink.configuration.Configuration): Unit = {
    val stateDesc = new ListStateDescriptor[(String, Long)]("zoneCounts", createTypeInformation[(String, Long)])
    zoneCounts = getRuntimeContext.getListState(stateDesc)
  }
  
  override def processElement(value: (Long, String, Long), ctx: KeyedProcessFunction[Long, (Long, String, Long), HotspotResult]#Context, out: Collector[HotspotResult]): Unit = {
    val (windowEnd, zone, cnt) = value
    zoneCounts.add((zone, cnt))
    ctx.timerService().registerProcessingTimeTimer(windowEnd + 1)
  }
  
  override def onTimer(timestamp: Long, ctx: KeyedProcessFunction[Long, (Long, String, Long), HotspotResult]#OnTimerContext, out: Collector[HotspotResult]): Unit = {
    val windowEnd = ctx.getCurrentKey
    val windowStart = windowEnd - Time.minutes(5).toMilliseconds
    
    val list = scala.collection.mutable.ListBuffer[(String, Long)]()
    val iter = zoneCounts.get().iterator()
    while (iter.hasNext) {
      list += iter.next()
    }
    
    val sorted = list.sortBy(-_._2).take(n)
    sorted.zipWithIndex.foreach { case ((zone, cnt), idx) =>
      out.collect(HotspotResult(
        windowStart = windowStart,
        windowEnd = windowEnd,
        zone = zone,
        cnt = cnt,
        rank = idx + 1
      ))
    }
    
    zoneCounts.clear()
  }
}

class FeeAmountAggregateFunction extends AggregateFunction[Trip, Double, Double] {
  override def createAccumulator(): Double = 0.0
  override def add(t: Trip, acc: Double): Double = acc + t.fareAmount
  override def getResult(acc: Double): Double = acc
  override def merge(a: Double, b: Double): Double = a + b
}

class FeeCompositionWindowFunction extends ProcessWindowFunction[Double, FeeComposition, String, TimeWindow] {
  override def process(key: String, context: ProcessWindowFunction[Double, FeeComposition, String, TimeWindow]#Context, elements: java.lang.Iterable[Double], out: Collector[FeeComposition]): Unit = {
    val totalAmount = elements.iterator().next()
    out.collect(FeeComposition(
      windowStart = context.window.getStart,
      windowEnd = context.window.getEnd,
      paymentType = key,
      totalAmount = totalAmount
    ))
  }
}