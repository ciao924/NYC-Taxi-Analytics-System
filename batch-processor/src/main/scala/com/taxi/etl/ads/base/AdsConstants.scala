package com.taxi.etl.ads.base

/**
 * ADS 层常量配置
 * 符合数仓规范手册 6.1 - 6.7
 */
object AdsConstants {

  // ==================== 表名常量 ====================
  object TableNames {
    val KPI_DAILY = "analysis_kpi_daily"
    val HOURLY_DISTRIBUTION = "analysis_hourly_distribution"
    val WEEKDAY_ANALYSIS = "analysis_weekday_analysis"
    val PAYMENT_ANALYSIS = "analysis_payment_analysis"
    val PICKUP_HOTSPOTS = "analysis_pickup_hotspots"
    val DROPOFF_HOTSPOTS = "analysis_dropoff_hotspots"
    val BOROUGH_FLOW = "analysis_borough_flow"
    val FEE_COMPOSITION = "analysis_fee_composition"
    val FEE_PERCENTAGE = "analysis_fee_percentage"
    val FEE_BY_BOROUGH = "analysis_fee_by_borough"
    val FEE_TREND = "analysis_fee_trend"
    val FEE_BY_TAXI_TYPE = "analysis_fee_by_taxi_type"
    val DISTANCE_DISTRIBUTION = "analysis_distance_distribution"
    val DURATION_DISTRIBUTION = "analysis_duration_distribution"
    val PASSENGER_DISTRIBUTION = "analysis_passenger_distribution"
    val REVENUE_CONTRIBUTION = "analysis_revenue_contribution"
    val AIRPORT = "analysis_airport"
    val VENDOR = "analysis_vendor"
  }

  // ==================== 唯一键常量 ====================
  object UniqueKeys {
    val SINGLE_DATE = Seq("stat_date")
    val DATE_HOUR = Seq("stat_date", "hour_of_day")
    val DATE_DAY_OF_WEEK = Seq("stat_date", "day_of_week")
    val DATE_PAYMENT = Seq("stat_date", "payment_name")
    val DATE_ZONE = Seq("stat_date", "zone_name")
    val DATE_BOROUGH_FLOW = Seq("stat_date", "pu_borough", "do_borough")
    val DATE_FEE_CODE = Seq("stat_date", "fee_code")
    val DATE_BOROUGH = Seq("stat_date", "borough")
    val DATE_TAXI_TYPE = Seq("stat_date", "taxi_type")
    val DATE_DISTANCE_RANGE = Seq("stat_date", "distance_range")
    val DATE_DURATION_RANGE = Seq("stat_date", "duration_range")
    val DATE_PASSENGER = Seq("stat_date", "passenger_count")
    val DATE_PU_ZONE = Seq("stat_date", "pu_zone")
    val DATE_AIRPORT_TRIP = Seq("stat_date", "airport_trip")
    val DATE_VENDOR = Seq("stat_date", "vendor_name")
  }

  // ==================== 范围检查规则 ====================
  object RangeRules {
    val KPI_RULES = Map(
      "total_trips" -> (0.0, 10000000.0),
      "total_revenue" -> (0.0, 100000000.0),
      "avg_fare" -> (0.0, 1000.0),
      "avg_distance" -> (0.0, 120.0),
      "avg_tip" -> (0.0, 500.0)
    )

    val TRIP_COUNT_RULES = Map(
      "trip_count" -> (0.0, 10000000.0)
    )

    val TOTAL_REVENUE_RULES = Map(
      "total_revenue" -> (0.0, 100000000.0)
    )
  }

  // ==================== 最少预期记录数 ====================
  object MinExpectedRows {
    val KPI_DAILY = 90
    val HOURLY_DISTRIBUTION = 2160
    val WEEKDAY_ANALYSIS = 630
    val PAYMENT_ANALYSIS = 630
    val AIRPORT = 180
    val VENDOR = 180
    val PICKUP_HOTSPOTS = 23850
    val DROPOFF_HOTSPOTS = 23850
    val BOROUGH_FLOW = 2250
    val FEE_COMPOSITION = 810
    val FEE_PERCENTAGE = 810
    val FEE_BY_BOROUGH = 450
    val FEE_TREND = 90
    val FEE_BY_TAXI_TYPE = 180
    val DISTANCE_DISTRIBUTION = 450
    val DURATION_DISTRIBUTION = 450
    val PASSENGER_DISTRIBUTION = 540
    val REVENUE_CONTRIBUTION = 4500
  }

  // ==================== 数值字段（需要填充0） ====================
  object NumericFields {
    val KPI_FIELDS = Seq("total_trips", "total_revenue", "avg_fare", "avg_distance", "avg_duration", "total_tip", "avg_tip", "airport_trips")
    val HOURLY_FIELDS = Seq("trip_count", "avg_fare", "avg_tip", "total_revenue")
    val PAYMENT_FIELDS = Seq("trip_count", "total_amount", "avg_amount", "total_tip", "avg_tip", "trip_ratio")
    val ZONE_FIELDS = Seq("pickup_count", "dropoff_count", "total_revenue")
    val FEE_FIELDS = Seq("total_amount")
    val FEE_PERCENTAGE_FIELDS = Seq("percentage")
    val VENDOR_FIELDS = Seq("trip_count", "total_revenue", "avg_fare", "avg_distance", "revenue_ratio")
    val DISTRIBUTION_FIELDS = Seq("trip_count", "avg_distance", "avg_duration")
  }

  // ==================== 字符串字段（需要填充"未知"） ====================
  object StringFields {
    val ZONE_FIELDS = Seq("zone_name", "borough", "service_zone")
    val BOROUGH_FIELDS = Seq("pu_borough", "do_borough")
    val PAYMENT_FIELDS = Seq("payment_name")
    val VENDOR_FIELDS = Seq("vendor_name")
    val FEE_FIELDS = Seq("fee_code", "fee_name")
  }

  object QualityThresholds {
    val STRICT_RATIO = 1.0
    val NORMAL_RATIO = 0.95
    val LOOSE_RATIO = 0.85
    val VERY_LOOSE_RATIO = 0.70

    val TABLE_THRESHOLDS: Map[String, Double] = Map(
      TableNames.KPI_DAILY -> STRICT_RATIO,
      TableNames.HOURLY_DISTRIBUTION -> NORMAL_RATIO,
      TableNames.WEEKDAY_ANALYSIS -> NORMAL_RATIO,
      TableNames.PAYMENT_ANALYSIS -> NORMAL_RATIO,
      TableNames.AIRPORT -> NORMAL_RATIO,
      TableNames.VENDOR -> NORMAL_RATIO,
      TableNames.PICKUP_HOTSPOTS -> LOOSE_RATIO,
      TableNames.DROPOFF_HOTSPOTS -> LOOSE_RATIO,
      TableNames.BOROUGH_FLOW -> LOOSE_RATIO,
      TableNames.REVENUE_CONTRIBUTION -> VERY_LOOSE_RATIO,
      TableNames.FEE_COMPOSITION -> NORMAL_RATIO,
      TableNames.FEE_PERCENTAGE -> NORMAL_RATIO,
      TableNames.FEE_BY_BOROUGH -> LOOSE_RATIO,
      TableNames.FEE_BY_TAXI_TYPE -> NORMAL_RATIO,
      TableNames.DISTANCE_DISTRIBUTION -> NORMAL_RATIO,
      TableNames.DURATION_DISTRIBUTION -> NORMAL_RATIO,
      TableNames.PASSENGER_DISTRIBUTION -> NORMAL_RATIO,
      TableNames.FEE_TREND -> NORMAL_RATIO
    )

    def getThreshold(tableName: String): Double = {
      TABLE_THRESHOLDS.getOrElse(tableName, NORMAL_RATIO)
    }
  }
}