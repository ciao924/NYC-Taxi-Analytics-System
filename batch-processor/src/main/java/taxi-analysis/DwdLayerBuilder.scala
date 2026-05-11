package taxi

import org.apache.spark.sql.{DataFrame, SaveMode}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, DoubleType}
import org.apache.spark.sql.expressions.Window
import taxi.DataUtil._

object DwdLayerBuilder {

  def main(args: Array[String]): Unit = {

    println("=" * 70)
    println("DWD层数据清洗（行业标准版）")
    println("=" * 70)
    println(s"ETL日期: $etl_date\n")

    // 1. 读取ODS数据
    println("1. 读取ODS数据...")
    val odsDF = spark.table("nyc_taxi_ods.yellow_taxi_trips_ods")
      .filter(col("etl_date") === etl_date)

    val originalCount = odsDF.count()
    println(s"   原始记录数: $originalCount")

    // 2. 读取维度表（注意：dim_location中is_airport等字段是String类型）
    println("\n2. 读取维度表...")

    val dimLocation = spark.table("nyc_taxi_dim.dim_location")
      .filter(col("etl_date") === etl_date)
      .select(
        col("location_id").cast(IntegerType),
        col("borough_zh"),
        col("zone_name_zh"),
        col("service_zone_zh"),
        col("zone_type"),
        // 将String类型的is_airport转换为Boolean类型
        when(col("is_airport") === "True", true)
          .when(col("is_airport") === "true", true)
          .otherwise(false).as("is_airport"),
        when(col("is_manhattan_core") === "True", true)
          .when(col("is_manhattan_core") === "true", true)
          .otherwise(false).as("is_manhattan_core")
      )

    val dimVendor = spark.table("nyc_taxi_dim.dim_vendor")
      .filter(col("etl_date") === etl_date)
      .select(col("vendor_id").cast(IntegerType), col("vendor_name_zh"))

    val dimPayment = spark.table("nyc_taxi_dim.dim_payment")
      .filter(col("etl_date") === etl_date)
      .select(
        col("payment_type").cast(IntegerType),
        col("payment_name_zh"),
        when(col("is_cashless") === "True", true)
          .when(col("is_cashless") === "true", true)
          .otherwise(false).as("is_cashless")
      )

    val dimRatecode = spark.table("nyc_taxi_dim.dim_ratecode")
      .filter(col("etl_date") === etl_date)
      .select(
        col("ratecode_id").cast(IntegerType),
        col("ratecode_name_zh"),
        col("rate_type")
      )

    println("   维度表加载完成")
    println(s"   dim_vendor记录数: ${dimVendor.count()}")
    println(s"   dim_payment记录数: ${dimPayment.count()}")
    println(s"   dim_ratecode记录数: ${dimRatecode.count()}")
    println(s"   dim_location记录数: ${dimLocation.count()}")

    // 3. 数据清洗（行业标准：空值直接删除）
    println("\n3. 执行数据清洗（行业标准）...")

    var cleanedDF = odsDF

    // 3.1 删除空值记录（使用na.drop）
    println("   - 删除空值记录...")
    val beforeNull = cleanedDF.count()
    cleanedDF = cleanedDF.na.drop()
    val afterNull = cleanedDF.count()
    println(s"     删除空值记录: ${beforeNull - afterNull} 条")

    // 3.2 过滤异常数据
    println("   - 过滤异常数据...")

    // 过滤异常距离
    cleanedDF = cleanedDF
      .filter(col("trip_distance") > 0 && col("trip_distance") <= 100)
    val afterDistance = cleanedDF.count()
    println(s"     过滤异常距离: ${afterNull - afterDistance} 条")

    // 过滤异常费用
    cleanedDF = cleanedDF
      .filter(col("total_amount") > 0 && col("total_amount") <= 500)
    val afterAmount = cleanedDF.count()
    println(s"     过滤异常费用: ${afterDistance - afterAmount} 条")

    // 过滤异常乘客数
    cleanedDF = cleanedDF
      .filter(col("passenger_count") >= 1 && col("passenger_count") <= 6)
    val afterPassenger = cleanedDF.count()
    println(s"     过滤异常乘客数: ${afterAmount - afterPassenger} 条")

    // 3.3 修复时间逻辑
    println("   - 修复时间逻辑...")
    cleanedDF = cleanedDF
      .filter(col("tpep_dropoff_datetime") > col("tpep_pickup_datetime"))
    val afterTime = cleanedDF.count()
    println(s"     修复时间逻辑: ${afterPassenger - afterTime} 条")

    // 3.4 计算行程时长
    cleanedDF = cleanedDF
      .withColumn("trip_duration_minutes",
        (unix_timestamp(col("tpep_dropoff_datetime")) -
          unix_timestamp(col("tpep_pickup_datetime"))) / 60)

    // 过滤异常时长
    cleanedDF = cleanedDF
      .filter(col("trip_duration_minutes") >= 1 && col("trip_duration_minutes") <= 180)
    val afterDuration = cleanedDF.count()
    println(s"     过滤异常时长: ${afterTime - afterDuration} 条")

    // 3.5 修复机场费逻辑
    println("   - 修复机场费逻辑...")
    val airportZones = Seq(132, 138)
    cleanedDF = cleanedDF
      .filter(!((col("Airport_fee") > 0 && !col("PULocationID").isin(airportZones: _*)) ||
        (col("Airport_fee") === 0 && col("PULocationID").isin(airportZones: _*) &&
          col("RatecodeID").isin(2, 3))))
    val afterAirport = cleanedDF.count()
    println(s"     修复机场费逻辑: ${afterDuration - afterAirport} 条")

    // 3.6 处理现金小费逻辑
    println("   - 处理现金小费逻辑...")
    cleanedDF = cleanedDF
      .withColumn("tip_amount_fixed",
        when(col("payment_type") === 2, 0.0)
          .otherwise(col("tip_amount")))
      .withColumn("total_amount_fixed",
        when(col("payment_type") === 2,
          col("total_amount") - col("tip_amount"))
          .otherwise(col("total_amount")))

    // 清洗统计
    val cleanedCount = cleanedDF.count()
    println(s"\n   清洗完成:")
    println(s"   - 清洗后记录数: $cleanedCount")
    println(s"   - 总删除记录数: ${originalCount - cleanedCount}")
    println(s"   - 保留比例: ${cleanedCount.toDouble / originalCount * 100}%")

    // 4. 添加主键ID
    println("\n4. 添加主键ID...")

    // 按时间排序并添加行号
    val dfWithId = cleanedDF
      .orderBy(col("tpep_pickup_datetime").asc)
      .withColumn("trip_id", row_number().over(Window.orderBy("tpep_pickup_datetime")))
      .withColumn("trip_id", col("trip_id").cast(IntegerType))

    // 5. 关联维度表（深度清洗）
    println("\n5. 关联维度表（深度清洗）...")

    // 关联vendor
    val withVendor = dfWithId
      .join(dimVendor, dfWithId("VendorID") === dimVendor("vendor_id"), "left")
      .drop(dimVendor("vendor_id"))

    // 关联payment
    val withPayment = withVendor
      .join(dimPayment, withVendor("payment_type") === dimPayment("payment_type"), "left")
      .drop(dimPayment("payment_type"))

    // 关联ratecode
    val withRatecode = withPayment
      .join(dimRatecode, withPayment("RatecodeID") === dimRatecode("ratecode_id"), "left")
      .drop(dimRatecode("ratecode_id"))

    // 关联上车区域
    val withPickupLocation = withRatecode
      .join(dimLocation.as("pu"), withRatecode("PULocationID") === col("pu.location_id"), "left")
      .drop(col("pu.location_id"))

    // 关联下车区域
    val withAll = withPickupLocation
      .join(dimLocation.as("do"), withPickupLocation("DOLocationID") === col("do.location_id"), "left")
      .drop(col("do.location_id"))

    // 选择最终字段
    val factDF = withAll.select(
      // 主键
      col("trip_id"),
      // 原始字段
      col("VendorID"),
      col("tpep_pickup_datetime"),
      col("tpep_dropoff_datetime"),
      col("passenger_count").cast(IntegerType),
      col("trip_distance"),
      col("RatecodeID").cast(IntegerType),
      col("store_and_fwd_flag"),
      col("PULocationID"),
      col("DOLocationID"),
      col("payment_type").cast(IntegerType),
      col("fare_amount"),
      col("extra"),
      col("mta_tax"),
      col("tip_amount_fixed").as("tip_amount"),
      col("tolls_amount"),
      col("improvement_surcharge"),
      col("total_amount_fixed").as("total_amount"),
      col("congestion_surcharge"),
      col("Airport_fee"),
      col("cbd_congestion_fee"),
      col("trip_duration_minutes"),
      // 维度字段（中文）
      coalesce(col("vendor_name_zh"), lit("未知")).as("vendor_name"),
      coalesce(col("payment_name_zh"), lit("未知")).as("payment_name"),
      coalesce(col("is_cashless"), lit(false)).as("is_cashless"),
      coalesce(col("ratecode_name_zh"), lit("未知")).as("ratecode_name"),
      coalesce(col("rate_type"), lit("未知")).as("rate_type"),
      // 上车区域信息
      coalesce(col("pu.borough_zh"), lit("未知区域")).as("pu_borough"),
      coalesce(col("pu.zone_name_zh"), lit("未知区域")).as("pu_zone"),
      coalesce(col("pu.service_zone_zh"), lit("未知")).as("pu_service_zone"),
      coalesce(col("pu.zone_type"), lit("未知")).as("pu_zone_type"),
      coalesce(col("pu.is_airport"), lit(false)).as("pu_is_airport"),
      coalesce(col("pu.is_manhattan_core"), lit(false)).as("pu_is_manhattan_core"),
      // 下车区域信息
      coalesce(col("do.borough_zh"), lit("未知区域")).as("do_borough"),
      coalesce(col("do.zone_name_zh"), lit("未知区域")).as("do_zone"),
      coalesce(col("do.service_zone_zh"), lit("未知")).as("do_service_zone"),
      coalesce(col("do.zone_type"), lit("未知")).as("do_zone_type"),
      coalesce(col("do.is_airport"), lit(false)).as("do_is_airport"),
      coalesce(col("do.is_manhattan_core"), lit(false)).as("do_is_manhattan_core"),
      // 时间特征
      to_date(col("tpep_pickup_datetime")).as("pickup_date"),
      hour(col("tpep_pickup_datetime")).as("pickup_hour"),
      dayofweek(col("tpep_pickup_datetime")).as("pickup_dayofweek"),
      when(dayofweek(col("tpep_pickup_datetime")).isin(1, 7), true).otherwise(false).as("is_weekend"),
      // 派生字段
      when(col("pu.is_airport") === true || col("do.is_airport") === true, "机场行程")
        .when(col("rate_type") === "shared", "拼车行程")
        .when(col("pu.borough_zh") =!= col("do.borough_zh") &&
          col("pu.borough_zh") =!= "未知区域" &&
          col("do.borough_zh") =!= "未知区域", "跨区行程")
        .when(col("pu.is_manhattan_core") === true, "曼哈顿核心区行程")
        .otherwise("普通行程").as("trip_type"),
      // 分区字段
      col("etl_date")
    )

    // 6. 数据质量验证
    println("\n6. 数据质量验证...")
    val factCount = factDF.count()
    println(s"   - DWD记录数: $factCount")

    // 空值检查
    val nullFields = factDF.columns.map { colName =>
      val nullCount = factDF.filter(col(colName).isNull).count()
      (colName, nullCount)
    }.filter(_._2 > 0)

    if (nullFields.isEmpty) {
      println("   - ✅ 无空值字段")
    } else {
      println(s"   - ⚠️ 存在 ${nullFields.length} 个字段有空值")
      nullFields.take(5).foreach { case (colName, count) =>
        println(s"      $colName: $count 个")
      }
    }

    // 主键唯一性检查
    val duplicateCount = factDF.groupBy("trip_id").count().filter("count > 1").count()
    if (duplicateCount == 0) {
      println("   - ✅ 主键唯一性: 通过")
    } else {
      println(s"   - ⚠️ 主键唯一性: 发现 $duplicateCount 个重复ID")
    }

    // 7. 写入DWD层
    println("\n7. 写入DWD层...")
    spark.sql("CREATE DATABASE IF NOT EXISTS nyc_taxi_dwd")

    factDF.write
      .mode(SaveMode.Overwrite)
      .partitionBy("etl_date")
      .saveAsTable("nyc_taxi_dwd.fact_taxi_trips")

    println(s"   ✅ 写入完成: nyc_taxi_dwd.fact_taxi_trips")

  }
}