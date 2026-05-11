package taxi

import org.apache.spark.sql.{DataFrame, SaveMode}
import org.apache.spark.sql.functions._
import taxi.DataUtil._

object CsvToHive {

  def main(args: Array[String]): Unit = {

    println("=" * 70)
    println("CSV文件导入Hive ODS层")
    println("=" * 70)

    // 配置参数
    val csvFilePath = "E:/development/spark_test/spark_test/src/main/java/data/yellow_taxi_2025-11_50万条_原始数据.csv"
    val hiveDB = "nyc_taxi_ods"
    val hiveTable = "yellow_taxi_trips_ods"

    println(s"\n配置参数:")
    println(s"   - CSV文件: $csvFilePath")
    println(s"   - 目标库: $hiveDB")
    println(s"   - 目标表: $hiveTable")
    println(s"   - ETL日期: $etl_date")

    // 1. 读取CSV文件
    println("\n1. 读取CSV文件...")

    val csvDF = spark.read
      .option("header", "true")           // 第一行作为列名
      .option("inferSchema", "true")      // 自动推断数据类型
      .option("encoding", "UTF-8")        // UTF-8编码
      .option("multiLine", "true")        // 支持多行
      .csv(csvFilePath)

    println(s"   读取完成，记录数: ${csvDF.count()}")
    println(s"   字段数: ${csvDF.columns.length}")
    println(s"   字段列表: ${csvDF.columns.mkString(", ")}")

    // 2. 数据预览
    println("\n2. 数据预览（前10条）:")
    csvDF.show(10, false)

    // 3. 数据类型检查
    println("\n3. 数据结构:")
    csvDF.printSchema()

    // 4. 添加分区字段
    println("\n4. 添加分区字段...")
    val hiveDF = csvDF.withColumn("etl_date", lit(etl_date))

    // 5. 确保数据库存在
    println("\n5. 创建Hive数据库（如果不存在）...")
    spark.sql(s"CREATE DATABASE IF NOT EXISTS $hiveDB")

    // 6. 写入Hive表
    println(s"\n6. 写入Hive表: $hiveDB.$hiveTable")

    hiveDF.write
      .mode(SaveMode.Overwrite)
      .partitionBy("etl_date")
      .saveAsTable(s"$hiveDB.$hiveTable")

    println(s"   ✅ 写入完成！")

    // 7. 验证写入结果
    println("\n7. 验证写入结果...")
    val resultDF = spark.sql(s"SELECT COUNT(*) as total FROM $hiveDB.$hiveTable WHERE etl_date='$etl_date'")
    resultDF.show()

    // 8. 查看分区信息
    println("\n8. 分区信息:")
    spark.sql(s"SHOW PARTITIONS $hiveDB.$hiveTable").show(false)

    // 9. 查看样例数据
    println("\n9. 验证数据样例:")
    spark.sql(s"SELECT * FROM $hiveDB.$hiveTable WHERE etl_date='$etl_date' LIMIT 5").show(false)

    println("\n" + "=" * 70)
    println("✅ CSV导入Hive完成！")
    println("=" * 70)

    spark.stop()
  }
}