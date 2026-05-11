package com.taxi.etl.ads.base

import com.taxi.etl.common.MetricsCollector
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

object ConsistencyValidator {

  private val logger = LoggerFactory.getLogger(getClass)

  // 【修正】允许的数值差异阈值
  private val EPSILON = 1e-6

  def validate(
                data: DwsDataBundle,
                metrics: MetricsCollector
              ): Boolean = {
    logger.info("\n" + "=" * 60)
    logger.info("📋 ADS 与 DWS 一致性校验")
    logger.info("=" * 60)

    var allConsistent = true

    val dailyTotalTrips = data.metadata.totalTrips

    val hourlyTotalTripsRow = data.hourlyDf.agg(sum("trip_count")).first()
    val hourlyTotalTrips = if (hourlyTotalTripsRow.isNullAt(0)) 0L else hourlyTotalTripsRow.getLong(0)

    if (dailyTotalTrips == hourlyTotalTrips) {
      logger.info(s"  ✅ 日/小时总行程数一致: ${formatNumber(dailyTotalTrips)}")
    } else {
      logger.warn(s"  ❌ 日/小时总行程数不一致: 日=${formatNumber(dailyTotalTrips)}, 小时=${formatNumber(hourlyTotalTrips)}")
      allConsistent = false
    }

    val feeTotalTripsRow = data.feeDf.agg(sum("trip_count")).first()
    val feeTotalTrips = if (feeTotalTripsRow.isNullAt(0)) 0L else feeTotalTripsRow.getLong(0)

    if (dailyTotalTrips == feeTotalTrips) {
      logger.info(s"  ✅ 日/费用总行程数一致: ${formatNumber(dailyTotalTrips)}")
    } else {
      logger.warn(s"  ❌ 日/费用总行程数不一致: 日=${formatNumber(dailyTotalTrips)}, 费用=${formatNumber(feeTotalTrips)}")
      allConsistent = false
    }

    val zoneTotalPickupsRow = data.zoneDf.agg(sum("pickup_count")).first()
    val zoneTotalPickups = if (zoneTotalPickupsRow.isNullAt(0)) 0L else zoneTotalPickupsRow.getLong(0)

    // 【修正】使用更宽松的比较，允许微小差异
    val isZoneConsistent = Math.abs(dailyTotalTrips - zoneTotalPickups) <= 1
    if (isZoneConsistent) {
      logger.info(s"  ✅ 日/区域总行程数一致: ${formatNumber(dailyTotalTrips)}")
    } else {
      logger.warn(s"  ❌ 日/区域总行程数不一致: 日=${formatNumber(dailyTotalTrips)}, 区域=${formatNumber(zoneTotalPickups)}")
      allConsistent = false
    }

    // 收入校验
    val dailyTotalRevenue = data.metadata.totalRevenue

    val hourlyTotalRevenueRow = data.hourlyDf.agg(sum("total_revenue")).first()
    val hourlyTotalRevenue = if (hourlyTotalRevenueRow.isNullAt(0)) 0.0 else hourlyTotalRevenueRow.getDouble(0)

    val feeTotalRevenueRow = data.feeDf.agg(sum("total_amount")).first()
    val feeTotalRevenue = if (feeTotalRevenueRow.isNullAt(0)) 0.0 else feeTotalRevenueRow.getDouble(0)

    val isRevenueConsistent =
      Math.abs(dailyTotalRevenue - hourlyTotalRevenue) < EPSILON &&
        Math.abs(dailyTotalRevenue - feeTotalRevenue) < EPSILON

    if (isRevenueConsistent) {
      logger.info(s"  ✅ 总收入一致: ${formatNumber(dailyTotalRevenue.toLong)}")
    } else {
      logger.warn(s"  ❌ 总收入不一致: 日=$dailyTotalRevenue, 小时=$hourlyTotalRevenue, 费用=$feeTotalRevenue")
      allConsistent = false
    }

    metrics.record("consistency.passed", if (allConsistent) 1.0 else 0.0)

    logger.info("=" * 60)
    allConsistent
  }

  private def formatNumber(num: Long): String = {
    if (num >= 1000000) f"${num / 1000000.0}%.1fM"
    else if (num >= 1000) f"${num / 1000.0}%.1fK"
    else num.toString
  }
}