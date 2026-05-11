package com.taxi.etl.utils

import org.apache.spark.sql.{DataFrame, SparkSession}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

object MonitorUtils {

  private val logger = LoggerFactory.getLogger(getClass)
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  def getCurrentMemoryUsage(): (Long, Long) = {
    val runtime = Runtime.getRuntime
    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    val maxMemory = runtime.maxMemory() / (1024 * 1024)
    (usedMemory, maxMemory)
  }

  def quickQualityCheck(df: DataFrame, tableName: String, executionId: String = ""): Long = {
    val totalCount = df.count()
    logger.info(s"📊 $tableName: ${formatNumber(totalCount)} 条记录")
    totalCount
  }

  def monitorTableSize(spark: SparkSession, database: String, tableName: String, executionId: String = ""): Long = {
    val fullTableName = s"$database.$tableName"
    val currentCount = spark.sql(s"SELECT COUNT(*) FROM $fullTableName").collect()(0).getLong(0)
    logger.info(s"📈 表数据量: $fullTableName = ${formatNumber(currentCount)} 条")
    currentCount
  }

  def printSystemResources(): Unit = {
    logger.info("\n💻 系统资源状态")
    logger.info("=" * 60)
    logger.info(s"CPU 核心数: ${Runtime.getRuntime.availableProcessors()}")
    val (usedMemory, maxMemory) = getCurrentMemoryUsage()
    logger.info(s"JVM 内存: ${formatMemory(usedMemory)} / ${formatMemory(maxMemory)}")
  }

  def printSparkInfo(spark: SparkSession): Unit = {
    logger.info("\n⚡ Spark 执行信息")
    logger.info("=" * 60)
    val sc = spark.sparkContext
    logger.info(s"应用名称: ${sc.appName}")
    logger.info(s"应用 ID: ${sc.applicationId}")
    logger.info(s"Spark 版本: ${sc.version}")
  }

  def generateReport(): Unit = {
    logger.info("\n" + "=" * 80)
    logger.info("📊 数仓 ETL 监控报告")
    logger.info("=" * 80)
    logger.info(s"报告时间: ${LocalDateTime.now().format(dateFormatter)}")
    val (usedMemory, maxMemory) = getCurrentMemoryUsage()
    logger.info(s"JVM 内存: ${formatMemory(usedMemory)} / ${formatMemory(maxMemory)}")
    logger.info("=" * 80)
  }

  def formatNumber(num: Long): String = f"$num%,d"
  def formatMemory(mb: Long): String = if (mb < 1024) s"${mb} MB" else s"${mb / 1024} GB"
}