package com.taxi.etl.ads.daily

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{ConfigManager, MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object HourlyDistributionBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.HOURLY_DISTRIBUTION
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_HOUR

  // 【修正】允许缺失的小时数
  private val MISSING_HOURS_TOLERANCE = ConfigManager.getIntOrDefault("ads.hourly-missing-tolerance", 10)

  def build(
             spark: SparkSession,
             df: DataFrame,
             startDate: String,
             endDate: String,
             ctx: JobContext,
             metrics: MetricsCollector
           ): BuilderResult = {
    import spark.implicits._
    try {
      val filteredDf = df.filter(col("stat_date") >= startDate && col("stat_date") <= endDate)

      // 获取实际数据
      val actualData = filteredDf.select(
        $"stat_date",
        $"pickup_hour".as("hour_of_day"),
        $"trip_count",
        round($"avg_amount", 2).as("avg_fare"),
        round($"avg_tip", 2).as("avg_tip"),
        round($"total_revenue", 2).as("total_revenue")
      )

      // 【修正】补全缺失的小时
      val allHours = spark.range(0, 24).toDF("hour_of_day")
      val allDates = filteredDf.select("stat_date").distinct()
      val fullGrid = allDates.crossJoin(allHours)

      val resultDf = fullGrid
        .join(actualData, Seq("stat_date", "hour_of_day"), "left")
        .na.fill(0, Seq("trip_count", "avg_fare", "avg_tip", "total_revenue"))
        .orderBy($"stat_date", $"hour_of_day")

      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = AdsConstants.NumericFields.HOURLY_FIELDS,
        stringFields = Seq.empty
      )

      val days = getDaysBetween(startDate, endDate)
      val expectedRows = days * 24
      val minExpectedRows = expectedRows - MISSING_HOURS_TOLERANCE

      logger.info(s"  HourlyDistribution 预期: $expectedRows 条, 最小接受: $minExpectedRows 条")

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "hour_of_day", "trip_count"),
        keyFields = UNIQUE_KEYS,
        minExpectedRows = minExpectedRows
      )

      val rowCount = BaseAdsWriter.writeToMysqlIdempotent(
        df = filledDf,
        tableName = TABLE_NAME,
        uniqueKeys = UNIQUE_KEYS,
        dateField = "stat_date",
        startDate = startDate,
        endDate = endDate
      )

      metrics.recordJobMetric("ADS", ctx.executionId, s"${TABLE_NAME}_rows", rowCount.toDouble)
      metrics.recordJobMetric("ADS", ctx.executionId, s"${TABLE_NAME}_quality", if (qualityPass) 1.0 else 0.0)

      BuilderResult(success = true, rowCount = rowCount)

    } catch {
      case e: Exception =>
        logger.error(s"构建 $TABLE_NAME 失败: ${e.getMessage}", e)
        BuilderResult(success = false, errorMessage = Some(e.getMessage))
    }
  }

  private def getDaysBetween(startDate: String, endDate: String): Int = {
    try {
      val start = java.time.LocalDate.parse(startDate)
      val end = java.time.LocalDate.parse(endDate)
      (end.toEpochDay - start.toEpochDay + 1).toInt
    } catch {
      case _: Exception => 1
    }
  }
}