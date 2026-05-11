package com.taxi.etl.ads.fee

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object FeeCompositionBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.FEE_COMPOSITION
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.DATE_FEE_CODE

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

      val resultDf = filteredDf.groupBy($"stat_date")
        .agg(
          sum($"total_fare").as("fare"),
          sum($"total_extra").as("extra"),
          sum($"total_mta_tax").as("mta_tax"),
          sum($"total_tip").as("tip"),
          sum($"total_tolls").as("tolls"),
          sum($"total_improvement").as("improvement"),
          sum($"total_congestion").as("congestion"),
          sum($"total_airport_fee").as("airport"),
          sum($"total_cbd_fee").as("cbd")
        )
        .select(
          $"stat_date",
          explode(array(
            struct(lit("fare").as("fee_code"), lit("基础车费").as("fee_name"), $"fare".as("amount")),
            struct(lit("extra").as("fee_code"), lit("附加费").as("fee_name"), $"extra".as("amount")),
            struct(lit("mta_tax").as("fee_code"), lit("MTA税").as("fee_name"), $"mta_tax".as("amount")),
            struct(lit("tip").as("fee_code"), lit("小费").as("fee_name"), $"tip".as("amount")),
            struct(lit("tolls").as("fee_code"), lit("过路费").as("fee_name"), $"tolls".as("amount")),
            struct(lit("improvement").as("fee_code"), lit("改善附加费").as("fee_name"), $"improvement".as("amount")),
            struct(lit("congestion").as("fee_code"), lit("拥堵费").as("fee_name"), $"congestion".as("amount")),
            struct(lit("airport").as("fee_code"), lit("机场费").as("fee_name"), $"airport".as("amount")),
            struct(lit("cbd").as("fee_code"), lit("CBD拥堵费").as("fee_name"), $"cbd".as("amount"))
          )).as("fee")
        )
        .select(
          $"stat_date",
          $"fee.fee_code",
          $"fee.fee_name",
          round($"fee.amount", 2).as("total_amount")
        )
        .orderBy($"stat_date", $"fee_code")

      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = AdsConstants.NumericFields.FEE_FIELDS,
        stringFields = AdsConstants.StringFields.FEE_FIELDS
      )

      val days = getDaysBetween(startDate, endDate)
      val minExpectedRows = days * 9

      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq("stat_date", "fee_code", "total_amount"),
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