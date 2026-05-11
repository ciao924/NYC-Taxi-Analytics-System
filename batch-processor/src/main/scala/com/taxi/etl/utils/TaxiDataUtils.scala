package com.taxi.etl.utils

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.slf4j.LoggerFactory

object TaxiDataUtils {

  private val logger = LoggerFactory.getLogger(getClass)

  def readYellowData(spark: SparkSession, basePath: String): DataFrame = {
    logger.info(s"读取黄表数据: $basePath/*yellow*.parquet")
    spark.read.parquet(s"$basePath/*yellow*.parquet")
  }

  def readGreenData(spark: SparkSession, basePath: String): DataFrame = {
    logger.info(s"读取绿表数据: $basePath/*green*.parquet")
    spark.read.parquet(s"$basePath/*green*.parquet")
  }

  def writeToHiveOptimized(
                            df: DataFrame,
                            database: String,
                            tableName: String,
                            partitionColumns: Seq[String],
                            saveMode: SaveMode = SaveMode.Overwrite,
                            optimizeWrite: Boolean = true
                          ): Unit = {
    val fullTableName = s"$database.$tableName"
    logger.info(s"开始写入 Hive 表: $fullTableName")
    val startTime = System.currentTimeMillis()

    val dfToWrite = if (optimizeWrite) df.coalesce(partitionColumns.length * 10) else df

    dfToWrite.write
      .mode(saveMode)
      .format("hive")
      .partitionBy(partitionColumns: _*)
      .option("spark.sql.sources.partitionOverwriteMode", "dynamic")
      .saveAsTable(fullTableName)

    logger.info(s"✓ 写入完成，耗时: ${(System.currentTimeMillis() - startTime) / 1000} 秒")
  }

  def getPartitions(spark: SparkSession, database: String, tableName: String): Array[Map[String, String]] = {
    try {
      spark.sql(s"SHOW PARTITIONS $database.$tableName").collect().map { row =>
        row.getString(0).split("/").map { part =>
          val Array(key, value) = part.split("=")
          key -> value
        }.toMap
      }
    } catch { case _: Exception => Array.empty }
  }

  def dropPartitions(spark: SparkSession, database: String, tableName: String, partitions: Seq[Map[String, String]]): Unit = {
    val fullTableName = s"$database.$tableName"
    partitions.foreach { partition =>
      val conditions = partition.map { case (k, v) => s"$k='$v'" }.mkString(", ")
      try {
        spark.sql(s"ALTER TABLE $fullTableName DROP IF EXISTS PARTITION ($conditions)")
        logger.info(s"  删除分区: $conditions")
      } catch {
        case e: Exception => logger.warn(s"  警告: ${e.getMessage}")
      }
    }
  }
}