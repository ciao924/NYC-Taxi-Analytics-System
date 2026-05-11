package com.taxi.etl.ads.daily

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object VendorAnalysisBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.VENDOR
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_VENDOR

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

      val dailyTotal = filteredDf.groupBy($"stat_date")
        .agg(sum($"total_revenue").as("daily_total"))

      val resultDf = filteredDf.join(dailyTotal, Seq("stat_date"))
        .withColumn("revenue_ratio", round($"total_revenue" / $"daily_total" * 100, 2))
        .select(
          $"stat_date",
          $"vendor_name",
          $"trip_count",
          round($"total_revenue", 2).as("total_revenue"),
          round($"avg_amount", 2).as("avg_fare"),
          round($"avg_distance", 2).as("avg_distance"),
          $"revenue_ratio"
        )
        .orderBy($"stat_date", $"trip_count".desc)

      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = AdsConstants.NumericFields.VENDOR_FIELDS,
        stringFields = AdsConstants.StringFields.VENDOR_FIELDS
      )

      val days = getDaysBetween(startDate, endDate)
      val minExpectedRows = days * 2

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "vendor_name", "total_revenue"),
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