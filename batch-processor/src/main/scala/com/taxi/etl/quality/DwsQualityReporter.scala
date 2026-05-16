package com.taxi.etl.quality

import com.taxi.etl.common.ConfigManager
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

import java.time.LocalDate
import scala.collection.mutable.Map

object DwsQualityReporter {

  private val logger = LoggerFactory.getLogger(getClass)

  def validateAndReport(
    spark: SparkSession,
    checkDate: LocalDate,
    dwdRowCount: Long,
    dwdRevenue: Double
  ): Boolean = {
    logger.info("=" * 80)
    logger.info(s"📊 DWS 层数据质量检测与报告 - ${checkDate}")
    logger.info("=" * 80)

    var allPassed = true

    val tableNames = List(
      "nyc_taxi_dws.dws_trip_daily",
      "nyc_taxi_dws.dws_trip_hourly",
      "nyc_taxi_dws.dws_trip_zone_pickup_daily",
      "nyc_taxi_dws.dws_trip_zone_dropoff_daily"
    )

    tableNames.foreach { tableName =>
      val passed = validateSingleTable(spark, tableName, checkDate, dwdRowCount, dwdRevenue)
      if (!passed) allPassed = false
    }

    logger.info("=" * 80)
    logger.info(s"✅ DWS 层数据质量检测完成 - ${if (allPassed) "全部通过" else "存在异常"}")
    logger.info("=" * 80)

    allPassed
  }

  private def validateSingleTable(
    spark: SparkSession,
    tableName: String,
    checkDate: LocalDate,
    dwdRowCount: Long,
    dwdRevenue: Double
  ): Boolean = {
    val result = Map[String, Any]()
    var passed = true

    try {
      logger.info(s"\n📋 表: $tableName")
      logger.info("-" * 60)

      val statDate = checkDate.toString
      val tableRowCount = spark.sql(s"SELECT COUNT(*) FROM $tableName WHERE stat_date = '$statDate'")
        .collect()(0).getLong(0)
      logger.info(s"  行数: ${formatNumber(tableRowCount)}")

      val expectedMinRows = getExpectedMinRows(tableName, checkDate)
      if (tableRowCount < expectedMinRows) {
        logger.warn(s"  行数低于预期: $tableRowCount < $expectedMinRows")
        passed = false
      }

      val (totalTrips, totalRevenue, consistencyError) = if (tableName.contains("daily")) {
        val stats = spark.sql(s"""
          SELECT
            SUM(total_trips) AS total_trips,
            SUM(total_revenue) AS total_revenue
          FROM $tableName WHERE stat_date = '$statDate'
        """).collect()(0)

        val sumTrips = if (stats.isNullAt(0)) 0L else stats.getLong(0)
        val sumRevenue = if (stats.isNullAt(1)) 0.0 else stats.getDouble(1)

        val expectedDays = 1
        val estimatedDwdTrips = dwdRowCount * expectedDays
        val tripsError = if (estimatedDwdTrips > 0) {
          Math.abs(sumTrips - estimatedDwdTrips).toDouble / estimatedDwdTrips
        } else 0.0

        (sumTrips, sumRevenue, tripsError)
      } else {
        (tableRowCount, 0.0, 0.0)
      }

      logger.info(s"  总行程数: ${formatNumber(totalTrips)}")
      logger.info(s"  总收入: ${"$%.2f".format(totalRevenue)}")
      logger.info(s"  与DWD一致性误差: ${consistencyError * 100}%.4f%% ${if (consistencyError < 0.01) "✅" else "❌"}")

      if (consistencyError >= 0.01) {
        passed = false
      }

      val nullCheck = spark.sql(s"""
        SELECT
          SUM(CASE WHEN pu_borough IS NULL OR pu_borough = '未知' THEN 1 ELSE 0 END) AS pu_borough_nulls,
          SUM(CASE WHEN do_borough IS NULL OR do_borough = '未知' THEN 1 ELSE 0 END) AS do_borough_nulls
        FROM $tableName WHERE stat_date = '$statDate'
      """).collect()(0)

      val puNullRate = if (tableRowCount > 0) nullCheck.getLong(0).toDouble / tableRowCount else 0.0
      val doNullRate = if (tableRowCount > 0) nullCheck.getLong(1).toDouble / tableRowCount else 0.0

      logger.info(s"  维度字段空值率:")
      logger.info(s"    pu_borough: ${puNullRate * 100}%.2f%% ${if (puNullRate < 0.01) "✅" else "❌"}")
      logger.info(s"    do_borough: ${doNullRate * 100}%.2f%% ${if (doNullRate < 0.01) "✅" else "❌"}")

      val nullRates = Map(
        "pu_borough" -> puNullRate,
        "do_borough" -> doNullRate
      )

      QualityReporter.reportDwsQuality(
        tableName = tableName,
        checkDate = checkDate,
        rowCount = tableRowCount,
        expectedRowCount = expectedMinRows,
        consistencyError = consistencyError,
        nullRates = nullRates.toMap
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

  private def getExpectedMinRows(tableName: String, checkDate: LocalDate): Long = {
    tableName match {
      case name if name.contains("daily") => 1L
      case name if name.contains("hourly") => 24L
      case name if name.contains("zone") => 10L
      case _ => 1L
    }
  }

  private def formatNumber(num: Long): String = {
    if (num >= 1000000) f"${num / 1000000.0}%.1fM"
    else if (num >= 1000) f"${num / 1000.0}%.1fK"
    else num.toString
  }
}