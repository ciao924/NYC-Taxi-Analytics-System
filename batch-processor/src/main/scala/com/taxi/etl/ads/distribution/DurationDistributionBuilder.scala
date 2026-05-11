package com.taxi.etl.ads.distribution

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object DurationDistributionBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.DURATION_DISTRIBUTION
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_DURATION_RANGE

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

      val resultDf = filteredDf.select($"stat_date", $"avg_trip_duration")
        .withColumn("duration_range",
          when($"avg_trip_duration" < 10, "0-10分钟")
            .when($"avg_trip_duration" < 20, "10-20分钟")
            .when($"avg_trip_duration" < 30, "20-30分钟")
            .when($"avg_trip_duration" < 60, "30-60分钟")
            .otherwise("60分钟以上")
        )
        .groupBy($"stat_date", $"duration_range")
        .agg(
          count("*").as("trip_count"),
          round(avg($"avg_trip_duration"), 2).as("avg_duration")
        )
        .orderBy($"stat_date", $"duration_range")

      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = Seq("trip_count", "avg_duration"),
        stringFields = Seq.empty
      )

      val days = getDaysBetween(startDate, endDate)
      val minExpectedRows = days

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "duration_range", "trip_count"),
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