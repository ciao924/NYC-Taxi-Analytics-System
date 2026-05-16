package com.taxi.etl.ods

import com.taxi.etl.common.{ConfigManager, MetricsCollector, SparkSessionFactory}
import com.taxi.etl.models.JobContext
import com.taxi.etl.utils.{MonitorUtils, TaxiDataUtils}
import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.functions.{col, lpad, month, year}
import org.slf4j.LoggerFactory

object GreenOdsLoader {

  private val logger = LoggerFactory.getLogger(getClass)
  private val ODS_DATABASE = "nyc_taxi_ods"
  private val ODS_TABLE = "taxi_trip_green_ods"
  private val DATA_PATH = ConfigManager.getString("data.ods.path")

  def main(args: Array[String]): Unit = {
    val ctx = JobContext.fromArgs("Green_ODS_Loader", args)
    ctx.setMDC()

    val spark = SparkSessionFactory.create(ctx.jobName, enableHive = true)

    try {
      logger.info("=" * 80)
      logger.info("🟢 绿表 ODS 层数据加载（v4-lite）")
      logger.info("=" * 80)
      logger.info(s"目标表: $ODS_DATABASE.$ODS_TABLE")
      logger.info(s"数据路径: $DATA_PATH")
      logger.info(s"执行ID: ${ctx.executionId}")
      logger.info("=" * 80)

      MonitorUtils.printSystemResources()

      spark.sql(s"CREATE DATABASE IF NOT EXISTS $ODS_DATABASE")
      spark.sql(s"USE $ODS_DATABASE")

      val rawData = TaxiDataUtils.readGreenData(spark, DATA_PATH)
      logger.info(s"原始数据列数: ${rawData.columns.length}")

      // 【修复】分区字段保持 int 类型（与 Hive 表结构一致）
      val dataWithPartition = rawData
        .withColumn("year", year(col("lpep_pickup_datetime")).cast("int"))
        .withColumn("month", month(col("lpep_pickup_datetime")).cast("int"))
        .filter(col("year").isNotNull && col("month").isNotNull)

      // 【优化点2】分区完整性校验
      val inputPartitions = dataWithPartition.select("year", "month").distinct().count()
      require(inputPartitions > 0, "ODS写入分区为空，终止任务")

      val partitionStats = dataWithPartition.groupBy("year", "month").count().orderBy("year", "month").collect()
      logger.info("分区数据统计:")
      partitionStats.foreach { row =>
        logger.info(s"  year=${row.getInt(0)}, month=${row.getInt(1)}, count=${MonitorUtils.formatNumber(row.getLong(2))}")
      }

      createTableIfNotExists(spark)

      // 【优化点3】开启动态分区覆盖
      spark.conf.set("spark.sql.sources.partitionOverwriteMode", "dynamic")

      dataWithPartition.write
        .mode(SaveMode.Overwrite)
        .insertInto(s"$ODS_DATABASE.$ODS_TABLE")

      val totalCount = MonitorUtils.monitorTableSize(spark, ODS_DATABASE, ODS_TABLE, ctx.executionId)
      MetricsCollector.recordJobMetric("Green_ODS", ctx.executionId, "total_count", totalCount)

      logger.info(s"\n✅ 绿表加载完成，总记录数: ${MonitorUtils.formatNumber(totalCount)}")

    } catch {
      case e: Exception =>
        logger.error(s"\n❌ 加载失败: ${e.getMessage}", e)
        System.exit(1)
    } finally {
      ctx.clearMDC()
      spark.stop()
    }
  }

  def createTableIfNotExists(spark: org.apache.spark.sql.SparkSession): Unit = {
    val fullTableName = s"$ODS_DATABASE.$ODS_TABLE"
    if (!spark.catalog.tableExists(fullTableName)) {
      spark.sql(
        s"""
           |CREATE TABLE IF NOT EXISTS $fullTableName (
           |  VendorID INT, lpep_pickup_datetime TIMESTAMP, lpep_dropoff_datetime TIMESTAMP,
           |  store_and_fwd_flag STRING, RatecodeID BIGINT, PULocationID INT, DOLocationID INT,
           |  passenger_count BIGINT, trip_distance DOUBLE, fare_amount DOUBLE, extra DOUBLE,
           |  mta_tax DOUBLE, tip_amount DOUBLE, tolls_amount DOUBLE, ehail_fee DOUBLE,
           |  improvement_surcharge DOUBLE, total_amount DOUBLE, payment_type BIGINT,
           |  trip_type BIGINT, congestion_surcharge DOUBLE, cbd_congestion_fee DOUBLE
           |)
           |PARTITIONED BY (year INT, month INT)
           |STORED AS ORC
           |TBLPROPERTIES ('orc.compress'='SNAPPY', 'creator'='taxi_analytics_system_v4')
         """.stripMargin)
      logger.info("✅ 绿表创建成功")
    }
  }
}