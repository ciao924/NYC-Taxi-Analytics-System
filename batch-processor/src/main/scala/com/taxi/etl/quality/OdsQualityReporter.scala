package com.taxi.etl.quality

import com.taxi.etl.common.ConfigManager
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

import java.time.LocalDate
import scala.collection.mutable

object OdsQualityReporter {

  private val logger = LoggerFactory.getLogger(getClass)

  def validateAndReport(spark: SparkSession, checkDate: LocalDate, targetMonths: Seq[Int]): Boolean = {
    logger.info("=" * 80)
    logger.info(s"📊 ODS 层数据质量检测与报告 - ${checkDate}")
    logger.info("=" * 80)

    var allPassed = true

    val yellowMetrics = validateYellowTable(spark, checkDate, targetMonths)
    val greenMetrics = validateGreenTable(spark, checkDate, targetMonths)

    if (!yellowMetrics("passed").asInstanceOf[Boolean]) allPassed = false
    if (!greenMetrics("passed").asInstanceOf[Boolean]) allPassed = false

    val yellowNullRates = yellowMetrics("null_rates").asInstanceOf[Map[String, Double]]
    val greenNullRates = greenMetrics("null_rates").asInstanceOf[Map[String, Double]]
    val yellowEncodingIssues = yellowMetrics("encoding_issues").asInstanceOf[Long]
    val greenEncodingIssues = greenMetrics("encoding_issues").asInstanceOf[Long]

    QualityReporter.reportOdsQuality(
      spark = spark,
      tableName = "nyc_taxi_ods.taxi_trip_yellow_ods",
      checkDate = checkDate,
      originalCount = yellowMetrics("row_count").asInstanceOf[Long],
      nullRates = yellowNullRates.toMap,
      encodingIssues = yellowEncodingIssues
    )

    QualityReporter.reportOdsQuality(
      spark = spark,
      tableName = "nyc_taxi_ods.taxi_trip_green_ods",
      checkDate = checkDate,
      originalCount = greenMetrics("row_count").asInstanceOf[Long],
      nullRates = greenNullRates.toMap,
      encodingIssues = greenEncodingIssues
    )

    logger.info("=" * 80)
    logger.info(s"✅ ODS 层数据质量检测完成 - ${if (allPassed) "全部通过" else "存在异常"}")
    logger.info("=" * 80)

    allPassed
  }

  private def validateYellowTable(spark: SparkSession, checkDate: LocalDate, targetMonths: Seq[Int]): Map[String, Any] = {
    val result = mutable.Map[String, Any]()
    val year = checkDate.getYear
    val tableName = "nyc_taxi_ods.taxi_trip_yellow_ods"
    val monthList = targetMonths.mkString(",")

    try {
      logger.info(s"\n🟡 黄表 ($tableName) 质量检测")
      logger.info("-" * 60)

      val totalCount = spark.sql(
        s"SELECT COUNT(*) FROM $tableName WHERE year = $year AND month IN ($monthList)"
      ).collect()(0).getLong(0)

      logger.info(s"  行数检测: ${formatNumber(totalCount)} 条")
      result("row_count") = totalCount

      val nullCheck = spark.sql(s"""
        SELECT
          SUM(CASE WHEN VendorID IS NULL THEN 1 ELSE 0 END) AS vendor_nulls,
          SUM(CASE WHEN tpep_pickup_datetime IS NULL THEN 1 ELSE 0 END) AS pickup_nulls,
          SUM(CASE WHEN tpep_dropoff_datetime IS NULL THEN 1 ELSE 0 END) AS dropoff_nulls,
          SUM(CASE WHEN PULocationID IS NULL THEN 1 ELSE 0 END) AS pu_nulls,
          SUM(CASE WHEN DOLocationID IS NULL THEN 1 ELSE 0 END) AS do_nulls
        FROM $tableName WHERE year = $year AND month IN ($monthList)
      """).collect()(0)

      val totalNulls = nullCheck.getLong(0) + nullCheck.getLong(1) + nullCheck.getLong(2) +
        nullCheck.getLong(3) + nullCheck.getLong(4)
      val vendorNullRate = if (totalCount > 0) nullCheck.getLong(0).toDouble / totalCount else 0.0
      val pickupNullRate = if (totalCount > 0) nullCheck.getLong(1).toDouble / totalCount else 0.0
      val dropoffNullRate = if (totalCount > 0) nullCheck.getLong(2).toDouble / totalCount else 0.0

      logger.info(s"  空值率检测:")
      logger.info(s"    VendorID: ${vendorNullRate * 100}%.2f%% ${if (vendorNullRate < 0.05) "✅" else "❌"}")
      logger.info(s"    pickup_datetime: ${pickupNullRate * 100}%.2f%% ${if (pickupNullRate < 0.05) "✅" else "❌"}")
      logger.info(s"    dropoff_datetime: ${dropoffNullRate * 100}%.2f%% ${if (dropoffNullRate < 0.05) "✅" else "❌"}")

      val nullRates = Map(
        "VendorID" -> vendorNullRate,
        "pickup_datetime" -> pickupNullRate,
        "dropoff_datetime" -> dropoffNullRate
      )
      result("null_rates") = nullRates

      val encodingIssues = spark.sql(s"""
        SELECT COUNT(*) FROM $tableName
        WHERE year = $year AND month IN ($monthList)
        AND store_and_fwd_flag NOT IN ('Y', 'N', '')
      """).collect()(0).getLong(0)

      logger.info(s"  编码检测: $encodingIssues 异常 ${if (encodingIssues == 0) "✅" else "❌"}")
      result("encoding_issues") = encodingIssues

      val passed = totalCount > 0 && vendorNullRate < 0.05 && pickupNullRate < 0.05 &&
        dropoffNullRate < 0.05 && encodingIssues == 0
      result("passed") = passed

      if (passed) {
        logger.info(s"  总体结果: ✅ 通过")
      } else {
        logger.warn(s"  总体结果: ❌ 未通过")
      }

    } catch {
      case e: Exception =>
        logger.error(s"  黄表质量检测失败: ${e.getMessage}")
        result("passed") = false
        result("error") = e.getMessage
    }

    result.toMap
  }

  private def validateGreenTable(spark: SparkSession, checkDate: LocalDate, targetMonths: Seq[Int]): Map[String, Any] = {
    val result = mutable.Map[String, Any]()
    val year = checkDate.getYear
    val tableName = "nyc_taxi_ods.taxi_trip_green_ods"
    val monthList = targetMonths.map(m => f"$m%02d").mkString(",")

    try {
      logger.info(s"\n🟢 绿表 ($tableName) 质量检测")
      logger.info("-" * 60)

      val totalCount = spark.sql(
        s"SELECT COUNT(*) FROM $tableName WHERE year = $year AND month IN ($monthList)"
      ).collect()(0).getLong(0)

      logger.info(s"  行数检测: ${formatNumber(totalCount)} 条")
      result("row_count") = totalCount

      val nullCheck = spark.sql(s"""
        SELECT
          SUM(CASE WHEN VendorID IS NULL THEN 1 ELSE 0 END) AS vendor_nulls,
          SUM(CASE WHEN lpep_pickup_datetime IS NULL THEN 1 ELSE 0 END) AS pickup_nulls,
          SUM(CASE WHEN lpep_dropoff_datetime IS NULL THEN 1 ELSE 0 END) AS dropoff_nulls,
          SUM(CASE WHEN PULocationID IS NULL THEN 1 ELSE 0 END) AS pu_nulls,
          SUM(CASE WHEN DOLocationID IS NULL THEN 1 ELSE 0 END) AS do_nulls
        FROM $tableName WHERE year = $year AND month IN ($monthList)
      """).collect()(0)

      val vendorNullRate = if (totalCount > 0) nullCheck.getLong(0).toDouble / totalCount else 0.0
      val pickupNullRate = if (totalCount > 0) nullCheck.getLong(1).toDouble / totalCount else 0.0
      val dropoffNullRate = if (totalCount > 0) nullCheck.getLong(2).toDouble / totalCount else 0.0

      logger.info(s"  空值率检测:")
      logger.info(s"    VendorID: ${vendorNullRate * 100}%.2f%% ${if (vendorNullRate < 0.05) "✅" else "❌"}")
      logger.info(s"    pickup_datetime: ${pickupNullRate * 100}%.2f%% ${if (pickupNullRate < 0.05) "✅" else "❌"}")
      logger.info(s"    dropoff_datetime: ${dropoffNullRate * 100}%.2f%% ${if (dropoffNullRate < 0.05) "✅" else "❌"}")

      val nullRates = Map(
        "VendorID" -> vendorNullRate,
        "pickup_datetime" -> pickupNullRate,
        "dropoff_datetime" -> dropoffNullRate
      )
      result("null_rates") = nullRates

      val encodingIssues = spark.sql(s"""
        SELECT COUNT(*) FROM $tableName
        WHERE year = $year AND month IN ($monthList)
        AND store_and_fwd_flag NOT IN ('Y', 'N', '')
      """).collect()(0).getLong(0)

      logger.info(s"  编码检测: $encodingIssues 异常 ${if (encodingIssues == 0) "✅" else "❌"}")
      result("encoding_issues") = encodingIssues

      val passed = totalCount > 0 && vendorNullRate < 0.05 && pickupNullRate < 0.05 &&
        dropoffNullRate < 0.05 && encodingIssues == 0
      result("passed") = passed

      if (passed) {
        logger.info(s"  总体结果: ✅ 通过")
      } else {
        logger.warn(s"  总体结果: ❌ 未通过")
      }

    } catch {
      case e: Exception =>
        logger.error(s"  绿表质量检测失败: ${e.getMessage}")
        result("passed") = false
        result("error") = e.getMessage
    }

    result.toMap
  }

  private def formatNumber(num: Long): String = {
    if (num >= 1000000) f"${num / 1000000.0}%.1fM"
    else if (num >= 1000) f"${num / 1000.0}%.1fK"
    else num.toString
  }
}