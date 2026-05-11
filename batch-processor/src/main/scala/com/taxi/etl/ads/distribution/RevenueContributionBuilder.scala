package com.taxi.etl.ads.distribution

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object RevenueContributionBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.REVENUE_CONTRIBUTION
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_PU_ZONE

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
        .agg(sum($"total_revenue_pickup").as("daily_total"))

      val resultDf = filteredDf.join(dailyTotal, Seq("stat_date"))
        .groupBy($"stat_date", $"zone_name")
        .agg(
          sum($"pickup_count").as("trip_count"),
          sum($"total_revenue_pickup").as("total_revenue"),
          first($"daily_total").as("daily_total")
        )
        .withColumn("revenue_ratio", round($"total_revenue" / $"daily_total" * 100, 2))
        .select(
          $"stat_date",
          $"zone_name".as("pu_zone"),
          $"trip_count",
          round($"total_revenue", 2).as("total_revenue"),
          $"revenue_ratio"
        )
        .filter($"trip_count" > 0)

      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = Seq("trip_count", "total_revenue", "revenue_ratio"),
        stringFields = Seq("pu_zone")
      )

      val days = getDaysBetween(startDate, endDate)
      val minExpectedRows = days * 50

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "pu_zone", "total_revenue"),
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