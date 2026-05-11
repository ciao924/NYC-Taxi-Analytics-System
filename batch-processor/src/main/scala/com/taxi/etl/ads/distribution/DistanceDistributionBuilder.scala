package com.taxi.etl.ads.distribution

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object DistanceDistributionBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.DISTANCE_DISTRIBUTION
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_DISTANCE_RANGE

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

      val resultDf = filteredDf.select($"stat_date", $"avg_distance")
        .withColumn("distance_range",
          when($"avg_distance" < 5, "0-5英里")
            .when($"avg_distance" < 10, "5-10英里")
            .when($"avg_distance" < 20, "10-20英里")
            .when($"avg_distance" < 50, "20-50英里")
            .otherwise("50-120英里")
        )
        .groupBy($"stat_date", $"distance_range")
        .agg(
          count("*").as("trip_count"),
          round(avg($"avg_distance"), 2).as("avg_distance")
        )
        .orderBy($"stat_date", $"distance_range")

      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = Seq("trip_count", "avg_distance"),
        stringFields = Seq.empty
      )

      val days = getDaysBetween(startDate, endDate)
      val minExpectedRows = days

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "distance_range", "trip_count"),
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