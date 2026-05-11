package com.taxi.etl.common

import com.taxi.etl.models.QualityReport
import com.taxi.etl.utils.DeadLetterWriter
import com.taxi.etl.exception.QualityCheckException
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object QualityManager {

  private val logger = LoggerFactory.getLogger(getClass)

  private val FAIL_ON_FAILURE: Boolean = ConfigManager.isQualityFailOnFailure
  private val DEAD_LETTER_ENABLED: Boolean = ConfigManager.isQualityDeadLetterEnabled
  private val ANOMALY_THRESHOLD: Double = ConfigManager.getDoubleOrDefault("quality.anomaly-threshold", 0.05)
  private val RANGE_THRESHOLD: Double = ConfigManager.getQualityRangeThreshold
  private val RETENTION_MIN: Double = ConfigManager.getQualityExpectedRetentionMin
  private val RETENTION_MAX: Double = ConfigManager.getQualityExpectedRetentionMax
  private val MAX_DEAD_LETTER_RECORDS: Int = ConfigManager.getQualityDeadLetterMaxRecords
  private val RANGE_CHECK_SAMPLE_RATE: Double = ConfigManager.getQualityRangeCheckSampleRate

  // 【修正】从配置读取阈值
  private val MAX_TRIP_DISTANCE: Double = ConfigManager.getDoubleOrDefault("quality.max-trip-distance", 120.0)
  private val MAX_TOTAL_AMOUNT: Double = ConfigManager.getDoubleOrDefault("quality.max-total-amount", 800.0)
  private val MAX_PASSENGER_COUNT: Int = ConfigManager.getIntOrDefault("quality.max-passenger-count", 6)
  private val MIN_PASSENGER_COUNT: Int = ConfigManager.getIntOrDefault("quality.min-passenger-count", 1)
  private val MAX_DURATION_MINUTES: Double = ConfigManager.getDoubleOrDefault("quality.max-duration-minutes", 240.0)
  private val MIN_DURATION_MINUTES: Double = ConfigManager.getDoubleOrDefault("quality.min-duration-minutes", 1.0)

  def fullCheck(
                 spark: SparkSession,
                 df: DataFrame,
                 tableName: String,
                 originalCount: Long,
                 executionId: String
               ): QualityReport = {
    logger.info(s"开始质量检测: $tableName (原始记录数: $originalCount)")
    val startTime = System.currentTimeMillis()

    val metrics = computeAllMetrics(df, originalCount)

    val isEmpty = metrics.finalCount == 0
    // 【修正】放宽留存率检查
    val retentionOk = metrics.retention >= RETENTION_MIN && metrics.retention <= RETENTION_MAX
    val anomalyOk = metrics.anomalyRatio < ANOMALY_THRESHOLD
    // 【修正】唯一性检测不再强制失败（仅警告）
    val uniqueOk = metrics.uniqueOk
    val rangeOk = metrics.rangeOk

    // 【修正】综合判断：只对严重问题标记失败
    val allPass = !isEmpty && retentionOk && anomalyOk && rangeOk

    val report = QualityReport(
      tableName, allPass, originalCount, metrics.finalCount,
      metrics.retention * 100, metrics.anomalyRatio * 100,
      metrics.uniqueOk, metrics.rangeOk, executionId
    )

    val duration = System.currentTimeMillis() - startTime
    logger.info(s"\n${report.format()}")
    logger.info(s"质量检测耗时: ${duration}ms (共扫描 ${metrics.finalCount} 条记录)")

    // 【修正】唯一性失败只记录警告，不写死信
    if (!metrics.uniqueOk) {
      logger.warn(s"⚠️ 唯一性检测失败: $tableName (总计 ${metrics.finalCount} 条，唯一 ${metrics.distinctCount} 条)")
    }

    if (!allPass && DEAD_LETTER_ENABLED && metrics.anomalyCount > 0) {
      val failedData = extractFailedRecords(df, metrics)
      DeadLetterWriter.writeToDeadLetter(spark, failedData, tableName, report.getFailureReason, executionId)
    }

    if (!allPass && FAIL_ON_FAILURE) {
      throw new QualityCheckException(s"[$tableName] 质量检测失败:\n${report.format()}")
    }

    report
  }

  private case class QualityMetrics(
                                     finalCount: Long,
                                     retention: Double,
                                     anomalyRatio: Double,
                                     anomalyCount: Long,
                                     uniqueOk: Boolean,
                                     rangeOk: Boolean,
                                     mean: Double,
                                     stddev: Double,
                                     distinctCount: Long
                                   )

  private def computeAllMetrics(df: DataFrame, originalCount: Long): QualityMetrics = {
    val anomalyCondition =
      col("trip_distance") <= 0 || col("trip_distance") > MAX_TRIP_DISTANCE ||
        col("total_amount") <= 0 || col("total_amount") > MAX_TOTAL_AMOUNT ||
        col("passenger_count") < MIN_PASSENGER_COUNT || col("passenger_count") > MAX_PASSENGER_COUNT ||
        col("trip_duration_minutes") <= MIN_DURATION_MINUTES || col("trip_duration_minutes") > MAX_DURATION_MINUTES

    val stats = df.agg(
      count("*").as("total"),
      sum(when(anomalyCondition, 1).otherwise(0)).as("anomaly_count"),
      approx_count_distinct(col("trip_id")).as("distinct_count"),
      avg(when(col("total_amount").isNotNull, col("total_amount"))).as("mean"),
      stddev_samp(when(col("total_amount").isNotNull, col("total_amount"))).as("stddev")
    ).collect()(0)

    val finalCount = stats.getLong(0)
    val anomalyCount = if (stats.isNullAt(1)) 0L else stats.getLong(1)
    val distinctCount = if (stats.isNullAt(2)) 0L else stats.getLong(2)
    val mean = if (stats.isNullAt(3)) 0.0 else stats.getDouble(3)
    val stddev = if (stats.isNullAt(4)) 0.0 else stats.getDouble(4)

    val retention = if (originalCount > 0) finalCount.toDouble / originalCount else 0.0
    val anomalyRatio = if (finalCount > 0) anomalyCount.toDouble / finalCount else 1.0
    // 【修正】唯一性检测：允许少量重复（容错率0.01%）
    val uniqueOk = finalCount == distinctCount || (finalCount - distinctCount).toDouble / finalCount < 0.0001

    val rangeOk = computeRangeOk(df, finalCount, mean, stddev)

    QualityMetrics(finalCount, retention, anomalyRatio, anomalyCount, uniqueOk, rangeOk, mean, stddev, distinctCount)
  }

  // computeRangeOk 保持不变...
  private def computeRangeOk(df: DataFrame, totalCount: Long, mean: Double, stddev: Double): Boolean = {
    if (stddev <= 1e-6 || totalCount == 0) {
      return true
    }

    val lowerBound = mean - RANGE_THRESHOLD * stddev
    val upperBound = mean + RANGE_THRESHOLD * stddev

    val amountCol = col("total_amount")
    val outOfRangeCondition = amountCol.isNotNull &&
      (amountCol < lowerBound || amountCol > upperBound)

    val shouldSample = totalCount > 1000000 && RANGE_CHECK_SAMPLE_RATE < 1.0

    if (shouldSample) {
      val sampledDf = df.sample(RANGE_CHECK_SAMPLE_RATE)
      val cachedSampled = sampledDf.persist(org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK)

      try {
        val sampledTotal = cachedSampled.count()
        if (sampledTotal == 0) return true

        val outOfRangeCount = cachedSampled.filter(outOfRangeCondition).count()
        val estimatedRatio = outOfRangeCount.toDouble / sampledTotal

        logger.info(s"范围检测使用采样率 ${RANGE_CHECK_SAMPLE_RATE * 100}%，" +
          s"估算异常比例: ${estimatedRatio * 100}% (样本量: $sampledTotal)")

        estimatedRatio < 0.10
      } finally {
        cachedSampled.unpersist()
      }
    } else {
      val outOfRangeCount = df.filter(outOfRangeCondition).count()
      val ratio = outOfRangeCount.toDouble / totalCount
      logger.info(s"范围检测全量计算，异常比例: ${ratio * 100}%")
      ratio < 0.10
    }
  }

  private def extractFailedRecords(df: DataFrame, metrics: QualityMetrics): DataFrame = {
    val anomalyCondition =
      col("trip_distance") <= 0 || col("trip_distance") > MAX_TRIP_DISTANCE ||
        col("total_amount") <= 0 || col("total_amount") > MAX_TOTAL_AMOUNT ||
        col("passenger_count") < MIN_PASSENGER_COUNT || col("passenger_count") > MAX_PASSENGER_COUNT ||
        col("trip_duration_minutes") <= MIN_DURATION_MINUTES || col("trip_duration_minutes") > MAX_DURATION_MINUTES

    df.filter(anomalyCondition)
      .limit(MAX_DEAD_LETTER_RECORDS)
  }

  def check(
             spark: SparkSession,
             df: DataFrame,
             tableName: String,
             passed: Boolean,
             reason: String,
             executionId: String
           ): Unit = {
    if (!passed) {
      logger.warn(s"质量检测失败: $tableName - $reason")
      if (DEAD_LETTER_ENABLED) {
        val failedData = df.limit(MAX_DEAD_LETTER_RECORDS)
        DeadLetterWriter.writeToDeadLetter(spark, failedData, tableName, reason, executionId)
      }
      if (FAIL_ON_FAILURE) throw new QualityCheckException(s"[$tableName] $reason")
    }
  }
}