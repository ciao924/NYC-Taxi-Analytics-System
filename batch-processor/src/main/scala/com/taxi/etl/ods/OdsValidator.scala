package com.taxi.etl.ods

import com.taxi.etl.common.SparkSessionFactory
import com.taxi.etl.utils.MonitorUtils
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object OdsValidator {

  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    val spark = SparkSessionFactory.create("ODS_Validator", enableHive = true)

    try {
      val targetYear = if (args.length > 0) args(0).toInt else 2025
      val targetMonths = if (args.length > 1) args(1).split(",").map(_.toInt).toSeq else Seq(1, 2, 3)

      logger.info("=" * 80)
      logger.info("📊 ODS 层数据验证工具（v4-lite）")
      logger.info("=" * 80)
      logger.info(s"目标年份: $targetYear, 目标月份: ${targetMonths.mkString(", ")}")
      logger.info("=" * 80)

      validateYellowTable(spark, targetYear, targetMonths)
      validateGreenTable(spark, targetYear, targetMonths)
      crossValidate(spark, targetYear, targetMonths)
      validateDataQuality(spark, targetYear, targetMonths)

      logger.info("\n" + "=" * 80 + "\n✅ ODS 层数据验证完成\n" + "=" * 80)

    } catch {
      case e: Exception =>
        logger.error(s"验证失败: ${e.getMessage}", e)
        System.exit(1)
    } finally {
      spark.stop()
    }
  }

  private def validateYellowTable(spark: org.apache.spark.sql.SparkSession, year: Int, months: Seq[Int]): Unit = {
    logger.info("\n🟡 黄表验证\n" + "-" * 60)
    val tableName = "nyc_taxi_ods.taxi_trip_yellow_ods"
    months.foreach { month =>
      val count = spark.sql(s"SELECT COUNT(*) FROM $tableName WHERE year = $year AND month = $month").collect()(0).getLong(0)
      logger.info(s"  ${if (count > 0) "✅" else "⚠️"} year=$year, month=$month: ${MonitorUtils.formatNumber(count)} 条")
    }
  }

  private def validateGreenTable(spark: org.apache.spark.sql.SparkSession, year: Int, months: Seq[Int]): Unit = {
    logger.info("\n🟢 绿表验证\n" + "-" * 60)
    val tableName = "nyc_taxi_ods.taxi_trip_green_ods"
    months.foreach { month =>
      val count = spark.sql(s"SELECT COUNT(*) FROM $tableName WHERE year = $year AND month = $month").collect()(0).getLong(0)
      logger.info(s"  ${if (count > 0) "✅" else "⚠️"} year=$year, month=$month: ${MonitorUtils.formatNumber(count)} 条")
    }
  }

  private def crossValidate(spark: org.apache.spark.sql.SparkSession, year: Int, months: Seq[Int]): Unit = {
    logger.info("\n📊 交叉验证（黄表 vs 绿表）\n" + "-" * 60)
    val yellowTable = "nyc_taxi_ods.taxi_trip_yellow_ods"
    val greenTable = "nyc_taxi_ods.taxi_trip_green_ods"
    months.foreach { month =>
      val yellowCount = spark.sql(s"SELECT COUNT(*) FROM $yellowTable WHERE year = $year AND month = $month").collect()(0).getLong(0)
      val greenCount = spark.sql(s"SELECT COUNT(*) FROM $greenTable WHERE year = $year AND month = $month").collect()(0).getLong(0)
      logger.info(s"  year=$year, month=$month: 黄表=${MonitorUtils.formatNumber(yellowCount)}, 绿表=${MonitorUtils.formatNumber(greenCount)}, 合计=${MonitorUtils.formatNumber(yellowCount + greenCount)}")
    }
  }

  /**
   * 数据质量验证：字段完整性、无乱码
   */
  private def validateDataQuality(spark: org.apache.spark.sql.SparkSession, year: Int, months: Seq[Int]): Unit = {
    logger.info("\n📋 数据质量验证（字段完整性、编码）\n" + "-" * 60)

    val yellowTable = "nyc_taxi_ods.taxi_trip_yellow_ods"
    val greenTable = "nyc_taxi_ods.taxi_trip_green_ods"

    // 验证黄表关键字段空值率
    val yellowNullCheck = spark.sql(
      s"""
         |SELECT
         |  SUM(CASE WHEN VendorID IS NULL THEN 1 ELSE 0 END) AS vendor_null,
         |  SUM(CASE WHEN tpep_pickup_datetime IS NULL THEN 1 ELSE 0 END) AS pickup_null,
         |  SUM(CASE WHEN tpep_dropoff_datetime IS NULL THEN 1 ELSE 0 END) AS dropoff_null,
         |  SUM(CASE WHEN PULocationID IS NULL THEN 1 ELSE 0 END) AS pu_null,
         |  SUM(CASE WHEN DOLocationID IS NULL THEN 1 ELSE 0 END) AS do_null,
         |  COUNT(*) AS total
         |FROM $yellowTable
         |WHERE year = $year AND month IN (${months.mkString(",")})
       """.stripMargin).collect()(0)

    val yellowTotal = yellowNullCheck.getLong(5)
    logger.info(s"黄表字段完整性:")
    logger.info(s"  VendorID 空值率: ${yellowNullCheck.getLong(0) * 100.0 / yellowTotal}%.2f%%")
    logger.info(s"  pickup_datetime 空值率: ${yellowNullCheck.getLong(1) * 100.0 / yellowTotal}%.2f%%")
    logger.info(s"  dropoff_datetime 空值率: ${yellowNullCheck.getLong(2) * 100.0 / yellowTotal}%.2f%%")
    logger.info(s"  PULocationID 空值率: ${yellowNullCheck.getLong(3) * 100.0 / yellowTotal}%.2f%%")
    logger.info(s"  DOLocationID 空值率: ${yellowNullCheck.getLong(4) * 100.0 / yellowTotal}%.2f%%")

    // 验证绿表关键字段空值率
    val greenNullCheck = spark.sql(
      s"""
         |SELECT
         |  SUM(CASE WHEN VendorID IS NULL THEN 1 ELSE 0 END) AS vendor_null,
         |  SUM(CASE WHEN lpep_pickup_datetime IS NULL THEN 1 ELSE 0 END) AS pickup_null,
         |  SUM(CASE WHEN lpep_dropoff_datetime IS NULL THEN 1 ELSE 0 END) AS dropoff_null,
         |  SUM(CASE WHEN PULocationID IS NULL THEN 1 ELSE 0 END) AS pu_null,
         |  SUM(CASE WHEN DOLocationID IS NULL THEN 1 ELSE 0 END) AS do_null,
         |  COUNT(*) AS total
         |FROM $greenTable
         |WHERE year = $year AND month IN (${months.mkString(",")})
       """.stripMargin).collect()(0)

    val greenTotal = greenNullCheck.getLong(5)
    logger.info(s"绿表字段完整性:")
    logger.info(s"  VendorID 空值率: ${greenNullCheck.getLong(0) * 100.0 / greenTotal}%.2f%%")
    logger.info(s"  pickup_datetime 空值率: ${greenNullCheck.getLong(1) * 100.0 / greenTotal}%.2f%%")
    logger.info(s"  dropoff_datetime 空值率: ${greenNullCheck.getLong(2) * 100.0 / greenTotal}%.2f%%")
    logger.info(s"  PULocationID 空值率: ${greenNullCheck.getLong(3) * 100.0 / greenTotal}%.2f%%")
    logger.info(s"  DOLocationID 空值率: ${greenNullCheck.getLong(4) * 100.0 / greenTotal}%.2f%%")

    // 验证UTF-8编码（检查是否有非ASCII字符导致的乱码）
    // 通过检查 store_and_fwd_flag 字段是否有异常值
    val yellowEncoding = spark.sql(
      s"""
         |SELECT COUNT(*) AS abnormal
         |FROM $yellowTable
         |WHERE year = $year AND month IN (${months.mkString(",")})
         |  AND store_and_fwd_flag NOT IN ('Y', 'N', '')
       """.stripMargin).collect()(0).getLong(0)

    val greenEncoding = spark.sql(
      s"""
         |SELECT COUNT(*) AS abnormal
         |FROM $greenTable
         |WHERE year = $year AND month IN (${months.mkString(",")})
         |  AND store_and_fwd_flag NOT IN ('Y', 'N', '')
       """.stripMargin).collect()(0).getLong(0)

    logger.info(s"编码检查（store_and_fwd_flag异常值）:")
    logger.info(s"  黄表异常记录数: ${yellowEncoding}")
    logger.info(s"  绿表异常记录数: ${greenEncoding}")

    if (yellowEncoding == 0 && greenEncoding == 0) {
      logger.info("  ✅ 编码正常，无乱码")
    } else {
      logger.warn("  ⚠️ 存在编码异常，请检查源数据")
    }
  }
}