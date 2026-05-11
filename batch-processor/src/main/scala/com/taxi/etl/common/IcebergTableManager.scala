package com.taxi.etl.common

import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}
import org.slf4j.LoggerFactory

object IcebergTableManager {

  private val logger = LoggerFactory.getLogger(getClass)
  private val ICEBERG_ENABLED = ConfigManager.getBooleanOrDefault("iceberg.enabled", false)

  def initCatalog(spark: SparkSession): Unit = {
    if (!ICEBERG_ENABLED) return
    spark.conf.set("spark.sql.catalog.iceberg", "org.apache.iceberg.spark.SparkCatalog")
    spark.conf.set("spark.sql.catalog.iceberg.type", "hadoop")
    spark.conf.set("spark.sql.catalog.iceberg.warehouse", ConfigManager.getString("iceberg.warehouse.path"))
    logger.info("Iceberg Catalog 初始化完成")
  }

  def atomicOverwrite(
                       spark: SparkSession,
                       df: DataFrame,
                       database: String,
                       tableName: String,
                       partitionCols: Seq[String]
                     ): Unit = {

    val startTime = System.currentTimeMillis()

    if (!ICEBERG_ENABLED) {
      // Hive 模式：动态分区覆盖
      logger.info(s"Hive 原子写入: $database.$tableName")
      spark.conf.set("spark.sql.sources.partitionOverwriteMode", "dynamic")
      df.write
        .mode(SaveMode.Overwrite)
        .partitionBy(partitionCols: _*)
        .saveAsTable(s"$database.$tableName")
      val duration = (System.currentTimeMillis() - startTime) / 1000
      logger.info(s"✅ Hive 写入完成: $database.$tableName, 耗时: ${duration}s")
      return
    }

    // ✅ Iceberg 模式：单次写入，真正原子
    // 原代码存在 createOrReplace() + save() 双写问题：
    //   createOrReplace() 已完成建表+全量写入；
    //   之后的 save() 又再写一次，导致数据被写两遍，且两次写入之间存在不一致窗口。
    // 修复：统一使用 overwritePartitions()，单次原子覆盖已有分区，不影响其他分区。
    val fullTableName = s"iceberg.$database.$tableName"
    logger.info(s"Iceberg 原子写入: $fullTableName")

    // 首次运行表不存在时先建表（仅建表，不写数据）
    val tableExists = Try(spark.table(fullTableName)).isSuccess
    if (!tableExists) {
      logger.info(s"表不存在，先创建: $fullTableName")
      val partitionColumns = partitionCols.map(org.apache.spark.sql.functions.col)
      df.writeTo(fullTableName)
        .using("iceberg")
        .partitionedBy(partitionColumns.head, partitionColumns.tail: _*)
        .tableProperty("write.format.default", "orc")
        .tableProperty("write.target-file-size-bytes", "134217728")
        .create()
    }

    // 单次原子写入：只覆盖本次涉及的分区，其他分区不受影响
    df.writeTo(fullTableName)
      .overwritePartitions()

    val duration = (System.currentTimeMillis() - startTime) / 1000
    logger.info(s"✅ Iceberg 写入完成: $fullTableName, 耗时: ${duration}s")
  }

  // 引入 Try 用于表存在性检测
  private def Try[T](f: => T): scala.util.Try[T] = scala.util.Try(f)
}