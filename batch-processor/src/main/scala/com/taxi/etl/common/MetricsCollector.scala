package com.taxi.etl.common

import org.slf4j.LoggerFactory

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.DoubleAdder
import scala.collection.JavaConverters._

/**
 * 指标收集器（实例化版本）
 *
 * 原版本为全局单例（object MetricsCollector），多 Job 并发时会互相污染数据。
 *
 * 修复方案：
 *   - 改为 class，每个 Job 持有独立实例，天然隔离
 *   - 内部使用 ConcurrentHashMap + DoubleAdder 保证单实例内的线程安全
 *   - 伴生对象保留一个全局默认实例，兼容现有不传 metrics 参数的调用
 *
 * 推荐用法（Job 级隔离）：
 *   val metrics = new MetricsCollector()
 *   metrics.record("row_count", count)
 *   metrics.recordJobMetric("DWD", executionId, "final_count", count)
 *   logger.info(metrics.generateReport())
 *
 * 兼容用法（维持原有调用不变）：
 *   MetricsCollector.record("key", value)
 */
class MetricsCollector {

  private val logger = LoggerFactory.getLogger(getClass)
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  // key -> (sum, count)，使用 DoubleAdder 保证并发累加安全
  private val sumMap   = new ConcurrentHashMap[String, DoubleAdder]()
  private val countMap = new ConcurrentHashMap[String, DoubleAdder]()
  private val maxMap   = new ConcurrentHashMap[String, Double]()
  private val minMap   = new ConcurrentHashMap[String, Double]()
  private val lock     = new Object  // 仅用于 max/min 更新

  def record(metricName: String, value: Double, tags: Map[String, String] = Map.empty): Unit = {
    sumMap.computeIfAbsent(metricName, _ => new DoubleAdder()).add(value)
    countMap.computeIfAbsent(metricName, _ => new DoubleAdder()).add(1.0)

    // max/min 需要 CAS 语义，用 synchronized 保护（指标更新频率低，无性能问题）
    lock.synchronized {
      maxMap.merge(metricName, value, math.max)
      minMap.merge(metricName, value, math.min)
    }

    if (logger.isDebugEnabled) {
      val tagStr = if (tags.nonEmpty) s"|${tags.mkString(",")}" else ""
      logger.debug(s"METRIC|$metricName|$value$tagStr")
    }
  }

  /** 兼容原有三参数调用：record(service, metricName, value) */
  def record(service: String, metricName: String, value: Double): Unit = {
    record(s"${service}_$metricName", value, Map("service" -> service))
  }

  def recordJobMetric(jobName: String, executionId: String, metric: String, value: Double): Unit = {
    record(s"job.$jobName.$metric", value, Map("job" -> jobName, "executionId" -> executionId))
  }

  def generateReport(): String = {
    val sb = new StringBuilder()
    sb.append("\n" + "=" * 80 + "\n📊 指标收集报告\n" + "=" * 80)
    sb.append(s"\n报告时间: ${LocalDateTime.now().format(dateFormatter)}\n")

    val keys = sumMap.keys().asScala.toSeq.sorted
    if (keys.isEmpty) {
      sb.append("\n（暂无指标）")
    } else {
      keys.foreach { key =>
        val sum   = sumMap.get(key).sum()
        val cnt   = countMap.get(key).sum()
        val avg   = if (cnt > 0) sum / cnt else 0.0
        val max   = maxMap.getOrDefault(key, 0.0)
        val min   = minMap.getOrDefault(key, 0.0)
        sb.append(f"\n  $key: avg=$avg%.2f, max=$max%.2f, min=$min%.2f, count=${cnt.toLong}")
      }
    }

    sb.append("\n" + "=" * 80)
    sb.toString()
  }

  def reset(): Unit = {
    sumMap.clear()
    countMap.clear()
    lock.synchronized {
      maxMap.clear()
      minMap.clear()
    }
  }
}

/**
 * 伴生对象：维持全局默认实例，兼容现有代码中直接调用 MetricsCollector.record(...) 的写法。
 * 新代码建议 new MetricsCollector() 后注入使用。
 */
object MetricsCollector extends MetricsCollector
