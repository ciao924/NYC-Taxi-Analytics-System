package com.taxi.etl.utils

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.apache.spark.sql.functions.{col, current_timestamp, lit, struct, to_json}
import org.slf4j.LoggerFactory

object DeadLetterWriter {

  private val logger = LoggerFactory.getLogger(getClass)
  private val DEAD_LETTER_DATABASE = "nyc_taxi_dead_letter"

  def writeToDeadLetter(
                         spark: SparkSession,
                         failedDf: DataFrame,
                         sourceTable: String,
                         failReason: String,
                         executionId: String = ""
                       ): Unit = {

    val deadTableName = s"$DEAD_LETTER_DATABASE.dead_letter_${sourceTable.replace(".", "_")}"

    try {
      // 确保数据库存在
      spark.sql(s"CREATE DATABASE IF NOT EXISTS $DEAD_LETTER_DATABASE")

      // 检查表是否存在，不存在则创建
      if (!spark.catalog.tableExists(deadTableName)) {
        spark.sql(
          s"""
             |CREATE TABLE IF NOT EXISTS $deadTableName (
             |  fail_reason  STRING,
             |  fail_time    TIMESTAMP,
             |  execution_id STRING,
             |  raw_data     STRING
             |) USING ORC
           """.stripMargin)
        logger.info(s"死信表创建成功: $deadTableName")
      }

      // 准备死信数据
      val deadLetterDf = failedDf
        .select(to_json(struct(failedDf.columns.map(col): _*)).as("raw_data"))
        .withColumn("fail_reason", lit(failReason))
        .withColumn("fail_time", current_timestamp())
        .withColumn("execution_id", lit(executionId))

      // 使用 insertInto 替代 saveAsTable，避免格式冲突
      deadLetterDf.write
        .mode(SaveMode.Append)
        .insertInto(deadTableName)

      val failedCount = failedDf.count()
      logger.warn(s"已写入死信表: $deadTableName | 失败记录数: $failedCount | 失败原因: $failReason | executionId: $executionId")

    } catch {
      case e: Exception =>
        // 死信写入失败不应中断主流程，只记录错误
        logger.error(s"写入死信表失败 ($deadTableName): ${e.getMessage}", e)
    }
  }
}