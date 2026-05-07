package com.taxi.realtime

/**
 * Realtime ODS Job - 原始明细落地到 HDFS ODS 层
 * 
 * 与离线模块 ODS 层对齐：
 * - 数据格式：ORC + SNAPPY 压缩
 * - 分区格式：year=YYYY/month=MM
 * - 数据路径：与离线模块一致
 * 
 * 注意: 数据写入 HDFS 后，需定期执行 MSCK REPAIR TABLE 同步 Hive 分区元数据
 */

import com.taxi.realtime.config.ConfigManager
import com.taxi.realtime.model.GreenTripRecord
import com.taxi.realtime.serializer.GreenTripRecordVectorizer
import com.taxi.realtime.sink.{DeadLetterSinkBuilder, GreenTripRecordBucketAssigner}
import com.taxi.realtime.source.KafkaSourceBuilder
import com.taxi.realtime.utils.LoggerUtil
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.core.fs.Path
import org.apache.flink.connector.file.sink.FileSink
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy
import org.apache.flink.orc.writer.OrcBulkWriterFactory
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.util.Collector

import java.time.Duration
import java.sql.Timestamp
import java.util.Calendar

import com.taxi.realtime.model.ErrorRecordJava
import org.apache.hadoop.conf.Configuration

object RealtimeOdsJob {

  def main(args: Array[String]): Unit = {
    try {
      ConfigManager.init(args)
      val env = StreamExecutionEnvironment.getExecutionEnvironment

      configureEnvironment(env)

      val source = env.fromSource(
        KafkaSourceBuilder.build(ConfigManager.getKafkaConsumerGroupId),
        WatermarkStrategy.noWatermarks(),
        "Kafka Source"
      )

      val jsonParser = new JsonParseFunction()

      val parsedStream = source
        .process(jsonParser)
        .name("JSON Parsing")
        .uid("json-parsing-ods")

      val parsingErrors = parsedStream.getSideOutput(jsonParser.parsingErrors)
      parsingErrors.addSink(DeadLetterSinkBuilder.build())
        .name("ODS Dead Letter Sink")
        .uid("ods-dead-letter-sink")

      val invalidTimestampErrors = new OutputTag[ErrorRecordJava]("invalid-timestamp") {
        override def getTypeInfo: TypeInformation[ErrorRecordJava] =
          TypeInformation.of(classOf[ErrorRecordJava])
      }

      val watermarkStrategy = WatermarkStrategy
        .forBoundedOutOfOrderness[GreenTripRecord](Duration.ofMinutes(2))
        .withIdleness(Duration.ofMinutes(5))
        .withTimestampAssigner(new SerializableTimestampAssigner[GreenTripRecord] {
          override def extractTimestamp(record: GreenTripRecord, recordTimestamp: Long): Long = {
            record.pickupDatetime.map(_.getTime).getOrElse(System.currentTimeMillis())
          }
        })

      val odsStream = parsedStream
        .filter(_ != null)
        .process(new ProcessFunction[GreenTripRecord, GreenTripRecord] {
          override def processElement(record: GreenTripRecord, ctx: ProcessFunction[GreenTripRecord, GreenTripRecord]#Context, out: Collector[GreenTripRecord]): Unit = {
            record.pickupDatetime match {
              case Some(ts) =>
                val cal = Calendar.getInstance()
                cal.setTimeInMillis(ts.getTime)
                val year = cal.get(Calendar.YEAR)
                if (year >= 2000 && year <= 2100) {
                  out.collect(record)
                } else {
                  ctx.output(invalidTimestampErrors, new ErrorRecordJava(record.toString, "Invalid year: " + year))
                  LoggerUtil.warn(s"Dropping record with invalid year $year: ${record.toString.take(200)}")
                }

              case None =>
                ctx.output(invalidTimestampErrors, new ErrorRecordJava(record.toString, "Missing pickupDatetime"))
                LoggerUtil.warn(s"Dropping record with missing pickupDatetime: ${record.toString.take(200)}")
            }
          }
        })
        .name("Data Cleaning")
        .uid("ods-data-cleaning")

      val invalidTimestamps = odsStream.getSideOutput(invalidTimestampErrors)
      invalidTimestamps.addSink(DeadLetterSinkBuilder.build())
        .name("Invalid Timestamp Dead Letter Sink")
        .uid("invalid-timestamp-dead-letter-sink")

      val eventTimeStream = odsStream
        .assignTimestampsAndWatermarks(watermarkStrategy)
        .name("ODS Stream with Event Time")
        .uid("ods-stream")

      val greenStream = eventTimeStream.filter(_.taxiType.contains("green"))
        .name("Green Taxi Stream")
        .uid("green-stream")

      val yellowStream = eventTimeStream.filter(_.taxiType.contains("yellow"))
        .name("Yellow Taxi Stream")
        .uid("yellow-stream")

      writeToHdfsWithOrcFileSink(greenStream, yellowStream)

      LoggerUtil.info("Starting RealtimeOdsJob")
      env.execute("RealtimeOdsJob")

    } catch {
      case e: Exception =>
        LoggerUtil.error("Failed to execute RealtimeOdsJob", e)
        throw new RuntimeException("Failed to execute job", e)
    }
  }

  private def writeToHdfsWithOrcFileSink(greenStream: org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator[GreenTripRecord], yellowStream: org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator[GreenTripRecord]): Unit = {
    val greenTripPath = ConfigManager.getOdsDataPath.replace("yellow", "green")
    val yellowTripPath = ConfigManager.getOdsDataPath.replace("green", "yellow")

    val hadoopConf = new Configuration()
    hadoopConf.set("orc.compress", "SNAPPY")
    hadoopConf.set("orc.stripe.size", "268435456")
    hadoopConf.set("orc.row.index.stride", "10000")

    val vectorizer = new GreenTripRecordVectorizer()
    val writerFactory = new OrcBulkWriterFactory[GreenTripRecord](vectorizer, hadoopConf)

    val greenOutputFileConfig = OutputFileConfig.builder()
      .withPartPrefix("green")
      .withPartSuffix(".orc")
      .build()

    val greenFileSink = FileSink
      .forBulkFormat(new Path(greenTripPath), writerFactory)
      .withBucketAssigner(new GreenTripRecordBucketAssigner())
      .withRollingPolicy(OnCheckpointRollingPolicy.build())
      .withOutputFileConfig(greenOutputFileConfig)
      .build()

    val yellowOutputFileConfig = OutputFileConfig.builder()
      .withPartPrefix("yellow")
      .withPartSuffix(".orc")
      .build()

    val yellowFileSink = FileSink
      .forBulkFormat(new Path(yellowTripPath), writerFactory)
      .withBucketAssigner(new GreenTripRecordBucketAssigner())
      .withRollingPolicy(OnCheckpointRollingPolicy.build())
      .withOutputFileConfig(yellowOutputFileConfig)
      .build()

    greenStream
      .sinkTo(greenFileSink)
      .name("ORC FileSink for Green Taxi ODS")
      .uid("green-ods-sink")

    yellowStream
      .sinkTo(yellowFileSink)
      .name("ORC FileSink for Yellow Taxi ODS")
      .uid("yellow-ods-sink")
  }

  private def configureEnvironment(env: StreamExecutionEnvironment): Unit = {
    val parallelism = ConfigManager.getFlinkParallelism
    env.setParallelism(parallelism)

    val checkpointPath = ConfigManager.getFlinkCheckpointStoragePath
    import org.apache.flink.runtime.state.hashmap.HashMapStateBackend
    env.setStateBackend(new HashMapStateBackend())
    env.getCheckpointConfig.setCheckpointStorage(checkpointPath)

    env.enableCheckpointing(120000, CheckpointingMode.EXACTLY_ONCE)
    env.getCheckpointConfig.setCheckpointTimeout(10 * 60 * 1000)
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(60000)
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

    LoggerUtil.info(s"ODS Job Environment configured: parallelism=$parallelism, checkpointInterval=120000ms, path=$checkpointPath")
  }
}
