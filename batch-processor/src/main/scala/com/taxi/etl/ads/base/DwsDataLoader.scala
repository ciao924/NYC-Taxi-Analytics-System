package com.taxi.etl.ads.base

import com.taxi.etl.common.{MetricsCollector, SmartCacheLite}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.LoggerFactory

case class DwsDataBundle(
                          dailyDf: DataFrame,
                          hourlyDf: DataFrame,
                          zoneDf: DataFrame,
                          feeDf: DataFrame,
                          vendorDf: DataFrame,
                          metadata: DwsMetadata
                        )

case class DwsMetadata(
                        dailyCount: Long,
                        hourlyCount: Long,
                        zoneCount: Long,
                        feeCount: Long,
                        vendorCount: Long,
                        totalTrips: Long,
                        totalRevenue: Double
                      )

object DwsDataLoader {

  private val logger = LoggerFactory.getLogger(getClass)
  private val DWS_DATABASE = "nyc_taxi_dws"

  def load(
            spark: SparkSession,
            startDate: String,
            endDate: String,
            cache: SmartCacheLite,
            metrics: MetricsCollector
          ): DwsDataBundle = {

    logger.info("=" * 60)
    logger.info("📦 开始加载 DWS 层数据")
    logger.info(s"   日期范围: $startDate ~ $endDate")
    logger.info("=" * 60)

    val startTime = System.currentTimeMillis()

    // 【优化点1】使用单次查询获取所有 counts
    val metadata = getMetadataOptimized(spark, startDate, endDate)

    logger.info(s"   日汇总表: ${formatNumber(metadata.dailyCount)} 条")
    logger.info(s"   小时汇总表: ${formatNumber(metadata.hourlyCount)} 条")
    logger.info(s"   区域热力表: ${formatNumber(metadata.zoneCount)} 条")
    logger.info(s"   费用汇总表: ${formatNumber(metadata.feeCount)} 条")
    logger.info(s"   供应商汇总表: ${formatNumber(metadata.vendorCount)} 条")

    // 【优化点2】加载数据并缓存（复用）
    val dailyDf = loadDailyData(spark, startDate, endDate, cache).persist()
    val hourlyDf = loadHourlyData(spark, startDate, endDate, cache).persist()
    val zoneDf = loadZoneData(spark, startDate, endDate, cache).persist()
    val feeDf = loadFeeData(spark, startDate, endDate, cache).persist()
    val vendorDf = loadVendorData(spark, startDate, endDate, cache).persist()

    metrics.record("dws.load.daily_count", metadata.dailyCount.toDouble)
    metrics.record("dws.load.hourly_count", metadata.hourlyCount.toDouble)
    metrics.record("dws.load.zone_count", metadata.zoneCount.toDouble)
    metrics.record("dws.load.fee_count", metadata.feeCount.toDouble)
    metrics.record("dws.load.vendor_count", metadata.vendorCount.toDouble)

    val duration = (System.currentTimeMillis() - startTime) / 1000
    logger.info(s"✅ DWS 数据加载完成，耗时: ${duration}s")
    logger.info("=" * 60)

    DwsDataBundle(dailyDf, hourlyDf, zoneDf, feeDf, vendorDf, metadata)
  }

  /**
   * 【优化点3】单次查询获取所有元数据
   */
  private def getMetadataOptimized(
                                    spark: SparkSession,
                                    startDate: String,
                                    endDate: String
                                  ): DwsMetadata = {
    // 使用单次 SQL 获取所有 counts
    val sql = s"""
      SELECT
        (SELECT COUNT(*) FROM $DWS_DATABASE.dws_trip_daily
         WHERE stat_date BETWEEN '$startDate' AND '$endDate') AS daily_cnt,
        (SELECT COUNT(*) FROM $DWS_DATABASE.dws_trip_hourly
         WHERE stat_date BETWEEN '$startDate' AND '$endDate') AS hourly_cnt,
        (SELECT COUNT(*) FROM $DWS_DATABASE.dws_trip_zone_daily
         WHERE stat_date BETWEEN '$startDate' AND '$endDate') AS zone_cnt,
        (SELECT COUNT(*) FROM $DWS_DATABASE.dws_trip_fee_daily
         WHERE stat_date BETWEEN '$startDate' AND '$endDate') AS fee_cnt,
        (SELECT COUNT(*) FROM $DWS_DATABASE.dws_trip_vendor_daily
         WHERE stat_date BETWEEN '$startDate' AND '$endDate') AS vendor_cnt,
        (SELECT COALESCE(SUM(total_trips), 0) FROM $DWS_DATABASE.dws_trip_daily
         WHERE stat_date BETWEEN '$startDate' AND '$endDate') AS total_trips,
        (SELECT COALESCE(SUM(total_revenue), 0) FROM $DWS_DATABASE.dws_trip_daily
         WHERE stat_date BETWEEN '$startDate' AND '$endDate') AS total_revenue
    """

    val row = spark.sql(sql).first()

    DwsMetadata(
      dailyCount = if (row.isNullAt(0)) 0L else row.getLong(0),
      hourlyCount = if (row.isNullAt(1)) 0L else row.getLong(1),
      zoneCount = if (row.isNullAt(2)) 0L else row.getLong(2),
      feeCount = if (row.isNullAt(3)) 0L else row.getLong(3),
      vendorCount = if (row.isNullAt(4)) 0L else row.getLong(4),
      totalTrips = if (row.isNullAt(5)) 0L else row.getLong(5),
      totalRevenue = if (row.isNullAt(6)) 0.0 else row.getDouble(6)
    )
  }

  private def loadDailyData(
                             spark: SparkSession,
                             startDate: String,
                             endDate: String,
                             cache: SmartCacheLite
                           ): DataFrame = {
    val df = spark.sql(s"""
      SELECT
        stat_date,
        total_trips,
        total_revenue,
        avg_amount,
        avg_distance,
        avg_trip_duration,
        total_tip,
        avg_tip,
        total_airport_trips,
        peak_hours
      FROM $DWS_DATABASE.dws_trip_daily
      WHERE stat_date BETWEEN '$startDate' AND '$endDate'
    """)
    cache.cacheIfNeeded(df, "dws_daily")
  }

  private def loadHourlyData(
                              spark: SparkSession,
                              startDate: String,
                              endDate: String,
                              cache: SmartCacheLite
                            ): DataFrame = {
    val df = spark.sql(s"""
      SELECT
        stat_date,
        pickup_hour,
        trip_count,
        avg_amount,
        avg_distance,
        avg_tip,
        total_revenue
      FROM $DWS_DATABASE.dws_trip_hourly
      WHERE stat_date BETWEEN '$startDate' AND '$endDate'
    """)
    cache.cacheIfNeeded(df, "dws_hourly")
  }

  private def loadZoneData(
                            spark: SparkSession,
                            startDate: String,
                            endDate: String,
                            cache: SmartCacheLite
                          ): DataFrame = {
    val df = spark.sql(s"""
      SELECT
        stat_date,
        location_id,
        zone_name,
        borough,
        service_zone,
        pickup_count,
        dropoff_count,
        total_revenue_pickup
      FROM $DWS_DATABASE.dws_trip_zone_daily
      WHERE stat_date BETWEEN '$startDate' AND '$endDate'
    """)
    cache.cacheIfNeeded(df, "dws_zone")
  }

  private def loadFeeData(
                           spark: SparkSession,
                           startDate: String,
                           endDate: String,
                           cache: SmartCacheLite
                         ): DataFrame = {
    val df = spark.sql(s"""
      SELECT
        stat_date,
        taxi_type,
        pu_borough,
        pu_service_zone,
        payment_name,
        is_cashless,
        trip_count,
        total_amount,
        avg_amount,
        total_fare,
        total_extra,
        total_mta_tax,
        total_tip,
        total_tolls,
        total_improvement,
        total_congestion,
        total_airport_fee,
        total_cbd_fee,
        avg_fare,
        avg_tip,
        tip_rate,
        cashless_rate
      FROM $DWS_DATABASE.dws_trip_fee_daily
      WHERE stat_date BETWEEN '$startDate' AND '$endDate'
    """)
    cache.cacheIfNeeded(df, "dws_fee")
  }

  private def loadVendorData(
                              spark: SparkSession,
                              startDate: String,
                              endDate: String,
                              cache: SmartCacheLite
                            ): DataFrame = {
    val df = spark.sql(s"""
      SELECT
        stat_date,
        vendor_id,
        vendor_name,
        trip_count,
        total_revenue,
        avg_amount,
        avg_distance
      FROM $DWS_DATABASE.dws_trip_vendor_daily
      WHERE stat_date BETWEEN '$startDate' AND '$endDate'
    """)
    cache.cacheIfNeeded(df, "dws_vendor")
  }

  private def formatNumber(num: Long): String = {
    if (num >= 1000000) f"${num / 1000000.0}%.1fM"
    else if (num >= 1000) f"${num / 1000.0}%.1fK"
    else num.toString
  }
}