package com.taxi.etl.ads.daily

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object KpiDailyBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.KPI_DAILY
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.SINGLE_DATE

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

      // 生产环境：保持 peak_hours 数组类型，保留完整的高峰小时信息
      // 注意：MySQL 表中 peak_hours 字段应为 JSON 或 TEXT 类型
      val resultDf = filteredDf.select(
        $"stat_date",
        $"total_trips",
        round($"total_revenue", 2).as("total_revenue"),
        round($"avg_amount", 2).as("avg_fare"),
        round($"avg_distance", 2).as("avg_distance"),
        round($"avg_trip_duration", 2).as("avg_duration"),
        round($"total_tip", 2).as("total_tip"),
        round($"avg_tip", 2).as("avg_tip"),
        $"total_airport_trips".as("airport_trips"),
        // 保持数组类型，存储完整的 peak_hours 信息
        // 将数组转换为 JSON 字符串以便存入 MySQL
        to_json($"peak_hours").as("peak_hours")
      ).orderBy($"stat_date")

      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = AdsConstants.NumericFields.KPI_FIELDS,
        stringFields = Seq.empty
      )

      val days = getDaysBetween(startDate, endDate)
      val minExpectedRows = days

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "total_trips", "total_revenue"),
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