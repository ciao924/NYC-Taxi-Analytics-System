package com.taxi.etl.common

import org.apache.spark.scheduler._
import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable

class SparkListenerMetrics extends SparkListener {

  private val logger = LoggerFactory.getLogger(getClass)

  private val jobStats = new ConcurrentHashMap[Int, (Long, String)]()
  private val stageStats = new ConcurrentHashMap[Int, (String, Long, String, Int)]()
  private val skewHintCount = new AtomicInteger(0)
  private val taskDurations = new ConcurrentHashMap[Int, mutable.ArrayBuffer[Long]]()

  override def onJobStart(jobStart: SparkListenerJobStart): Unit = {
    jobStats.put(jobStart.jobId, (System.currentTimeMillis(), "RUNNING"))
    logger.info(s"[Job] 开始: id=${jobStart.jobId}")
  }

  override def onJobEnd(jobEnd: SparkListenerJobEnd): Unit = {
    val stats = jobStats.get(jobEnd.jobId)
    if (stats != null) {
      val (startTime, _) = stats
      val endTime = System.currentTimeMillis()
      val duration = (endTime - startTime) / 1000.0

      // 使用类名判断，避免访问 private[spark] 的 JobFailed
      val status = jobEnd.jobResult.getClass.getSimpleName match {
        case "JobSucceeded" => "SUCCESS"
        case "JobFailed" => "FAILED"
        case _ => "UNKNOWN"
      }

      jobStats.put(jobEnd.jobId, (startTime, status))
      logger.info(s"[Job] 结束: id=${jobEnd.jobId}, status=$status, duration=${duration}s")
      MetricsCollector.record("spark.job.duration", duration, Map("jobId" -> jobEnd.jobId.toString))
    }
  }

  override def onStageSubmitted(stageSubmitted: SparkListenerStageSubmitted): Unit = {
    val stageId = stageSubmitted.stageInfo.stageId
    val stageName = stageSubmitted.stageInfo.name
    val startTime = stageSubmitted.stageInfo.submissionTime.getOrElse(System.currentTimeMillis())
    stageStats.put(stageId, (stageName, startTime, "RUNNING", 0))
    logger.info(s"[Stage] 开始: $stageName")
  }

  override def onStageCompleted(stageCompleted: SparkListenerStageCompleted): Unit = {
    val info = stageCompleted.stageInfo
    val stageId = info.stageId
    val stats = stageStats.get(stageId)
    if (stats != null) {
      val (name, startTime, _, _) = stats
      val endTime = info.completionTime.getOrElse(System.currentTimeMillis())
      val duration = (endTime - startTime) / 1000.0
      val status = info.failureReason.map(_ => "FAILED").getOrElse("SUCCESS")
      val numTasks = info.numTasks
      stageStats.put(stageId, (name, startTime, status, numTasks))
      logger.info(s"[Stage] 完成: $name, duration=${duration}s, tasks=$numTasks, status=$status")
      MetricsCollector.record("spark.stage.duration", duration, Map("stageId" -> stageId.toString))
    }
  }

  override def onTaskEnd(taskEnd: SparkListenerTaskEnd): Unit = {
    val duration = taskEnd.taskInfo.duration
    val stageId = taskEnd.stageId

    val durations = taskDurations.computeIfAbsent(stageId, _ => mutable.ArrayBuffer.empty[Long])
    durations.synchronized {
      durations += duration
    }

    if (duration > 60000) {
      skewHintCount.incrementAndGet()
      logger.warn(s"[倾斜检测] Stage $stageId, Task ${taskEnd.taskInfo.taskId}, duration=${duration}ms")
      MetricsCollector.record("spark.task.skew_hint", 1.0, Map("stageId" -> stageId.toString))
    }
  }

  private def getSkewedStages(ratioThreshold: Double = 3.0): Seq[(Int, Double, Long, Long)] = {
    val result = mutable.ListBuffer[(Int, Double, Long, Long)]()

    val iterator = taskDurations.entrySet().iterator()
    while (iterator.hasNext) {
      val entry = iterator.next()
      val stageId = entry.getKey
      val durations = entry.getValue
      if (durations.size > 1) {
        val maxDuration = durations.max
        val avgDuration = durations.sum / durations.size
        if (maxDuration > avgDuration * ratioThreshold) {
          val ratio = maxDuration.toDouble / avgDuration
          result += ((stageId, ratio, maxDuration, avgDuration))
        }
      }
    }

    result.toSeq.sortBy(-_._2)
  }

  def getSkewHintCount: Int = skewHintCount.get()

  def generateReport(): String = {
    val sb = new StringBuilder()
    sb.append("\n" + "=" * 80 + "\n📊 Spark 执行监控报告\n" + "=" * 80)

    sb.append(s"\n✅ Jobs: ${jobStats.size()}")
    val jobIterator = jobStats.entrySet().iterator()
    while (jobIterator.hasNext) {
      val entry = jobIterator.next()
      val jobId = entry.getKey
      val (_, status) = entry.getValue
      sb.append(s"\n  - Job $jobId: $status")
    }

    sb.append(s"\n✅ Stages: ${stageStats.size()}")
    val stageIterator = stageStats.entrySet().iterator()
    while (stageIterator.hasNext) {
      val entry = stageIterator.next()
      val stageId = entry.getKey
      val (name, _, status, numTasks) = entry.getValue
      sb.append(s"\n  - $name: $status, tasks=$numTasks")
    }

    val skewedStages = getSkewedStages()
    if (skewedStages.nonEmpty) {
      sb.append("\n⚠️ 倾斜检测:")
      skewedStages.foreach { case (stageId, ratio, maxDur, avgDur) =>
        sb.append(s"\n  - Stage $stageId: 倾斜比 ${f"$ratio%.2f"} (max=${maxDur}ms, avg=${avgDur}ms)")
      }
    } else {
      sb.append("\n✅ 未检测到严重倾斜")
    }

    sb.append("\n" + "=" * 80)
    sb.toString()
  }

  def printReport(): Unit = logger.info(generateReport())
}

object SparkListenerMetrics {

  @volatile private var instance: Option[SparkListenerMetrics] = None

  def register(spark: SparkSession): SparkListenerMetrics = {
    val listener = new SparkListenerMetrics()
    spark.sparkContext.addSparkListener(listener)
    instance = Some(listener)
    listener
  }

  def get: Option[SparkListenerMetrics] = instance

  def getAndPrintReport(): Unit = {
    instance.foreach(_.printReport())
  }
}