package com.taxi.etl.dws

import com.taxi.etl.common._
import com.taxi.etl.models._
import com.taxi.etl.utils.MonitorUtils
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object DwsLayerBuilder {

  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    Version.printVersion()

    val result = build(args)
    if (!result.isSuccess) {
      logger.error(s"❌ DWS 构建失败: ${result.errorMessage}")
      System.exit(1)
    }
    logger.info(s"✅ DWS 构建成功: ${result.summary}")
  }

  def build(args: Array[String]): BuildResult = {
    val spark = SparkSessionFactory.create("DWS_Layer_Builder_v4_lite")
    val ctx = JobContext.fromArgs("DWS", args)
    ctx.setMDC()

    val startTime = System.currentTimeMillis()
    val smartCache = SmartCacheLite()

    val metrics = new MetricsCollector()

    try {
      val targetYear = ConfigManager.getInt("common.target-year")
      val targetMonths = ConfigManager.getIntList("common.target-months")

      spark.sql("CREATE DATABASE IF NOT EXISTS nyc_taxi_dws")

      logger.info("=" * 80)
      logger.info(s"🏗️ DWS 层数据构建 - ${Version.VERSION}")
      logger.info("=" * 80)
      logger.info(s"执行ID: ${ctx.executionId}")
      logger.info("=" * 80)

      // 【优化1】只选择需要的列，减少内存占用
      // 分区字段使用 int 类型
      val dwdRaw = spark.table("nyc_taxi_dwd.fact_taxi_trips")
        .filter(col("year") === targetYear)
        .filter(col("month").isin(targetMonths: _*))
        .select(
          "VendorID", "pickup_date", "pickup_hour", "pickup_datetime",
          "PULocationID", "DOLocationID", "total_amount", "trip_distance",
          "tip_amount", "trip_duration_minutes", "is_airport", "taxi_type",
          "payment_name", "is_cashless", "fare_amount", "extra", "mta_tax",
          "tolls_amount", "improvement_surcharge", "congestion_surcharge",
          "Airport_fee", "cbd_congestion_fee"
        )

      // 【优化2】不缓存全部数据，使用流式处理
      val dwdCount = dwdRaw.count()
      logger.info(s"DWD 源数据记录数: ${MonitorUtils.formatNumber(dwdCount)}")

      // ==================== 加载维度表 ====================
      val locationDim = spark.table("nyc_taxi_dim.dim_location")

      val locationPu = locationDim.select(
        col("location_id").as("pu_location_id"),
        col("borough_zh").as("pu_borough"),
        col("zone_name_zh").as("pu_zone"),
        col("service_zone_zh").as("pu_service_zone")
      )

      val locationDo = locationDim.select(
        col("location_id").as("do_location_id"),
        col("borough_zh").as("do_borough"),
        col("zone_name_zh").as("do_zone"),
        col("service_zone_zh").as("do_service_zone")
      )

      // 关联上车区域（使用广播）
      val withPuZone = dwdRaw
        .join(broadcast(locationPu), col("PULocationID") === col("pu_location_id"), "left")
        .withColumn("pu_borough", coalesce(col("pu_borough"), lit("未知")))
        .withColumn("pu_zone", coalesce(col("pu_zone"), lit("未知区域")))
        .withColumn("pu_service_zone", coalesce(col("pu_service_zone"), lit("未知")))
        .drop("pu_location_id")

      // 关联下车区域（使用广播）
      val withDoZone = withPuZone
        .join(broadcast(locationDo), col("DOLocationID") === col("do_location_id"), "left")
        .withColumn("do_borough", coalesce(col("do_borough"), lit("未知")))
        .withColumn("do_zone", coalesce(col("do_zone"), lit("未知区域")))
        .withColumn("do_service_zone", coalesce(col("do_service_zone"), lit("未知")))
        .drop("do_location_id")

      logger.info("区域维度关联完成")

      // ==================== 表1: 日汇总表 ====================
      logger.info("构建 dws_trip_daily...")

      // 【优化3】使用 agg 一次性计算所有指标，避免多次扫描
      val dailyBaseStats = withDoZone
        .groupBy(col("pickup_date").as("stat_date"))
        .agg(
          count("*").as("total_trips"),
          round(sum("total_amount"), 2).as("total_revenue"),
          round(avg("total_amount"), 2).as("avg_amount"),
          round(avg("trip_distance"), 2).as("avg_distance"),
          round(avg("trip_duration_minutes"), 2).as("avg_trip_duration"),
          round(sum("tip_amount"), 2).as("total_tip"),
          round(avg("tip_amount"), 2).as("avg_tip"),
          sum(when(col("is_airport") === "True", 1).otherwise(0)).as("total_airport_trips")
        )

      // 计算高峰小时（使用窗口函数，但只扫描一次）
      val hourlyTripCount = withDoZone
        .groupBy(col("pickup_date").as("stat_date"), col("pickup_hour"))
        .agg(count("*").as("hour_trip_count"))

      val peakHourWindow = Window.partitionBy("stat_date").orderBy(col("hour_trip_count").desc)
      val dailyPeakHours = hourlyTripCount
        .withColumn("rank", dense_rank().over(peakHourWindow))
        .filter(col("rank") === 1)
        .groupBy("stat_date")
        .agg(collect_list("pickup_hour").as("peak_hours"))

      val dailyStats = dailyBaseStats.join(dailyPeakHours, Seq("stat_date"), "left")
        .orderBy("stat_date")

      logger.info(s"日汇总表记录数: ${dailyStats.count()}")
      IcebergTableManager.atomicOverwrite(spark, dailyStats, "nyc_taxi_dws", "dws_trip_daily", Seq("stat_date"))

      // ==================== 表2: 小时汇总表 ====================
      logger.info("构建 dws_trip_hourly...")
      val hourlyStats = withDoZone
        .groupBy(col("pickup_date").as("stat_date"), col("pickup_hour"))
        .agg(
          count("*").as("trip_count"),
          round(avg("total_amount"), 2).as("avg_amount"),
          round(avg("trip_distance"), 2).as("avg_distance"),
          round(avg("tip_amount"), 2).as("avg_tip"),
          round(sum("total_amount"), 2).as("total_revenue")
        ).orderBy("stat_date", "pickup_hour")

      logger.info(s"小时汇总表记录数: ${hourlyStats.count()}")
      IcebergTableManager.atomicOverwrite(spark, hourlyStats, "nyc_taxi_dws", "dws_trip_hourly", Seq("stat_date"))

      // ==================== 表3: 上车区域热力汇总表 ====================
      logger.info("构建 dws_trip_zone_pickup_daily...")
      val pickupZoneStats = withDoZone
        .groupBy(
          col("pickup_date").as("stat_date"),
          col("PULocationID").as("location_id"),
          col("pu_zone").as("zone_name"),
          col("pu_borough").as("borough"),
          col("pu_service_zone").as("service_zone")
        )
        .agg(
          count("*").as("pickup_count"),
          round(sum("total_amount"), 2).as("total_revenue_pickup")
        )
        .orderBy(col("stat_date"), col("pickup_count").desc)

      logger.info(s"上车区域热力表记录数: ${pickupZoneStats.count()}")
      IcebergTableManager.atomicOverwrite(spark, pickupZoneStats, "nyc_taxi_dws", "dws_trip_zone_pickup_daily", Seq("stat_date"))

      // ==================== 表4: 下车区域热力汇总表 ====================
      logger.info("构建 dws_trip_zone_dropoff_daily...")
      val dropoffZoneStats = withDoZone
        .groupBy(
          col("pickup_date").as("stat_date"),
          col("DOLocationID").as("location_id"),
          col("do_zone").as("zone_name"),
          col("do_borough").as("borough"),
          col("do_service_zone").as("service_zone")
        )
        .agg(
          count("*").as("dropoff_count"),
          round(sum("total_amount"), 2).as("total_revenue_dropoff")
        )
        .orderBy(col("stat_date"), col("dropoff_count").desc)

      logger.info(s"下车区域热力表记录数: ${dropoffZoneStats.count()}")
      IcebergTableManager.atomicOverwrite(spark, dropoffZoneStats, "nyc_taxi_dws", "dws_trip_zone_dropoff_daily", Seq("stat_date"))

      // ==================== 表5: 费用汇总宽表 ====================
      logger.info("构建 dws_trip_fee_daily（费用汇总宽表）...")
      val feeWideStats = withDoZone
        .groupBy(
          col("pickup_date").as("stat_date"),
          col("taxi_type"),
          col("pu_borough"),
          col("pu_service_zone"),
          col("payment_name"),
          col("is_cashless")
        )
        .agg(
          count("*").as("trip_count"),
          round(sum("total_amount"), 2).as("total_amount"),
          round(avg("total_amount"), 2).as("avg_amount"),
          round(sum("fare_amount"), 2).as("total_fare"),
          round(sum("extra"), 2).as("total_extra"),
          round(sum("mta_tax"), 2).as("total_mta_tax"),
          round(sum("tip_amount"), 2).as("total_tip"),
          round(sum("tolls_amount"), 2).as("total_tolls"),
          round(sum("improvement_surcharge"), 2).as("total_improvement"),
          round(sum("congestion_surcharge"), 2).as("total_congestion"),
          round(sum("Airport_fee"), 2).as("total_airport_fee"),
          round(sum("cbd_congestion_fee"), 2).as("total_cbd_fee"),
          round(avg("fare_amount"), 2).as("avg_fare"),
          round(avg("tip_amount"), 2).as("avg_tip"),
          round(sum(when(col("tip_amount") > 0, 1).otherwise(0)) * 100 / count("*"), 2).as("tip_rate"),
          round(sum(when(col("is_cashless") === "True", 1).otherwise(0)) * 100 / count("*"), 2).as("cashless_rate")
        )
        .orderBy(col("stat_date"), col("trip_count").desc)

      logger.info(s"费用汇总宽表记录数: ${feeWideStats.count()}")
      IcebergTableManager.atomicOverwrite(spark, feeWideStats, "nyc_taxi_dws", "dws_trip_fee_daily", Seq("stat_date"))

      // ==================== 表6: 供应商汇总表 ====================
      logger.info("构建 dws_trip_vendor_daily...")
      val vendorDim = spark.table("nyc_taxi_dim.dim_vendor")
      val vendorStats = withDoZone
        .join(vendorDim, col("VendorID") === col("vendor_id"), "left")
        .withColumn("vendor_name", coalesce(col("vendor_name_zh"), lit("未知供应商")))
        .groupBy(
          col("pickup_date").as("stat_date"),
          col("VendorID").as("vendor_id"),
          col("vendor_name")
        )
        .agg(
          count("*").as("trip_count"),
          round(sum("total_amount"), 2).as("total_revenue"),
          round(avg("total_amount"), 2).as("avg_amount"),
          round(avg("trip_distance"), 2).as("avg_distance")
        )
        .orderBy(col("stat_date"), col("trip_count").desc)

      logger.info(s"供应商汇总表记录数: ${vendorStats.count()}")
      IcebergTableManager.atomicOverwrite(spark, vendorStats, "nyc_taxi_dws", "dws_trip_vendor_daily", Seq("stat_date"))

      logger.info("✅ 6张 DWS 表写入完成")
      logger.info("   - dws_trip_daily (日汇总)")
      logger.info("   - dws_trip_hourly (小时汇总)")
      logger.info("   - dws_trip_zone_pickup_daily (上车区域热力)")
      logger.info("   - dws_trip_zone_dropoff_daily (下车区域热力)")
      logger.info("   - dws_trip_fee_daily (费用汇总宽表)")
      logger.info("   - dws_trip_vendor_daily (供应商汇总)")

      metrics.recordJobMetric("DWS", ctx.executionId, "source_row_count", dwdCount)

      val duration = (System.currentTimeMillis() - startTime) / 1000
      logger.info(metrics.generateReport())

      BuildResult.success("dws_layer", dwdCount, duration)

    } catch {
      case e: Exception =>
        logger.error(s"DWS 构建失败: ${e.getMessage}", e)
        BuildResult.failure("dws_layer", e.getMessage, (System.currentTimeMillis() - startTime) / 1000)
    } finally {
      smartCache.uncacheAll()
      CacheManager.uncacheTracked()
      ctx.clearMDC()
      spark.stop()
    }
  }
}