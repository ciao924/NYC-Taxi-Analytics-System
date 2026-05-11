package com.taxi.etl.ads.distribution

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory


object PassengerDistributionBuilder {


  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.PASSENGER_DISTRIBUTION
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_PASSENGER

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
        .groupBy("stat_date", "passenger_count")
        .agg(
          count("*").as("trip_count"),
          round(avg("passenger_count"), 2).as("avg_passenger_count")
        )
        .withColumn("passenger_range", 
          when(col("passenger_count") === 1, "1人")
          .when(col("passenger_count") === 2, "2人")
          .when(col("passenger_count") === 3, "3人")
          .when(col("passenger_count") === 4, "4人")
          .otherwise("5人及以上")
        )
        .select(
          $"stat_date",
          $"passenger_count",
          $"passenger_range",
          $"trip_count"
        )
        .orderBy($"stat_date", $"passenger_count")

      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = Seq("trip_count", "passenger_count"),
        stringFields = Seq("passenger_range")
      )

      val days = getDaysBetween(startDate, endDate)
      val minExpectedRows = days * 5

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "passenger_count", "trip_count"),
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