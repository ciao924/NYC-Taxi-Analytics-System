package taxi

import org.apache.spark.sql.SaveMode
import org.apache.spark.sql.functions._
import taxi.DataUtil._

object DimTableSync {

  def main(args: Array[String]): Unit = {

    println("=" * 70)
    println("维度表同步 - MySQL → Hive DIM层")
    println("=" * 70)

    // 确保数据库存在
    spark.sql("CREATE DATABASE IF NOT EXISTS nyc_taxi_dim")
    println(s"\n✅ 数据库 nyc_taxi_dim 已就绪")
    println(s"ETL日期: $etl_date\n")

    // 维度表列表
    val dimTables = List(
      "dim_vendor",
      "dim_location",
      "dim_payment",
      "dim_ratecode",
      "dim_storage_flag",
      "dim_trip_type",
      "dim_fee_type"
    )

    // 批量同步维度表
    dimTables.foreach { tableName =>
      syncDimensionTable(tableName)
    }

    // 验证同步结果
    verifySyncResult()

    spark.stop()
  }

  /**
   * 同步单个维度表
   * @param tableName 表名
   */
  def syncDimensionTable(tableName: String): Unit = {
    println(s"同步表: $tableName")

    try {
      // 从MySQL读取数据
      val mysqlDF = loadMysql(s"SELECT * FROM $tableName")
      val recordCount = mysqlDF.count()
      println(s"   MySQL记录数: $recordCount")

      // 添加ETL日期分区字段
      val hiveDF = mysqlDF.withColumn("etl_date", lit(etl_date))

      // 写入Hive DIM层
      writeHive("nyc_taxi_dim", SaveMode.Overwrite, tableName, hiveDF, "etl_date")

      println(s"   ✅ 写入成功: nyc_taxi_dim.$tableName (分区: etl_date=$etl_date)\n")

    } catch {
      case e: Exception =>
        println(s"   ❌ 同步失败: ${e.getMessage}\n")
    }
  }

  /**
   * 验证同步结果
   */
  def verifySyncResult(): Unit = {
    println("=" * 70)
    println("验证同步结果")
    println("=" * 70)

    // 显示所有维度表
    println("\n所有维度表:")
    spark.sql("SHOW TABLES IN nyc_taxi_dim").show(false)

    // 统计各表记录数
    println("\n各维度表记录数:")
    val tables = Array("dim_vendor", "dim_location", "dim_payment", "dim_ratecode",
      "dim_storage_flag", "dim_trip_type", "dim_fee_type")

    tables.foreach { table =>
      try {
        val count = spark.sql(s"SELECT COUNT(*) FROM nyc_taxi_dim.$table").collect()(0).getLong(0)
        println(s"   $table: $count 条")
      } catch {
        case e: Exception => println(s"   $table: 查询失败")
      }
    }

    // 显示区域维度表样例数据
    println("\n区域维度表样例数据 (前5条):")
    spark.sql("SELECT location_id, borough_zh, zone_name_zh, service_zone_zh, is_airport FROM nyc_taxi_dim.dim_location LIMIT 5").show(false)
  }
}