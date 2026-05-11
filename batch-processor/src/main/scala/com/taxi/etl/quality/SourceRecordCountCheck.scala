package com.taxi.etl.quality

import org.slf4j.LoggerFactory

import java.io.File
import java.sql.{Connection, DriverManager, PreparedStatement}

object SourceRecordCountCheck {

  private val logger = LoggerFactory.getLogger(getClass)

  def checkSourceVsOdsRecordCount(
    mysqlUrl: String,
    mysqlUser: String,
    mysqlPassword: String,
    date: String,
    sourceFilePath: String,
    odsTable: String
  ): CheckResult = {
    logger.info(s"Starting source file vs ODS record count check for date: $date")

    try {
      val sourceCount = countSourceRecords(sourceFilePath, date)
      val odsCount = countOdsRecords(mysqlUrl, mysqlUser, mysqlPassword, odsTable, date)
      val variance = Math.abs(sourceCount - odsCount)
      val varianceRate = if (sourceCount > 0) variance.toDouble / sourceCount else 0.0

      val result = CheckResult(
        checkDate = date,
        sourceFilePath = sourceFilePath,
        odsTable = odsTable,
        sourceRecordCount = sourceCount,
        odsRecordCount = odsCount,
        variance = variance,
        varianceRate = varianceRate,
        passed = varianceRate < 0.01,
        checkTimestamp = new java.sql.Timestamp(System.currentTimeMillis()),
        message = if (varianceRate < 0.01) "对账通过" else s"对账失败，差异率: ${(varianceRate * 100).formatted("%.2f")}%"
      )

      saveCheckResult(mysqlUrl, mysqlUser, mysqlPassword, result)
      logger.info(s"Check completed: source=$sourceCount, ods=$odsCount, passed=${result.passed}")
      result

    } catch {
      case e: Exception =>
        logger.error(s"Failed to execute source vs ODS check for date: $date", e)
        CheckResult(
          checkDate = date,
          sourceFilePath = sourceFilePath,
          odsTable = odsTable,
          sourceRecordCount = 0,
          odsRecordCount = 0,
          variance = 0,
          varianceRate = 0.0,
          passed = false,
          checkTimestamp = new java.sql.Timestamp(System.currentTimeMillis()),
          message = s"对账执行失败: ${e.getMessage}"
        )
    }
  }

  private def countSourceRecords(filePath: String, date: String): Long = {
    val datePattern = date.replace("-", "")
    val targetFile = findTargetFile(filePath, datePattern)

    if (targetFile.exists()) {
      if (targetFile.getName.endsWith(".parquet")) {
        countParquetRecords(targetFile)
      } else if (targetFile.getName.endsWith(".csv") || targetFile.getName.endsWith(".txt")) {
        countCsvRecords(targetFile)
      } else {
        logger.warn(s"Unsupported file format: ${targetFile.getName}")
        0L
      }
    } else {
      logger.warn(s"Source file not found: $targetFile")
      0L
    }
  }

  private def findTargetFile(basePath: String, datePattern: String): File = {
    val baseDir = new File(basePath)
    if (baseDir.isFile) {
      baseDir
    } else {
      val files = baseDir.listFiles()
        .filter(f => f.isFile && f.getName.contains(datePattern))
        .toList
        .sortBy(_.lastModified)
        .reverse

      if (files.isEmpty) {
        logger.warn(s"No file found for pattern: $datePattern in $basePath")
        new File(basePath)
      } else {
        files.head
      }
    }
  }

  private def countParquetRecords(file: File): Long = {
    try {
      val tempSpark = org.apache.spark.sql.SparkSession.builder().appName("temp").master("local[*]").getOrCreate()
      try {
        val df = tempSpark.read.parquet(file.getAbsolutePath)
        df.count()
      } finally {
        tempSpark.stop()
      }
    } catch {
      case e: Exception =>
        logger.error(s"Failed to count parquet records: ${e.getMessage}")
        0L
    }
  }

  private def countCsvRecords(file: File): Long = {
    try {
      val source = scala.io.Source.fromFile(file)
      try {
        source.getLines().size - 1
      } finally {
        source.close()
      }
    } catch {
      case e: Exception =>
        logger.warn(s"Failed to count CSV records: ${e.getMessage}")
        0L
    }
  }

  private def countOdsRecords(mysqlUrl: String, mysqlUser: String, mysqlPassword: String, table: String, date: String): Long = {
    var connection: Connection = null
    var statement: PreparedStatement = null
    var resultSet: java.sql.ResultSet = null

    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      connection = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword)

      val sql = s"SELECT COUNT(*) FROM $table WHERE dt = ?"
      statement = connection.prepareStatement(sql)
      statement.setString(1, date)

      resultSet = statement.executeQuery()
      if (resultSet.next()) {
        resultSet.getLong(1)
      } else {
        0L
      }
    } catch {
      case e: Exception =>
        logger.error(s"Failed to count ODS records: ${e.getMessage}", e)
        0L
    } finally {
      if (resultSet != null) resultSet.close()
      if (statement != null) statement.close()
      if (connection != null) connection.close()
    }
  }

  private def saveCheckResult(mysqlUrl: String, mysqlUser: String, mysqlPassword: String, result: CheckResult): Unit = {
    var connection: Connection = null
    var statement: PreparedStatement = null

    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      connection = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword)

      val sql =
        """
          INSERT INTO quality_record_check (check_date, source_file_path, ods_table,
            source_record_count, ods_record_count, variance, variance_rate, passed, message, check_timestamp)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """
      statement = connection.prepareStatement(sql)
      statement.setString(1, result.checkDate)
      statement.setString(2, result.sourceFilePath)
      statement.setString(3, result.odsTable)
      statement.setLong(4, result.sourceRecordCount)
      statement.setLong(5, result.odsRecordCount)
      statement.setLong(6, result.variance)
      statement.setDouble(7, result.varianceRate)
      statement.setBoolean(8, result.passed)
      statement.setString(9, result.message)
      statement.setTimestamp(10, result.checkTimestamp)

      statement.executeUpdate()

    } catch {
      case e: Exception =>
        logger.error(s"Failed to save check result: ${e.getMessage}", e)
    } finally {
      if (statement != null) statement.close()
      if (connection != null) connection.close()
    }
  }
}

case class CheckResult(
  checkDate: String,
  sourceFilePath: String,
  odsTable: String,
  sourceRecordCount: Long,
  odsRecordCount: Long,
  variance: Long,
  varianceRate: Double,
  passed: Boolean,
  checkTimestamp: java.sql.Timestamp,
  message: String
)