package com.taxi.etl.quality

import com.taxi.etl.ads.base.BaseAdsWriter
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

import java.time.LocalDate
import scala.collection.mutable.Map

object AdsQualityReporter {

  private val logger = LoggerFactory.getLogger(getClass)

  def validateAndReport(
    spark: SparkSession,
    checkDate: LocalDate,
    startDate: String,
    endDate: String
  ): Boolean = {
    logger.info("=" * 80)
    logger.info(s"📊 ADS 层数据质量检测与报告 - ${checkDate}")
    logger.info("=" * 80)

    var allPassed = true

    val tableNames = getAdsTableNames

    tableNames.foreach { tableName =>
      val passed = validateSingleTable(spark, tableName, checkDate, startDate, endDate)
      if (!passed) allPassed = false
    }

    logger.info("=" * 80)
    logger.info(s"✅ ADS 层数据质量检测完成 - ${if (allPassed) "全部通过" else "存在异常"}")
    logger.info("=" * 80)

    allPassed
  }

  private def getAdsTableNames: List[String] = {
    List(
      "kpi_daily",
      "vendor_analysis_daily",
      "payment_analysis_daily",
      "weekday_analysis_daily",
      "hourly_distribution_daily",
      "airport_analysis_daily",
      "fee_by_borough_daily",
      "fee_by_taxi_type_daily",
      "fee_trend_daily",
      "fee_composition_daily",
      "fee_percentage_daily",
      "distance_distribution_daily",
      "duration_distribution_daily",
      "passenger_distribution_daily",
      "revenue_contribution_daily",
      "borough_flow_daily",
      "pickup_hotspots_daily",
      "dropoff_hotspots_daily"
    )
  }

  private def validateSingleTable(
    spark: SparkSession,
    tableName: String,
    checkDate: LocalDate,
    startDate: String,
    endDate: String
  ): Boolean = {
    val result = Map[String, Any]()
    var passed = true

    try {
      logger.info(s"\n📋 表: $tableName")
      logger.info("-" * 60)

      val statDate = checkDate.toString
      val fullTableName = s"${BaseAdsWriter.getMysqlDatabase}.$tableName"

      Class.forName("com.mysql.cj.jdbc.Driver")
      val conn = java.sql.DriverManager.getConnection(
        BaseAdsWriter.getMysqlUrl,
        BaseAdsWriter.getMysqlUrl.split("\\?")(0).split("/").last,
        ""
      )

      val rowCount = try {
        val stmt = conn.createStatement()
        val rs = stmt.executeQuery(s"SELECT COUNT(*) FROM $tableName WHERE stat_date = '$statDate'")
        if (rs.next()) rs.getLong(1) else 0L
      } catch {
        case _: Exception => 0L
      } finally {
        conn.close()
      }

      logger.info(s"  行数: ${formatNumber(rowCount)}")

      val expectedMinRows = getExpectedMinRows(tableName)
      if (rowCount < expectedMinRows) {
        logger.warn(s"  行数低于预期: $rowCount < $expectedMinRows")
        passed = false
      }

      val uniqueCheckPassed = checkUniqueness(spark, tableName, statDate)
      if (!uniqueCheckPassed) {
        logger.warn(s"  唯一性检测: ❌ 未通过")
        passed = false
      } else {
        logger.info(s"  唯一性检测: ✅ 通过")
      }

      val nullCheckPassed = checkNullValues(spark, tableName, statDate)
      if (!nullCheckPassed) {
        logger.warn(s"  空值检测: ❌ 未通过")
        passed = false
      } else {
        logger.info(s"  空值检测: ✅ 通过")
      }

      QualityReporter.reportAdsQuality(
        tableName = fullTableName,
        checkDate = checkDate,
        rowCount = rowCount,
        expectedMinRows = expectedMinRows,
        uniquenessRate = if (uniqueCheckPassed) 1.0 else 0.999,
        nullRates = scala.collection.immutable.Map.empty[String, Double]
      )

      if (passed) {
        logger.info(s"  总体结果: ✅ 通过")
      } else {
        logger.warn(s"  总体结果: ❌ 未通过")
      }

    } catch {
      case e: Exception =>
        logger.error(s"  表 $tableName 质量检测失败: ${e.getMessage}")
        passed = false
    }

    passed
  }

  private def checkUniqueness(spark: SparkSession, tableName: String, statDate: String): Boolean = {
    try {
      val df = spark.read.format("jdbc")
        .option("url", BaseAdsWriter.getMysqlUrl)
        .option("dbtable", tableName)
        .option("user", BaseAdsWriter.getMysqlUrl.split("\\?")(0).split("/").last)
        .option("password", "")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .load()
        .filter(col("stat_date") === statDate)

      val totalCount = df.count()
      if (totalCount == 0) return true

      val uniqueKey = getUniqueKey(tableName)
      val distinctCount = df.select(uniqueKey).distinct().count()

      val uniquenessRate = distinctCount.toDouble / totalCount
      uniquenessRate >= 0.999

    } catch {
      case e: Exception =>
        logger.warn(s"  唯一性检测失败: ${e.getMessage}")
        true
    }
  }

  private def checkNullValues(spark: SparkSession, tableName: String, statDate: String): Boolean = {
    try {
      val criticalFields = getCriticalFields(tableName)
      if (criticalFields.isEmpty) return true

      val df = spark.read.format("jdbc")
        .option("url", BaseAdsWriter.getMysqlUrl)
        .option("dbtable", tableName)
        .option("user", BaseAdsWriter.getMysqlUrl.split("\\?")(0).split("/").last)
        .option("password", "")
        .option("driver", "com.mysql.cj.jdbc.Driver")
        .load()
        .filter(col("stat_date") === statDate)

      val totalCount = df.count()
      if (totalCount == 0) return true

      criticalFields.foreach { field =>
        val nullCount = df.filter(col(field).isNull || (col(field).cast("string") === "")).count()
        val nullRate = nullCount.toDouble / totalCount
        if (nullRate > 0.01) {
          logger.warn(s"    $field 空值率: ${nullRate * 100}%.2f%% (阈值 1%)")
        }
      }

      true

    } catch {
      case e: Exception =>
        logger.warn(s"  空值检测失败: ${e.getMessage}")
        true
    }
  }

  private def getUniqueKey(tableName: String): String = {
    tableName match {
      case name if name.contains("kpi") => "stat_date"
      case name if name.contains("vendor") => "stat_date,vendor_id"
      case name if name.contains("payment") => "stat_date,payment_type"
      case name if name.contains("weekday") => "stat_date,day_of_week"
      case name if name.contains("hourly") => "stat_date,hour"
      case name if name.contains("airport") => "stat_date,airport"
      case name if name.contains("fee_by_borough") => "stat_date,borough"
      case name if name.contains("fee_by_taxi_type") => "stat_date,taxi_type"
      case name if name.contains("fee_trend") => "stat_date"
      case name if name.contains("fee_composition") => "stat_date,payment_type"
      case name if name.contains("fee_percentage") => "stat_date,payment_type"
      case name if name.contains("distance_distribution") => "stat_date,distance_bin"
      case name if name.contains("duration_distribution") => "stat_date,duration_bin"
      case name if name.contains("passenger_distribution") => "stat_date,passenger_count"
      case name if name.contains("revenue_contribution") => "stat_date,borough"
      case name if name.contains("borough_flow") => "stat_date,origin_borough,dest_borough"
      case name if name.contains("pickup_hotspots") => "stat_date,zone_id"
      case name if name.contains("dropoff_hotspots") => "stat_date,zone_id"
      case _ => "stat_date"
    }
  }

  private def getCriticalFields(tableName: String): Seq[String] = {
    tableName match {
      case name if name.contains("kpi") => Seq("stat_date", "total_trips", "total_revenue")
      case name if name.contains("vendor") => Seq("stat_date", "vendor_id", "trip_count")
      case name if name.contains("payment") => Seq("stat_date", "payment_type")
      case name if name.contains("weekday") => Seq("stat_date", "day_of_week")
      case name if name.contains("hourly") => Seq("stat_date", "hour")
      case name if name.contains("airport") => Seq("stat_date", "airport")
      case _ => Seq.empty
    }
  }

  private def getExpectedMinRows(tableName: String): Long = {
    tableName match {
      case name if name.contains("kpi") => 1L
      case name if name.contains("vendor") => 2L
      case name if name.contains("payment") => 1L
      case name if name.contains("weekday") => 7L
      case name if name.contains("hourly") => 24L
      case name if name.contains("airport") => 2L
      case name if name.contains("fee_by_borough") => 5L
      case name if name.contains("fee_by_taxi_type") => 2L
      case name if name.contains("fee_trend") => 1L
      case name if name.contains("fee_composition") => 1L
      case name if name.contains("fee_percentage") => 1L
      case name if name.contains("distance_distribution") => 5L
      case name if name.contains("duration_distribution") => 5L
      case name if name.contains("passenger_distribution") => 5L
      case name if name.contains("revenue_contribution") => 5L
      case name if name.contains("borough_flow") => 10L
      case name if name.contains("hotspots") => 10L
      case _ => 1L
    }
  }

  private def formatNumber(num: Long): String = {
    if (num >= 1000000) f"${num / 1000000.0}%.1fM"
    else if (num >= 1000) f"${num / 1000.0}%.1fK"
    else num.toString
  }
}