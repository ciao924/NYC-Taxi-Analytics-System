package com.taxi.etl.ads.daily

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object WeekdayAnalysisBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.WEEKDAY_ANALYSIS
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_DAY_OF_WEEK

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

      val resultDf = filteredDf
        .withColumn("day_of_week", dayofweek($"stat_date"))
        .withColumn("day_of_week_name", date_format($"stat_date", "EEEE"))
        .select(
          $"stat_date",
          $"day_of_week",
          $"day_of_week_name",
          $"total_trips",
          round($"total_revenue", 2).as("total_revenue"),
          round($"avg_amount", 2).as("avg_fare"),
          round($"avg_distance", 2).as("avg_distance")
        )
        .orderBy($"stat_date", $"day_of_week")

      val weekdayNumericFields = Seq("total_trips", "total_revenue", "avg_fare", "avg_distance")
      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = weekdayNumericFields,
        stringFields = Seq.empty
      )

      val days = getDaysBetween(startDate, endDate)
      val minExpectedRows = days

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "day_of_week", "total_trips"),
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