package com.taxi.etl.ads.daily

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object PaymentAnalysisBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.PAYMENT_ANALYSIS
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_PAYMENT

  private val STANDARD_PAYMENTS = Seq(
    ("Credit Card", true),
    ("Cash", false),
    ("Dispute", false),
    ("No Charge", false),
    ("Unknown", false)
  )

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

      val actualDf = filteredDf.groupBy($"stat_date", $"payment_name", $"is_cashless")
        .agg(
          sum($"trip_count").as("trip_count"),
          sum($"total_amount").as("total_amount"),
          round(avg($"avg_amount"), 2).as("avg_amount"),
          sum($"total_tip").as("total_tip"),
          round(avg($"avg_tip"), 2).as("avg_tip")
        )

      val allDatesDf = filteredDf.select($"stat_date").distinct()
      val standardPaymentsDf = STANDARD_PAYMENTS.toDF("payment_name", "is_cashless")
      val allDatePaymentsDf = allDatesDf.crossJoin(standardPaymentsDf)

      val resultDf = allDatePaymentsDf
        .join(actualDf, Seq("stat_date", "payment_name", "is_cashless"), "left_outer")
        .na.fill(0, Seq("trip_count", "total_amount", "avg_amount", "total_tip", "avg_tip"))

      val dailyTotal = filteredDf.groupBy($"stat_date")
        .agg(sum($"trip_count").as("daily_total"))

      val finalDf = resultDf
        .join(dailyTotal, Seq("stat_date"))
        .withColumn("trip_ratio", round(when($"daily_total" > 0, $"trip_count" / $"daily_total" * 100).otherwise(0), 2))
        .select(
          $"stat_date",
          $"payment_name",
          $"is_cashless",
          $"trip_count",
          round($"total_amount", 2).as("total_amount"),
          $"avg_amount",
          round($"total_tip", 2).as("total_tip"),
          $"avg_tip",
          $"trip_ratio"
        )
        .orderBy($"stat_date", $"trip_count".desc)

      val filledDf = BaseAdsWriter.fillNullValues(
        finalDf,
        numericFields = AdsConstants.NumericFields.PAYMENT_FIELDS,
        stringFields = AdsConstants.StringFields.PAYMENT_FIELDS
      )

      val days = getDaysBetween(startDate, endDate)
      val minExpectedRows = days * STANDARD_PAYMENTS.size

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "payment_name", "trip_count"),
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