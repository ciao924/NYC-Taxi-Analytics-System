package com.taxi.etl.ads.base

import com.taxi.etl.common.ConfigManager
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.functions._
import java.util.Properties
import scala.collection.mutable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

object BaseAdsWriter {

  private val logger = LoggerFactory.getLogger(getClass)

  private val MYSQL_HOST = ConfigManager.getStringOrDefault("mysql.host", "192.168.127.102")
  private val MYSQL_PORT = ConfigManager.getIntOrDefault("mysql.port", 3306)
  private val MYSQL_DATABASE = ConfigManager.getStringOrDefault("mysql.database", "nyc_taxi_ads")
  private val MYSQL_USER = ConfigManager.getStringOrDefault("mysql.user", "root")
  private val MYSQL_PASSWORD = ConfigManager.getStringOrDefault("mysql.password", "BAi@123456")

  val JDBC_BATCH_SIZE: Int = ConfigManager.getIntOrDefault("mysql.batch-size", 10000)
  val JDBC_FETCH_SIZE: Int = ConfigManager.getIntOrDefault("mysql.fetch-size", 5000)

  // 控制写入并发
  private val WRITE_PARTITIONS: Int = Math.min(ConfigManager.getIntOrDefault("mysql.write-partitions", 5), 4)

  private val MYSQL_URL = s"jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/$MYSQL_DATABASE?useSSL=false&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true&useUnicode=true&characterEncoding=utf-8"

  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  private val qualityResults = mutable.Map[String, mutable.Map[String, Any]]()

  private val NUMERIC_NULL = 0.0
  private val STRING_NULL = "未知"
  private val DECIMAL_SCALE = 2

  def getJdbcProperties(): Properties = {
    val props = new Properties()
    props.setProperty("user", MYSQL_USER)
    props.setProperty("password", MYSQL_PASSWORD)
    props.setProperty("driver", "com.mysql.cj.jdbc.Driver")
    props.setProperty("batchsize", JDBC_BATCH_SIZE.toString)
    props.setProperty("fetchsize", JDBC_FETCH_SIZE.toString)
    props.setProperty("rewriteBatchedStatements", "true")
    props.setProperty("useServerPrepStmts", "false")
    props
  }

  def getMysqlUrl: String = MYSQL_URL
  def getMysqlDatabase: String = MYSQL_DATABASE

  def createDatabaseIfNotExists(spark: SparkSession): Unit = {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      val adminUrl = s"jdbc:mysql://$MYSQL_HOST:$MYSQL_PORT/?useSSL=false&serverTimezone=Asia/Shanghai"
      val conn = java.sql.DriverManager.getConnection(adminUrl, MYSQL_USER, MYSQL_PASSWORD)
      try {
        val stmt = conn.createStatement()
        stmt.executeUpdate(s"CREATE DATABASE IF NOT EXISTS $MYSQL_DATABASE")
        logger.info(s"✅ 数据库 $MYSQL_DATABASE 已就绪")
        stmt.close()
      } finally {
        conn.close()
      }
    } catch {
      case e: Exception =>
        logger.warn(s"创建数据库失败: ${e.getMessage}")
    }
  }

  def addQualityResult(tableName: String, checkName: String, result: Any): Unit = {
    val tableMetrics = qualityResults.getOrElseUpdate(tableName, mutable.Map())
    tableMetrics(checkName) = result
  }

  def printQualityReport(): Unit = {
    logger.info("=" * 80)
    logger.info("📊 ADS 层数据质量检测报告")
    logger.info("=" * 80)

    if (qualityResults.isEmpty) {
      logger.info("  无质量检测数据")
    } else {
      var totalPassed = 0
      var totalFailed = 0
      qualityResults.foreach { case (tableName, metrics) =>
        logger.info(s"\n  表名: $tableName")
        logger.info("  " + "-" * 50)
        metrics.foreach { case (checkName, result) =>
          val resultStr = result.toString
          val isPass = resultStr.contains("通过") || resultStr.contains("✅")
          if (isPass) totalPassed += 1 else totalFailed += 1
          logger.info(s"    ${if (isPass) "✅" else "❌"} $checkName: $resultStr")
        }
      }
      logger.info("\n" + "-" * 60)
      logger.info(s"  检测项统计: 通过 $totalPassed 项, 失败 $totalFailed 项")
    }
    logger.info("=" * 80)
  }

  def resetQualityResults(): Unit = {
    qualityResults.clear()
  }

  def withUpdateTime(df: DataFrame): DataFrame = {
    val currentTime = LocalDateTime.now().format(dateTimeFormatter)
    df.withColumn("update_time", lit(currentTime))
  }

  def fillNullValues(
                      df: DataFrame,
                      numericFields: Seq[String] = Seq.empty,
                      stringFields: Seq[String] = Seq.empty
                    ): DataFrame = {
    var result = df
    numericFields.foreach { field =>
      result = result.withColumn(field, when(col(field).isNull, lit(NUMERIC_NULL)).otherwise(col(field)))
    }
    stringFields.foreach { field =>
      result = result.withColumn(field, when(col(field).isNull, lit(STRING_NULL)).otherwise(col(field)))
    }
    result
  }

  def roundDecimal(df: DataFrame, fields: Seq[String]): DataFrame = {
    fields.foldLeft(df) { (currentDf, field) =>
      currentDf.withColumn(field, round(col(field), DECIMAL_SCALE))
    }
  }

  def writeToMysqlIdempotent(
                              df: DataFrame,
                              tableName: String,
                              uniqueKeys: Seq[String],
                              dateField: String = "stat_date",
                              startDate: String,
                              endDate: String
                            ): Long = {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver")
      val conn = java.sql.DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)
      try {
        val deleteSql = s"DELETE FROM $tableName WHERE $dateField >= ? AND $dateField <= ?"
        val stmt = conn.prepareStatement(deleteSql)
        stmt.setString(1, startDate)
        stmt.setString(2, endDate)
        val deletedCount = stmt.executeUpdate()
        if (deletedCount > 0) {
          logger.debug(s"  已删除 $tableName 中 $startDate 至 $endDate 的数据，共 $deletedCount 条")
        }
        stmt.close()
      } finally {
        conn.close()
      }
    } catch {
      case e: Exception =>
        logger.warn(s"  删除数据失败: ${e.getMessage}，将继续写入")
    }

    writeToMysql(df, tableName, uniqueKeys)
  }

  def writeToMysql(
                    df: DataFrame,
                    tableName: String,
                    uniqueKeys: Seq[String]
                  ): Long = {
    val dfWithTime = withUpdateTime(df)
    val rowCount = dfWithTime.count()

    logger.info(s"  写入记录数: ${formatNumber(rowCount)} 条")

    val startTime = System.currentTimeMillis()
    val optimalPartitions = Math.min(WRITE_PARTITIONS, Math.max(1, (rowCount / 10000).toInt))

    val columns = dfWithTime.columns
    val placeholders = columns.map(_ => "?").mkString(",")
    val insertSql = s"INSERT INTO $tableName (${columns.mkString(",")}) VALUES ($placeholders)"

    dfWithTime.coalesce(optimalPartitions).foreachPartition { partition: Iterator[Row] =>
      var conn: java.sql.Connection = null
      var stmt: java.sql.PreparedStatement = null
      try {
        conn = java.sql.DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)
        conn.setAutoCommit(false)
        stmt = conn.prepareStatement(insertSql)
        var batchSize = 0

        partition.foreach { row =>
          columns.zipWithIndex.foreach { case (col, idx) =>
            val value = row.getAs[Any](col)
            stmt.setObject(idx + 1, value)
          }
          stmt.addBatch()
          batchSize += 1

          if (batchSize >= JDBC_BATCH_SIZE) {
            stmt.executeBatch()
            batchSize = 0
          }
        }

        if (batchSize > 0) {
          stmt.executeBatch()
        }

        conn.commit()
      } finally {
        if (stmt != null) stmt.close()
        if (conn != null) conn.close()
      }
    }

    val durationSec = (System.currentTimeMillis() - startTime) / 1000
    logger.info(s"  ✓ 写入完成，耗时: ${durationSec} 秒")

    rowCount
  }

  // 【修正】quickQualityCheck 增加配置化阈值
  def quickQualityCheck(
                         df: DataFrame,
                         tableName: String,
                         requiredFields: Seq[String],
                         keyFields: Seq[String],
                         minExpectedRows: Long
                       ): Boolean = {
    val cachedDf = df.persist()

    try {
      val actualRows = cachedDf.count()
      val isComplete = actualRows >= minExpectedRows
      logger.info(s"  完整性检查: 预期≥$minExpectedRows 条, 实际 $actualRows 条 → ${if (isComplete) "✅" else "❌"}")

      val nullCheckExprs = requiredFields.map { field =>
        sum(when(col(field).isNull, 1).otherwise(0)).as(s"${field}_null")
      }

      val nullCounts = cachedDf.agg(nullCheckExprs.head, nullCheckExprs.tail: _*).first()

      var allNullPass = true
      requiredFields.zipWithIndex.foreach { case (field, idx) =>
        val nullCount = if (nullCounts.isNullAt(idx)) 0L else nullCounts.getLong(idx)
        val nullRate = if (actualRows > 0) nullCount.toDouble / actualRows * 100 else 0.0
        logger.info(s"  空值检查 - $field: ${"%.2f".format(nullRate)}% ${if (nullRate == 0) "✅" else "❌"}")
        if (nullRate != 0) allNullPass = false
      }

      // 【修正】唯一性检查使用更宽松的判断
      val isUnique = if (keyFields.nonEmpty) {
        val distinctCount = cachedDf.select(keyFields.map(col): _*).distinct().count()
        val unique = actualRows == distinctCount
        if (!unique) {
          logger.warn(s"  唯一性检查: ${keyFields.mkString(", ")} → ❌ (总计 $actualRows 条，唯一 $distinctCount 条)")
        } else {
          logger.info(s"  唯一性检查: ${keyFields.mkString(", ")} → ✅")
        }
        unique
      } else true

      val allPass = isComplete && allNullPass && isUnique

      addQualityResult(tableName, "完整性", if (isComplete) "通过" else s"失败 (仅${actualRows}条)")
      addQualityResult(tableName, "空值", if (allNullPass) "通过" else "失败")
      addQualityResult(tableName, "唯一性", if (isUnique) "通过" else "失败")
      addQualityResult(tableName, "总体", if (allPass) "通过" else "失败")

      allPass
    } finally {
      cachedDf.unpersist()
    }
  }

  private def formatNumber(num: Long): String = {
    if (num >= 1000000) f"${num / 1000000.0}%.1fM"
    else if (num >= 1000) f"${num / 1000.0}%.1fK"
    else num.toString
  }
}