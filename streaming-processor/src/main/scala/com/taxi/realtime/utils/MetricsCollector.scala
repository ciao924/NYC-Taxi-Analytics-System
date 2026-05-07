package com.taxi.realtime.utils

import org.apache.flink.metrics.{Counter, Gauge, Meter, MetricGroup}

object MetricsCollector {
  private var totalRecordsCounter: Counter = _
  private var filteredRecordsCounter: Counter = _
  private var writeSuccessCounter: Counter = _
  private var writeFailureCounter: Counter = _
  private var throughputMeter: Meter = _
  private var filterRateGauge: Gauge[Double] = _
  private var consumerLatencyCounter: Counter = _
  private var consumerLatencyGauge: Gauge[Double] = _
  private var dataQualityPassCounter: Counter = _
  private var dataQualityFailCounter: Counter = _
  private var dataQualityRateGauge: Gauge[Double] = _
  private var totalLatency: Long = 0
  private var latencyCount: Long = 0

  def initialize(metricGroup: MetricGroup): Unit = {
    // 初始化计数器
    totalRecordsCounter = metricGroup.counter("totalRecords")
    filteredRecordsCounter = metricGroup.counter("filteredRecords")
    writeSuccessCounter = metricGroup.counter("writeSuccess")
    writeFailureCounter = metricGroup.counter("writeFailure")
    consumerLatencyCounter = metricGroup.counter("consumerLatency")
    dataQualityPassCounter = metricGroup.counter("dataQualityPass")
    dataQualityFailCounter = metricGroup.counter("dataQualityFail")

    // 初始化吞吐量仪表
    throughputMeter = metricGroup.meter("throughput", new Meter {
      private var count: Long = 0
      private var lastTime: Long = System.currentTimeMillis()

      override def markEvent(): Unit = markEvent(1)

      override def markEvent(n: Long): Unit = {
        count += n
      }

      override def getRate: Double = {
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastTime
        if (elapsed == 0) 0.0
        else {
          val rate = (count * 1000.0) / elapsed
          count = 0
          lastTime = currentTime
          rate
        }
      }

      override def getCount: Long = count
    })

    // 初始化过滤率仪表
    filterRateGauge = metricGroup.gauge[Double, Gauge[Double]]("filterRate", new Gauge[Double] {
      override def getValue: Double = {
        val total = totalRecordsCounter.getCount
        if (total == 0) 0.0
        else {
          val filtered = filteredRecordsCounter.getCount
          (filtered.toDouble / total) * 100
        }
      }
    })

    // 初始化消费延迟仪表
    consumerLatencyGauge = metricGroup.gauge[Double, Gauge[Double]]("consumerLatency", new Gauge[Double] {
      override def getValue: Double = {
        if (latencyCount == 0) 0.0
        else totalLatency.toDouble / latencyCount
      }
    })

    // 初始化数据质量率仪表
    dataQualityRateGauge = metricGroup.gauge[Double, Gauge[Double]]("dataQualityRate", new Gauge[Double] {
      override def getValue: Double = {
        val total = dataQualityPassCounter.getCount + dataQualityFailCounter.getCount
        if (total == 0) 0.0
        else (dataQualityPassCounter.getCount.toDouble / total) * 100
      }
    })

    LoggerUtil.info("Metrics initialized successfully")
  }

  def initialize(): Unit = {
    // 无参数初始化，不注册指标
    LoggerUtil.info("Metrics initialized without metric group")
  }

  def incrementTotalRecords(): Unit = {
    if (totalRecordsCounter != null) {
      totalRecordsCounter.inc()
    }
  }

  def incrementFilteredRecords(): Unit = {
    if (filteredRecordsCounter != null) {
      filteredRecordsCounter.inc()
    }
  }

  def incrementWriteSuccess(): Unit = {
    if (writeSuccessCounter != null) {
      writeSuccessCounter.inc()
    }
  }

  def incrementWriteFailure(): Unit = {
    if (writeFailureCounter != null) {
      writeFailureCounter.inc()
    }
  }

  def markThroughput(): Unit = {
    if (throughputMeter != null) {
      throughputMeter.markEvent()
    }
  }

  def markThroughput(n: Long): Unit = {
    if (throughputMeter != null) {
      throughputMeter.markEvent(n)
    }
  }

  def getFilterRate: Double = {
    if (filterRateGauge != null) filterRateGauge.getValue else 0.0
  }

  def getTotalRecords: Long = {
    if (totalRecordsCounter != null) totalRecordsCounter.getCount else 0
  }

  def getFilteredRecords: Long = {
    if (filteredRecordsCounter != null) filteredRecordsCounter.getCount else 0
  }

  def getWriteSuccess: Long = {
    if (writeSuccessCounter != null) writeSuccessCounter.getCount else 0
  }

  def getWriteFailure: Long = {
    if (writeFailureCounter != null) writeFailureCounter.getCount else 0
  }

  def getThroughput: Double = {
    if (throughputMeter != null) throughputMeter.getRate else 0.0
  }

  def recordConsumerLatency(eventTime: Long): Unit = {
    val currentTime = System.currentTimeMillis()
    val latency = currentTime - eventTime
    if (consumerLatencyCounter != null) {
      consumerLatencyCounter.inc(latency)
    }
    totalLatency += latency
    latencyCount += 1
  }

  def getConsumerLatency: Double = {
    if (latencyCount == 0) 0.0
    else totalLatency.toDouble / latencyCount
  }

  def incrementDataQualityPass(): Unit = {
    if (dataQualityPassCounter != null) {
      dataQualityPassCounter.inc()
    }
  }

  def incrementDataQualityFail(): Unit = {
    if (dataQualityFailCounter != null) {
      dataQualityFailCounter.inc()
    }
  }

  def getDataQualityRate: Double = {
    if (dataQualityRateGauge != null) dataQualityRateGauge.getValue else 0.0
  }

  def getDataQualityPass: Long = {
    if (dataQualityPassCounter != null) dataQualityPassCounter.getCount else 0
  }

  def getDataQualityFail: Long = {
    if (dataQualityFailCounter != null) dataQualityFailCounter.getCount else 0
  }
}
