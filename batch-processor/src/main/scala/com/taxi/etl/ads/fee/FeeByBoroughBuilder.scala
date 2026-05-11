package com.taxi.etl.ads.fee

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object FeeByBoroughBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.FEE_BY_BOROUGH
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_BOROUGH

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
        .agg(sum($"total_amount").as("daily_total"))

      val resultDf = filteredDf.join(dailyTotal, Seq("stat_date"))
        .groupBy($"stat_date", $"pu_borough".as("borough"))
        .agg(
          sum($"trip_count").as("trip_count"),
          sum($"total_amount").as("total_revenue"),
          round(avg($"avg_amount"), 2).as("avg_fare"),
          sum($"total_tip").as("total_tip"),
          round(avg($"avg_tip"), 2).as("avg_tip"),
          first($"daily_total").as("daily_total")
        )
        .withColumn("revenue_ratio", round($"total_revenue" / $"daily_total" * 100, 2))
        .select(
          $"stat_date",
          $"borough",
          $"trip_count",
          round($"total_revenue", 2).as("total_revenue"),
          $"avg_fare",
          round($"total_tip", 2).as("total_tip"),
          $"avg_tip",
          $"revenue_ratio"
        )
        .orderBy($"stat_date", $"total_revenue".desc)

      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = Seq("trip_count", "total_revenue", "avg_fare", "total_tip", "avg_tip", "revenue_ratio"),
        stringFields = Seq("borough")
      )

      val days = getDaysBetween(startDate, endDate)
      val minExpectedRows = days * 5

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "borough", "total_revenue"),
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