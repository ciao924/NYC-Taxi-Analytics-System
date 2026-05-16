package com.taxi.etl.quality

import com.taxi.etl.common.ConfigManager
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{DecimalType, DoubleType, StringType, StructField, StructType}
import org.slf4j.LoggerFactory

import java.sql.{Connection, DriverManager, PreparedStatement, Timestamp}
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.collection.mutable.ListBuffer

object QualityReporter {

  private val logger = LoggerFactory.getLogger(getClass)
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  private val MYSQL_HOST = ConfigManager.getStringOrDefault("mysql.quality.host", "192.168.127.102")
  private val MYSQL_PORT = ConfigManager.getIntOrDefault("mysql.quality.port", 3306)
  private val MYSQL_DATABASE = ConfigManager.getStringOrDefault("mysql.quality.database", "nyc_taxi_quality")
  private val MYSQL_USER = ConfigManager.getStringOrDefault("mysql.quality.user", "root")
  private val MYSQL_PASSWORD = ConfigManager.getStringOrDefault("mysql.quality.password", "BAi@123456")

  private val MYSQL_URL = s"jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/$MYSQL_DATABASE?useSSL=false&serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8"

  private val QUALITY_TABLE = "data_quality_daily"
  private val ALERT_CONFIG_TABLE = "quality_alert_config"
  private val ALERT_HISTORY_TABLE = "quality_alert_history"

  def recordQualityCheck(
    tableName: String,
    checkDate: LocalDate,
    checkType: String,
    expectedValue: Double,
    actualValue: Double,
    status: String,
    detailJson: Option[String] = None
  ): Unit = {
    logger.debug(s"Recording quality check: table=$tableName, type=$checkType, status=$status")
    var conn: Connection = null
    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)

      val deviationRate = if (expectedValue != 0) {
        (actualValue - expectedValue) / expectedValue
      } else {
        0.0
      }

      val sql = s"""
        INSERT INTO $QUALITY_TABLE (check_date, table_name, check_type, check_status, expected_value, actual_value, deviation_rate, detail_json, create_time)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      """

      val preparedStatement = conn.prepareStatement(sql)
      preparedStatement.setDate(1, java.sql.Date.valueOf(checkDate))
      preparedStatement.setString(2, tableName)
      preparedStatement.setString(3, checkType)
      preparedStatement.setString(4, status)
      preparedStatement.setBigDecimal(5, new java.math.BigDecimal(expectedValue))
      preparedStatement.setBigDecimal(6, new java.math.BigDecimal(actualValue))
      preparedStatement.setBigDecimal(7, new java.math.BigDecimal(deviationRate))
      preparedStatement.setString(8, detailJson.orNull)
      preparedStatement.setTimestamp(9, new Timestamp(System.currentTimeMillis()))

      preparedStatement.executeUpdate()
      preparedStatement.close()

      checkAndTriggerAlert(conn, tableName, checkType, checkDate, status, actualValue, expectedValue)

    } catch {
      case e: Exception =>
        logger.warn(s"Failed to record quality check to MySQL: ${e.getMessage}")
    } finally {
      if (conn != null) conn.close()
    }
  }

  private def checkAndTriggerAlert(
    conn: Connection,
    tableName: String,
    checkType: String,
    checkDate: LocalDate,
    status: String,
    actualValue: Double,
    expectedValue: Double
  ): Unit = {
    try {
      val alertSql = s"""
        SELECT id, alert_name, threshold_type, warning_threshold, critical_threshold, webhook_url, email_recipients
        FROM $ALERT_CONFIG_TABLE
        WHERE check_type = ? AND enabled = 1 AND (table_name IS NULL OR table_name = ?)
        LIMIT 1
      """

      val preparedStatement = conn.prepareStatement(alertSql)
      preparedStatement.setString(1, checkType)
      preparedStatement.setString(2, tableName)

      val resultSet = preparedStatement.executeQuery()
      if (resultSet.next()) {
        val alertConfigId = resultSet.getLong("id")
        val alertName = resultSet.getString("alert_name")
        val thresholdType = resultSet.getString("threshold_type")
        val warningThreshold = resultSet.getDouble("warning_threshold")
        val criticalThreshold = resultSet.getDouble("critical_threshold")
        val webhookUrl = resultSet.getString("webhook_url")
        val emailRecipients = resultSet.getString("email_recipients")

        val (alertLevel, shouldAlert) = if (thresholdType == "RATIO") {
          val ratio = actualValue / expectedValue
          if (ratio < criticalThreshold) ("CRITICAL", true)
          else if (ratio < warningThreshold) ("WARNING", true)
          else ("NORMAL", false)
        } else {
          val diff = Math.abs(actualValue - expectedValue)
          if (diff > criticalThreshold) ("CRITICAL", true)
          else if (diff > warningThreshold) ("WARNING", true)
          else ("NORMAL", false)
        }

        if (shouldAlert && (status == "WARN" || status == "FAIL")) {
          insertAlertHistory(conn, alertConfigId, alertLevel, alertName, checkDate, tableName, checkType, actualValue,
            if (thresholdType == "RATIO") warningThreshold else expectedValue)
          logger.warn(s"Alert triggered: $alertName, level=$alertLevel, actual=$actualValue")
        }
      }

      resultSet.close()
      preparedStatement.close()
    } catch {
      case e: Exception =>
        logger.warn(s"Failed to check and trigger alert: ${e.getMessage}")
    }
  }

  private def insertAlertHistory(
    conn: Connection,
    alertConfigId: Long,
    alertLevel: String,
    alertName: String,
    checkDate: LocalDate,
    tableName: String,
    checkType: String,
    actualValue: Double,
    thresholdValue: Double
  ): Unit = {
    try {
      val alertContent = s"[$alertLevel] $alertName: 实际值=$actualValue, 阈值=$thresholdValue"

      val sql = s"""
        INSERT INTO $ALERT_HISTORY_TABLE (alert_config_id, alert_level, alert_content, check_date, table_name, check_type, actual_value, threshold_value, is_resolved, create_time)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, ?)
      """

      val preparedStatement = conn.prepareStatement(sql)
      preparedStatement.setLong(1, alertConfigId)
      preparedStatement.setString(2, alertLevel)
      preparedStatement.setString(3, alertContent)
      preparedStatement.setDate(4, java.sql.Date.valueOf(checkDate))
      preparedStatement.setString(5, tableName)
      preparedStatement.setString(6, checkType)
      preparedStatement.setBigDecimal(7, new java.math.BigDecimal(actualValue))
      preparedStatement.setBigDecimal(8, new java.math.BigDecimal(thresholdValue))
      preparedStatement.setTimestamp(9, new Timestamp(System.currentTimeMillis()))

      preparedStatement.executeUpdate()
      preparedStatement.close()
    } catch {
      case e: Exception =>
        logger.warn(s"Failed to insert alert history: ${e.getMessage}")
    }
  }

  def reportOdsQuality(
    spark: SparkSession,
    tableName: String,
    checkDate: LocalDate,
    originalCount: Long,
    nullRates: Map[String, Double],
    encodingIssues: Long
  ): Unit = {
    logger.info(s"Reporting ODS quality for $tableName on $checkDate")

    recordQualityCheck(
      tableName = tableName,
      checkDate = checkDate,
      checkType = "row_count",
      expectedValue = 0,
      actualValue = originalCount,
      status = if (originalCount > 0) "PASS" else "FAIL",
      detailJson = Some(s"""{"encoding_issues": $encodingIssues}""")
    )

    nullRates.foreach { case (field, nullRate) =>
      recordQualityCheck(
        tableName = tableName,
        checkDate = checkDate,
        checkType = "null_rate",
        expectedValue = ConfigManager.getDoubleOrDefault("quality.null-rate-threshold", 0.05),
        actualValue = nullRate,
        status = if (nullRate < 0.05) "PASS" else "FAIL",
        detailJson = Some(s"""{"field": "$field"}""")
      )
    }
  }

  def reportDwdQuality(
    tableName: String,
    checkDate: LocalDate,
    originalCount: Long,
    finalCount: Long,
    retentionRate: Double,
    nullRates: Map[String, Double],
    uniquenessRate: Double,
    anomalyRatio: Double
  ): Unit = {
    logger.info(s"Reporting DWD quality for $tableName on $checkDate")

    recordQualityCheck(
      tableName = tableName,
      checkDate = checkDate,
      checkType = "row_count",
      expectedValue = 0,
      actualValue = finalCount,
      status = if (finalCount > 0) "PASS" else "FAIL",
      detailJson = Some(s"""{"original": $originalCount}""")
    )

    recordQualityCheck(
      tableName = tableName,
      checkDate = checkDate,
      checkType = "retention_rate",
      expectedValue = ConfigManager.getDoubleOrDefault("quality.expected-retention-rate-min", 0.70),
      actualValue = retentionRate,
      status = if (retentionRate >= 0.70) "PASS" else "FAIL",
      detailJson = Some(s"""{"original":$originalCount,"final":$finalCount}""")
    )

    recordQualityCheck(
      tableName = tableName,
      checkDate = checkDate,
      checkType = "uniqueness",
      expectedValue = ConfigManager.getDoubleOrDefault("quality.uniqueness-threshold", 0.999),
      actualValue = uniquenessRate,
      status = if (uniquenessRate >= 0.999) "PASS" else "FAIL",
      detailJson = None
    )

    recordQualityCheck(
      tableName = tableName,
      checkDate = checkDate,
      checkType = "range_anomaly",
      expectedValue = ConfigManager.getDoubleOrDefault("quality.anomaly-threshold", 0.05),
      actualValue = anomalyRatio,
      status = if (anomalyRatio < 0.05) "PASS" else "FAIL",
      detailJson = Some(s"""{"null_rates": ${nullRates.mkString(",")}}}""")
    )
  }

  def reportDwsQuality(
    tableName: String,
    checkDate: LocalDate,
    rowCount: Long,
    expectedRowCount: Long,
    consistencyError: Double,
    nullRates: Map[String, Double]
  ): Unit = {
    logger.info(s"Reporting DWS quality for $tableName on $checkDate")

    recordQualityCheck(
      tableName = tableName,
      checkDate = checkDate,
      checkType = "row_count",
      expectedValue = expectedRowCount,
      actualValue = rowCount,
      status = if (Math.abs(rowCount - expectedRowCount) <= expectedRowCount * 0.1) "PASS" else "WARN",
      detailJson = None
    )

    recordQualityCheck(
      tableName = tableName,
      checkDate = checkDate,
      checkType = "consistency",
      expectedValue = ConfigManager.getDoubleOrDefault("quality.consistency-threshold", 0.0001),
      actualValue = consistencyError,
      status = if (consistencyError < 0.0001) "PASS" else "FAIL",
      detailJson = None
    )
  }

  def reportAdsQuality(
    tableName: String,
    checkDate: LocalDate,
    rowCount: Long,
    expectedMinRows: Long,
    uniquenessRate: Double,
    nullRates: Map[String, Double]
  ): Unit = {
    logger.info(s"Reporting ADS quality for $tableName on $checkDate")

    recordQualityCheck(
      tableName = tableName,
      checkDate = checkDate,
      checkType = "row_count",
      expectedValue = expectedMinRows,
      actualValue = rowCount,
      status = if (rowCount >= expectedMinRows) "PASS" else "WARN",
      detailJson = None
    )

    recordQualityCheck(
      tableName = tableName,
      checkDate = checkDate,
      checkType = "uniqueness",
      expectedValue = ConfigManager.getDoubleOrDefault("quality.uniqueness-threshold", 0.999),
      actualValue = uniquenessRate,
      status = if (uniquenessRate >= 0.999) "PASS" else "WARN",
      detailJson = None
    )
  }

  def calculateOdsQualityMetrics(spark: SparkSession, tableName: String, checkDate: LocalDate, targetMonths: Seq[Int]): Map[String, Any] = {
    val result = scala.collection.mutable.Map[String, Any]()

    try {
      val year = checkDate.getYear
      val monthList = targetMonths.mkString(",")
      val greenMonthList = targetMonths.map(m => f"$m%02d").mkString(",")

      val (count, nullRates, encodingIssues) = if (tableName.contains("yellow")) {
        val total = spark.sql(s"SELECT COUNT(*) FROM $tableName WHERE year = $year AND month IN ($monthList)").collect()(0).getLong(0)
        val nullCheck = spark.sql(s"""
          SELECT
            SUM(CASE WHEN VendorID IS NULL THEN 1 ELSE 0 END) +
            SUM(CASE WHEN tpep_pickup_datetime IS NULL THEN 1 ELSE 0 END) +
            SUM(CASE WHEN tpep_dropoff_datetime IS NULL THEN 1 ELSE 0 END) +
            SUM(CASE WHEN PULocationID IS NULL THEN 1 ELSE 0 END) +
            SUM(CASE WHEN DOLocationID IS NULL THEN 1 ELSE 0 END) AS total_nulls
          FROM $tableName WHERE year = $year AND month IN ($monthList)
        """).collect()(0).getLong(0)
        val encoding = spark.sql(s"""
          SELECT COUNT(*) FROM $tableName
          WHERE year = $year AND month IN ($monthList) AND store_and_fwd_flag NOT IN ('Y', 'N', '')
        """).collect()(0).getLong(0)
        (total, Map("vendor_id" -> (nullCheck * 100.0 / total), "pickup_datetime" -> 0.0, "dropoff_datetime" -> 0.0), encoding)
      } else {
        val total = spark.sql(s"SELECT COUNT(*) FROM $tableName WHERE year = $year AND month IN ($greenMonthList)").collect()(0).getLong(0)
        val nullCheck = spark.sql(s"""
          SELECT
            SUM(CASE WHEN VendorID IS NULL THEN 1 ELSE 0 END) +
            SUM(CASE WHEN lpep_pickup_datetime IS NULL THEN 1 ELSE 0 END) +
            SUM(CASE WHEN lpep_dropoff_datetime IS NULL THEN 1 ELSE 0 END) +
            SUM(CASE WHEN PULocationID IS NULL THEN 1 ELSE 0 END) +
            SUM(CASE WHEN DOLocationID IS NULL THEN 1 ELSE 0 END) AS total_nulls
          FROM $tableName WHERE year = $year AND month IN ($greenMonthList)
        """).collect()(0).getLong(0)
        val encoding = spark.sql(s"""
          SELECT COUNT(*) FROM $tableName
          WHERE year = $year AND month IN ($greenMonthList) AND store_and_fwd_flag NOT IN ('Y', 'N', '')
        """).collect()(0).getLong(0)
        (total, Map("vendor_id" -> (nullCheck * 100.0 / total), "pickup_datetime" -> 0.0, "dropoff_datetime" -> 0.0), encoding)
      }

      result("row_count") = count
      result("null_rates") = nullRates
      result("encoding_issues") = encodingIssues

    } catch {
      case e: Exception =>
        logger.warn(s"Failed to calculate ODS quality metrics: ${e.getMessage}")
    }

    result.toMap
  }
}