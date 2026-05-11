package taxi

import org.apache.spark.sql.functions._
import taxi.DataUtil._

object ReadDimensionTables {

  def main(args: Array[String]): Unit = {

    println("=" * 70)
    println("维度表结构分析")
    println("=" * 70)
    println(s"ETL日期: $etl_date\n")

    // ==================== 1. 查看所有维度表 ====================
    println("1. 查看所有维度表:")
    println("-" * 50)
    spark.sql("SHOW TABLES IN nyc_taxi_dim").show(false)

    // ==================== 2. 供应商维度表 ====================
    println("\n2. 供应商维度表 (dim_vendor):")
    println("-" * 50)
    val dimVendor = spark.table("nyc_taxi_dim.dim_vendor")
      .filter(col("etl_date") === 20260322)

    println("   表结构:")
    dimVendor.printSchema()

    println("   数据样例:")
    dimVendor.show(10, false)

    println("   数据统计:")
    dimVendor.groupBy("vendor_id").count().show(false)

    // ==================== 3. 支付方式维度表 ====================
    println("\n3. 支付方式维度表 (dim_payment):")
    println("-" * 50)
    val dimPayment = spark.table("nyc_taxi_dim.dim_payment")
      .filter(col("etl_date") === 20260322)

    println("   表结构:")
    dimPayment.printSchema()

    println("   数据样例:")
    dimPayment.show(10, false)

    println("   支付方式映射:")
    dimPayment.select("payment_type", "payment_name_zh", "is_cashless").show(false)

    // ==================== 4. 费率代码维度表 ====================
    println("\n4. 费率代码维度表 (dim_ratecode):")
    println("-" * 50)
    val dimRatecode = spark.table("nyc_taxi_dim.dim_ratecode")
      .filter(col("etl_date") === 20260322)

    println("   表结构:")
    dimRatecode.printSchema()

    println("   数据样例:")
    dimRatecode.show(10, false)

    println("   费率代码映射:")
    dimRatecode.select("ratecode_id", "ratecode_name_zh", "rate_type", "is_airport").show(false)

    // ==================== 5. 区域维度表 ====================
    println("\n5. 区域维度表 (dim_location) - 核心维度:")
    println("-" * 50)
    val dimLocation = spark.table("nyc_taxi_dim.dim_location")
      .filter(col("etl_date") === 20260322)

    println("   表结构:")
    dimLocation.printSchema()

    println("   数据样例（前10条）:")
    dimLocation.select(
      "location_id", "borough_zh", "zone_name_zh", "service_zone_zh", "is_airport", "is_manhattan_core"
    ).show(10, false)

    println("   区域统计:")
    println("   行政区分布:")
    dimLocation.groupBy("borough_zh").count().orderBy(desc("count")).show(false)

    println("   服务区类型分布:")
    dimLocation.groupBy("service_zone_zh").count().show(false)

    println("   机场区域:")
    dimLocation.filter(col("is_airport") === true)
      .select("location_id", "zone_name_zh", "borough_zh")
      .show(false)

    println("   曼哈顿核心区:")
    dimLocation.filter(col("is_manhattan_core") === true)
      .select("location_id", "zone_name_zh", "borough_zh")
      .show(10, false)

    // ==================== 6. 存储标志维度表 ====================
    println("\n6. 存储标志维度表 (dim_storage_flag):")
    println("-" * 50)
    val dimStorageFlag = spark.table("nyc_taxi_dim.dim_storage_flag")
      .filter(col("etl_date") === 20260322)

    println("   表结构:")
    dimStorageFlag.printSchema()

    println("   数据样例:")
    dimStorageFlag.show(false)

    // ==================== 7. 行程类型维度表 ====================
    println("\n7. 行程类型维度表 (dim_trip_type):")
    println("-" * 50)
    val dimTripType = spark.table("nyc_taxi_dim.dim_trip_type")
      .filter(col("etl_date") === 20260322)

    println("   表结构:")
    dimTripType.printSchema()

    println("   数据样例:")
    dimTripType.show(false)

    // ==================== 8. 费用类型维度表 ====================
    println("\n8. 费用类型维度表 (dim_fee_type):")
    println("-" * 50)
    val dimFeeType = spark.table("nyc_taxi_dim.dim_fee_type")
      .filter(col("etl_date") === 20260322)

    println("   表结构:")
    dimFeeType.printSchema()

    println("   数据样例:")
    dimFeeType.show(false)

    // ==================== 9. 汇总信息 ====================
    println("\n" + "=" * 70)
    println("维度表汇总信息")
    println("=" * 70)

    val dimTables = Seq(
      ("dim_vendor", dimVendor),
      ("dim_payment", dimPayment),
      ("dim_ratecode", dimRatecode),
      ("dim_location", dimLocation),
      ("dim_storage_flag", dimStorageFlag),
      ("dim_trip_type", dimTripType),
      ("dim_fee_type", dimFeeType)
    )

    println("\n各维度表记录数:")
    dimTables.foreach { case (name, df) =>
      val count = df.count()
      println(f"   $name%-20s : $count%5d 条")
    }

    // ==================== 10. 字段映射关系总结 ====================
    println("\n" + "=" * 70)
    println("字段映射关系总结")
    println("=" * 70)

    println("""
              |ODS字段              | 维度表            | 关联字段        | 输出字段
              |---------------------|-------------------|-----------------|-------------------------
              |VendorID            | dim_vendor        | vendor_id       | vendor_name_zh
              |payment_type        | dim_payment       | payment_type    | payment_name_zh, is_cashless
              |RatecodeID          | dim_ratecode      | ratecode_id     | ratecode_name_zh, rate_type
              |PULocationID        | dim_location      | location_id     | borough_zh, zone_name_zh, service_zone_zh, is_airport, is_manhattan_core
              |DOLocationID        | dim_location      | location_id     | borough_zh, zone_name_zh, service_zone_zh, is_airport, is_manhattan_core
    """.stripMargin)

    // ==================== 11. 维度表关联SQL示例 ====================
    println("\n" + "=" * 70)
    println("维度表关联SQL示例")
    println("=" * 70)

    val joinExample = """
    -- 示例：关联所有维度表
    SELECT
        t.trip_id,
        t.VendorID,
        v.vendor_name_zh,
        t.payment_type,
        p.payment_name_zh,
        p.is_cashless,
        t.RatecodeID,
        r.ratecode_name_zh,
        r.rate_type,
        t.PULocationID,
        pu.zone_name_zh as pu_zone,
        pu.borough_zh as pu_borough,
        pu.is_airport as pu_is_airport,
        t.DOLocationID,
        do.zone_name_zh as do_zone,
        do.borough_zh as do_borough,
        do.is_airport as do_is_airport
    FROM nyc_taxi_ods.yellow_taxi_trips_ods t
    LEFT JOIN nyc_taxi_dim.dim_vendor v ON t.VendorID = v.vendor_id
    LEFT JOIN nyc_taxi_dim.dim_payment p ON t.payment_type = p.payment_type
    LEFT JOIN nyc_taxi_dim.dim_ratecode r ON t.RatecodeID = r.ratecode_id
    LEFT JOIN nyc_taxi_dim.dim_location pu ON t.PULocationID = pu.location_id
    LEFT JOIN nyc_taxi_dim.dim_location do ON t.DOLocationID = do.location_id
    WHERE t.etl_date = '20260322'
    LIMIT 10;
    """

    println(joinExample)

    // ==================== 12. 检查维度表数据完整性 ====================
    println("\n" + "=" * 70)
    println("维度表数据完整性检查")
    println("=" * 70)

    println("\n检查区域维度表是否有空值:")
    val locationNullCheck = dimLocation.select(
      sum(when(col("borough_zh").isNull, 1).otherwise(0)).as("borough_null"),
      sum(when(col("zone_name_zh").isNull, 1).otherwise(0)).as("zone_null"),
      sum(when(col("service_zone_zh").isNull, 1).otherwise(0)).as("service_zone_null")
    ).collect()(0)

    println(s"   borough_zh空值数: ${locationNullCheck.getLong(0)}")
    println(s"   zone_name_zh空值数: ${locationNullCheck.getLong(1)}")
    println(s"   service_zone_zh空值数: ${locationNullCheck.getLong(2)}")

    println("\n检查区域ID覆盖范围:")
    val locationIds = dimLocation.select("location_id").collect().map(_.getInt(0)).sorted
    println(s"   最小ID: ${locationIds.head}")
    println(s"   最大ID: ${locationIds.last}")
    println(s"   总数: ${locationIds.length}")

    // 检查是否有缺失的ID（如265个区域）
    val expectedIds = (1 to 265).toSet
    val actualIds = locationIds.toSet
    val missingIds = expectedIds -- actualIds
    if (missingIds.nonEmpty) {
      println(s"   缺失的区域ID: ${missingIds.toList.sorted.mkString(", ")}")
    } else {
      println("   ✅ 所有区域ID（1-265）都存在")
    }

    println("\n" + "=" * 70)
    println("✅ 维度表结构分析完成")
    println("=" * 70)

    spark.stop()
  }
}