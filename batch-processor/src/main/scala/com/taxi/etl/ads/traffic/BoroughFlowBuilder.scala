package com.taxi.etl.ads.traffic

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object BoroughFlowBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.BOROUGH_FLOW
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_BOROUGH_FLOW

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

      val pickupByBorough = filteredDf.groupBy($"stat_date", $"borough".as("pu_borough"))
        .agg(sum($"pickup_count").as("pickup_count"))

      val dropoffByBorough = filteredDf.groupBy($"stat_date", $"borough".as("do_borough"))
        .agg(sum($"dropoff_count").as("dropoff_count"))

      val resultDf = pickupByBorough.join(dropoffByBorough, Seq("stat_date"), "full_outer")
        .na.fill(0, Seq("pickup_count", "dropoff_count"))
        .na.fill("未知", Seq("pu_borough", "do_borough"))
        .select(
          $"stat_date",
          $"pu_borough",
          $"do_borough",
          $"pickup_count",
          $"dropoff_count"
        )
        .orderBy($"stat_date", $"pickup_count".desc)

      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = Seq("pickup_count", "dropoff_count"),
        stringFields = AdsConstants.StringFields.BOROUGH_FIELDS
      )

      val days = getDaysBetween(startDate, endDate)
      val minExpectedRows = days * 25

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "pu_borough", "do_borough"),
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