package com.taxi.etl.ads.traffic

import com.taxi.etl.ads.base.{AdsConstants, BaseAdsWriter, BuilderResult}
import com.taxi.etl.common.{ConfigManager, MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object DropoffHotspotsBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.DROPOFF_HOTSPOTS
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_ZONE

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

      // 【优化】缓存过滤后的数据
      val cachedDf = filteredDf.persist()

      try {
        val resultDf = cachedDf.groupBy($"stat_date", $"zone_name", $"borough", $"service_zone")
          .agg(sum($"dropoff_count").as("trip_count"))
          .filter($"trip_count" > 0)
          .orderBy($"stat_date", $"trip_count".desc)

        val filledDf = BaseAdsWriter.fillNullValues(
          resultDf,
          numericFields = Seq("trip_count"),
          stringFields = AdsConstants.StringFields.ZONE_FIELDS
        )

        val days = getDaysBetween(startDate, endDate)
        // 【修正】放宽预期行数
        val minExpectedRowsMultiplier = ConfigManager.getIntOrDefault("ads.hotspot-min-rows-multiplier", 200)
        val minExpectedRows = days * minExpectedRowsMultiplier

        logger.info(s"  DropoffHotspots 预期最小行数: $minExpectedRows (${days}天 × $minExpectedRowsMultiplier)")

        val qualityPass = BaseAdsWriter.quickQualityCheck(
          df = filledDf,
          tableName = TABLE_NAME,
          requiredFields = Seq("stat_date", "zone_name", "trip_count"),
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
      } finally {
        cachedDf.unpersist()
      }

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