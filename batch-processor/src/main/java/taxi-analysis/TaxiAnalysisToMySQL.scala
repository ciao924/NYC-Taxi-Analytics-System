package taxi.analysis

import org.apache.spark.sql.{DataFrame, SaveMode}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window
import taxi.DataUtil
import taxi.DataUtil._

object TaxiAnalysisToMySQL {

  def main(args: Array[String]): Unit = {

    println("=" * 70)
    println("出租车数据分析 - DWD层 + 维度表 → MySQL")
    println("=" * 70)
    println(s"ETL日期: $etl_date\n")

    // 1. 读取DWD数据（已关联维度表）
    val dwdDF = spark.table("nyc_taxi_dwd.fact_taxi_trips")
      .filter(col("etl_date") === etl_date)

    val totalCount = dwdDF.count()
    println(s"DWD记录数: $totalCount")

    // ==================== 计算所有分析维度 ====================

    println("\n1. 计算核心指标卡片...")
    val kpiDF = calculateKPIs(dwdDF)

    println("\n2. 计算小时级出行分布...")
    val hourlyDF = calculateHourlyDistribution(dwdDF)

    println("\n3. 计算周内出行对比...")
    val weekdayDF = calculateWeekdayDistribution(dwdDF)

    println("\n4. 计算周末/工作日对比...")
    val weekendDF = calculateWeekendComparison(dwdDF)

    println("\n5. 计算时段平均费用...")
    val hourlyAmountDF = calculateHourlyAmount(dwdDF)

    println("\n6. 计算支付方式分布...")
    val paymentDF = calculatePaymentDistribution(dwdDF)

    println("\n7. 计算支付方式费用对比...")
    val paymentAmountDF = calculatePaymentAmountComparison(dwdDF)

    println("\n8. 计算小费金额分布...")
    val tipDistributionDF = calculateTipDistribution(dwdDF)

    println("\n9. 计算小费与距离关系...")
    val tipDistanceDF = calculateTipDistanceRelation(dwdDF)

    println("\n10. 计算上车热点分布...")
    val pickupHotspotsDF = calculatePickupHotspots(dwdDF)

    println("\n11. 计算下车热点分布...")
    val dropoffHotspotsDF = calculateDropoffHotspots(dwdDF)

    println("\n12. 计算区域流量...")
    val boroughFlowDF = calculateBoroughFlow(dwdDF)

    println("\n13. 计算费用构成...")
    val feeCompositionDF = calculateFeeComposition(dwdDF)

    println("\n14. 计算各项费用占比...")
    val feePercentageDF = calculateFeePercentage(dwdDF)

    println("\n15. 计算行程距离分布...")
    val distanceDistributionDF = calculateDistanceDistribution(dwdDF)

    println("\n16. 计算乘客数量分布...")
    val passengerDistributionDF = calculatePassengerDistribution(dwdDF)

    println("\n17. 计算距离-费用关系...")
    val distanceAmountDF = calculateDistanceAmountRelation(dwdDF)

    println("\n18. 计算行程时长分布...")
    val durationDistributionDF = calculateDurationDistribution(dwdDF)

    println("\n19. 计算收入贡献度...")
    val revenueContributionDF = calculateRevenueContribution(dwdDF)

    println("\n20. 计算时段-区域交叉分析...")
    val hourZoneCrossDF = calculateHourZoneCross(dwdDF)

    println("\n21. 计算供应商分析...")
    val vendorAnalysisDF = calculateVendorAnalysis(dwdDF)

    println("\n22. 计算费率代码分析...")
    val ratecodeAnalysisDF = calculateRatecodeAnalysis(dwdDF)

    println("\n23. 计算机场行程分析...")
    val airportAnalysisDF = calculateAirportAnalysis(dwdDF)

    // ==================== 写入MySQL ====================
    println("\n" + "=" * 70)
    println("写入MySQL数据库")
    println("=" * 70)

    writeToMySQL(kpiDF, "analysis_kpi_daily")
    writeToMySQL(hourlyDF, "analysis_hourly_distribution")
    writeToMySQL(weekdayDF, "analysis_weekday_distribution")
    writeToMySQL(weekendDF, "analysis_weekend_comparison")
    writeToMySQL(hourlyAmountDF, "analysis_hourly_amount")
    writeToMySQL(paymentDF, "analysis_payment_distribution")
    writeToMySQL(paymentAmountDF, "analysis_payment_amount")
    writeToMySQL(tipDistributionDF, "analysis_tip_distribution")
    writeToMySQL(tipDistanceDF, "analysis_tip_distance")
    writeToMySQL(pickupHotspotsDF, "analysis_pickup_hotspots")
    writeToMySQL(dropoffHotspotsDF, "analysis_dropoff_hotspots")
    writeToMySQL(boroughFlowDF, "analysis_borough_flow")
    writeToMySQL(feeCompositionDF, "analysis_fee_composition")
    writeToMySQL(feePercentageDF, "analysis_fee_percentage")
    writeToMySQL(distanceDistributionDF, "analysis_distance_distribution")
    writeToMySQL(passengerDistributionDF, "analysis_passenger_distribution")
    writeToMySQL(distanceAmountDF, "analysis_distance_amount")
    writeToMySQL(durationDistributionDF, "analysis_duration_distribution")
    writeToMySQL(revenueContributionDF, "analysis_revenue_contribution")
    writeToMySQL(hourZoneCrossDF, "analysis_hour_zone_cross")
    writeToMySQL(vendorAnalysisDF, "analysis_vendor")
    writeToMySQL(ratecodeAnalysisDF, "analysis_ratecode")
    writeToMySQL(airportAnalysisDF, "analysis_airport")

    println("\n" + "=" * 70)
    println("✅ 数据分析完成！")
    println("=" * 70)

    spark.stop()
  }

  // ==================== 计算函数 ====================

  /**
   * 1. 核心指标卡片
   */
  def calculateKPIs(df: DataFrame): DataFrame = {
    val total = df.count()
    val kpi = df.agg(
      count("*").as("total_trips"),
      sum("total_amount").as("total_revenue"),
      round(avg("trip_distance"), 2).as("avg_distance"),
      round(avg("tip_amount"), 2).as("avg_tip"),
      round(sum(when(col("payment_name") === "信用卡", 1).otherwise(0)).cast("double") / total * 100, 2).as("credit_card_rate")
    )

    // 计算高峰期
    val peakHourRow = df.groupBy("pickup_hour")
      .count()
      .orderBy(desc("count"))
      .limit(1)
      .select(col("pickup_hour"))
      .collect()

    val peakHour = if (peakHourRow.nonEmpty) peakHourRow(0).getInt(0) else 0

    kpi.withColumn("stat_date", lit(current_date()))
      .withColumn("peak_hour", lit(peakHour))
      .withColumn("avg_amount_per_trip", round(col("total_revenue") / col("total_trips"), 2))
      .withColumn("update_time", current_timestamp())
  }

  /**
   * 2. 小时级出行分布
   */
  def calculateHourlyDistribution(df: DataFrame): DataFrame = {
    df.groupBy("pickup_hour")
      .agg(
        count("*").as("trip_count"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("trip_distance"), 2).as("avg_distance"),
        round(avg("tip_amount"), 2).as("avg_tip")
      )
      .withColumn("stat_date", lit(current_date()))
      .orderBy(col("pickup_hour").asc)
  }

  /**
   * 3. 周内出行对比
   */
  def calculateWeekdayDistribution(df: DataFrame): DataFrame = {
    df.groupBy("pickup_dayofweek")
      .agg(
        count("*").as("trip_count"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("trip_distance"), 2).as("avg_distance")
      )
      .withColumn("stat_date", lit(current_date()))
      .orderBy(col("pickup_dayofweek").asc)
  }

  /**
   * 4. 周末 vs 工作日对比
   */
  def calculateWeekendComparison(df: DataFrame): DataFrame = {
    df.groupBy("is_weekend")
      .agg(
        count("*").as("trip_count"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("trip_distance"), 2).as("avg_distance"),
        round(avg("tip_amount"), 2).as("avg_tip")
      )
      .withColumn("stat_date", lit(current_date()))
      .withColumn("day_type", when(col("is_weekend") === true, "周末").otherwise("工作日"))
      .drop("is_weekend")
      .orderBy(col("day_type").desc)
  }

  /**
   * 5. 时段平均费用
   */
  def calculateHourlyAmount(df: DataFrame): DataFrame = {
    df.groupBy("pickup_hour")
      .agg(
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("trip_distance"), 2).as("avg_distance"),
        count("*").as("trip_count")
      )
      .withColumn("stat_date", lit(current_date()))
      .orderBy(col("pickup_hour").asc)
  }

  /**
   * 6. 支付方式分布
   */
  def calculatePaymentDistribution(df: DataFrame): DataFrame = {
    val total = df.count()
    df.groupBy("payment_name")
      .agg(
        count("*").as("trip_count"),
        round(sum("total_amount"), 2).as("total_amount"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("tip_amount"), 2).as("avg_tip")
      )
      .withColumn("percentage", round(col("trip_count") / total * 100, 2))
      .withColumn("stat_date", lit(current_date()))
      .orderBy(col("trip_count").desc)
  }

  /**
   * 7. 支付方式费用对比
   */
  def calculatePaymentAmountComparison(df: DataFrame): DataFrame = {
    df.groupBy("payment_name")
      .agg(
        round(avg("fare_amount"), 2).as("avg_fare"),
        round(avg("total_amount"), 2).as("avg_total"),
        round(avg("tip_amount"), 2).as("avg_tip")
      )
      .withColumn("stat_date", lit(current_date()))
      .orderBy(col("payment_name").asc)
  }

  /**
   * 8. 小费金额分布
   */
  def calculateTipDistribution(df: DataFrame): DataFrame = {
    df.withColumn("tip_range",
        when(col("tip_amount") === 0, "无小费")
          .when(col("tip_amount") < 2, "小于$2")
          .when(col("tip_amount") < 5, "$2-$5")
          .when(col("tip_amount") < 10, "$5-$10")
          .otherwise("$10以上")
      ).groupBy("tip_range")
      .agg(
        count("*").as("trip_count"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("tip_amount"), 2).as("avg_tip")
      )
      .withColumn("stat_date", lit(current_date()))
      .orderBy(
        when(col("tip_range") === "无小费", 1)
          .when(col("tip_range") === "小于$2", 2)
          .when(col("tip_range") === "$2-$5", 3)
          .when(col("tip_range") === "$5-$10", 4)
          .otherwise(5)
      )
  }

  /**
   * 9. 小费与距离关系
   */
  def calculateTipDistanceRelation(df: DataFrame): DataFrame = {
    df.select(
      col("trip_distance"),
      col("tip_amount"),
      col("total_amount")
    ).withColumn("stat_date", lit(current_date()))
  }

  /**
   * 10. 上车热点分布
   */
  def calculatePickupHotspots(df: DataFrame): DataFrame = {
    df.groupBy("pu_zone", "pu_borough")
      .agg(
        count("*").as("trip_count"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("trip_distance"), 2).as("avg_distance"),
        round(avg("tip_amount"), 2).as("avg_tip")
      )
      .withColumn("stat_date", lit(current_date()))
      .withColumn("rank_position", row_number().over(Window.orderBy(col("trip_count").desc)))
      .filter(col("rank_position") <= 30)
      .withColumn("zone_type", lit("pickup"))
      .select(
        col("stat_date"),
        col("zone_type"),
        col("pu_zone").as("zone_name"),
        col("pu_borough").as("borough"),
        col("trip_count"),
        col("avg_amount"),
        col("avg_distance"),
        col("avg_tip"),
        col("rank_position")
      )
      .orderBy(col("rank_position").asc)
  }

  /**
   * 11. 下车热点分布
   */
  def calculateDropoffHotspots(df: DataFrame): DataFrame = {
    df.groupBy("do_zone", "do_borough")
      .agg(
        count("*").as("trip_count"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("trip_distance"), 2).as("avg_distance"),
        round(avg("tip_amount"), 2).as("avg_tip")
      )
      .withColumn("stat_date", lit(current_date()))
      .withColumn("rank_position", row_number().over(Window.orderBy(col("trip_count").desc)))
      .filter(col("rank_position") <= 30)
      .withColumn("zone_type", lit("dropoff"))
      .select(
        col("stat_date"),
        col("zone_type"),
        col("do_zone").as("zone_name"),
        col("do_borough").as("borough"),
        col("trip_count"),
        col("avg_amount"),
        col("avg_distance"),
        col("avg_tip"),
        col("rank_position")
      )
      .orderBy(col("rank_position").asc)
  }

  /**
   * 12. 区域流量桑基图
   */
  def calculateBoroughFlow(df: DataFrame): DataFrame = {
    df.groupBy("pu_borough", "do_borough")
      .agg(
        count("*").as("flow_count"),
        round(avg("total_amount"), 2).as("avg_amount")
      )
      .withColumn("stat_date", lit(current_date()))
      .filter(col("pu_borough") =!= "未知区域" && col("do_borough") =!= "未知区域")
      .orderBy(col("flow_count").desc)
  }

  /**
   * 13. 费用构成瀑布图数据
   */
  def calculateFeeComposition(df: DataFrame): DataFrame = {
    val feeItems = Seq(
      ("fare_amount", "基础车费"),
      ("extra", "附加费"),
      ("mta_tax", "MTA税"),
      ("tip_amount", "小费"),
      ("tolls_amount", "过路费"),
      ("improvement_surcharge", "改善附加费"),
      ("congestion_surcharge", "拥堵附加费"),
      ("Airport_fee", "机场费")
    )

    val avgFees = feeItems.map { case (colName, feeName) =>
      val avgValue = df.agg(round(avg(colName), 2)).collect()(0).getDouble(0)
      (feeName, avgValue)
    }

    import spark.implicits._
    avgFees.toSeq.toDF("fee_name", "avg_amount")
      .withColumn("stat_date", lit(current_date()))
      .orderBy(col("avg_amount").desc)
  }

  /**
   * 14. 各项费用占比
   */
  def calculateFeePercentage(df: DataFrame): DataFrame = {
    import spark.implicits._

    val feeItems = Seq(
      ("fare_amount", "基础车费"),
      ("extra", "附加费"),
      ("mta_tax", "MTA税"),
      ("tip_amount", "小费"),
      ("tolls_amount", "过路费"),
      ("improvement_surcharge", "改善附加费"),
      ("congestion_surcharge", "拥堵附加费"),
      ("Airport_fee", "机场费")
    )

    // 使用agg一次性计算所有平均值
    val aggExpr = feeItems.map { case (colName, _) =>
      round(avg(colName), 2).alias(s"avg_$colName")
    } :+ round(avg("total_amount"), 2).alias("avg_total")

    val avgRow = df.agg(aggExpr.head, aggExpr.tail: _*).collect()(0)
    val totalAvg = avgRow.getDouble(avgRow.fieldIndex("avg_total"))

    // 构建结果
    val results = feeItems.map { case (colName, feeName) =>
      val avgValue = avgRow.getDouble(avgRow.fieldIndex(s"avg_$colName"))
      val percentage = if (totalAvg > 0) avgValue / totalAvg * 100 else 0.0
      (feeName, avgValue, percentage)
    }

    results.toSeq.toDF("fee_name", "avg_amount", "percentage")
      .withColumn("stat_date", lit(current_date()))
      .withColumn("percentage", round(col("percentage"), 2))
      .orderBy(col("percentage").desc)
  }

  /**
   * 15. 行程距离分布
   */
  def calculateDistanceDistribution(df: DataFrame): DataFrame = {
    df.withColumn("distance_range",
        when(col("trip_distance") <= 1, "0-1英里")
          .when(col("trip_distance") <= 3, "1-3英里")
          .when(col("trip_distance") <= 5, "3-5英里")
          .when(col("trip_distance") <= 10, "5-10英里")
          .otherwise("10+英里")
      ).groupBy("distance_range")
      .agg(
        count("*").as("trip_count"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("tip_amount"), 2).as("avg_tip")
      )
      .withColumn("stat_date", lit(current_date()))
      .orderBy(
        when(col("distance_range") === "0-1英里", 1)
          .when(col("distance_range") === "1-3英里", 2)
          .when(col("distance_range") === "3-5英里", 3)
          .when(col("distance_range") === "5-10英里", 4)
          .otherwise(5)
      )
  }

  /**
   * 16. 乘客数量分布
   */
  def calculatePassengerDistribution(df: DataFrame): DataFrame = {
    df.groupBy("passenger_count")
      .agg(
        count("*").as("trip_count"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("tip_amount"), 2).as("avg_tip")
      )
      .withColumn("stat_date", lit(current_date()))
      .orderBy(col("passenger_count").asc)
  }

  /**
   * 17. 距离-费用关系
   */
  def calculateDistanceAmountRelation(df: DataFrame): DataFrame = {
    df.select(
        col("trip_distance"),
        col("total_amount"),
        col("tip_amount")
      ).sample(false, 0.03)
      .withColumn("stat_date", lit(current_date()))
  }

  /**
   * 18. 行程时长分布
   */
  def calculateDurationDistribution(df: DataFrame): DataFrame = {
    df.withColumn("duration_range",
        when(col("trip_duration_minutes") <= 10, "0-10分钟")
          .when(col("trip_duration_minutes") <= 20, "10-20分钟")
          .when(col("trip_duration_minutes") <= 30, "20-30分钟")
          .when(col("trip_duration_minutes") <= 60, "30-60分钟")
          .otherwise("60+分钟")
      ).groupBy("duration_range")
      .agg(
        count("*").as("trip_count"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("trip_distance"), 2).as("avg_distance")
      )
      .withColumn("stat_date", lit(current_date()))
      .orderBy(
        when(col("duration_range") === "0-10分钟", 1)
          .when(col("duration_range") === "10-20分钟", 2)
          .when(col("duration_range") === "20-30分钟", 3)
          .when(col("duration_range") === "30-60分钟", 4)
          .otherwise(5)
      )
  }

  /**
   * 19. 收入贡献度分析
   */
  def calculateRevenueContribution(df: DataFrame): DataFrame = {
    val zoneRevenue = df.groupBy("pu_zone")
      .agg(
        count("*").as("trip_count"),
        round(sum("total_amount"), 2).as("total_revenue")
      )
      .orderBy(col("total_revenue").desc)

    val totalRevenue = zoneRevenue.agg(sum("total_revenue")).collect()(0).getDouble(0)

    zoneRevenue.withColumn("revenue_percentage", round(col("total_revenue") / totalRevenue * 100, 2))
      .withColumn("cumulative_percentage", round(sum("revenue_percentage").over(Window.orderBy(col("total_revenue").desc)), 2))
      .withColumn("stat_date", lit(current_date()))
      .limit(50)
      .orderBy(col("total_revenue").desc)
  }

  /**
   * 20. 时段-区域交叉分析（修复orderBy问题）
   */
  def calculateHourZoneCross(df: DataFrame): DataFrame = {
    df.groupBy("pickup_hour", "pu_zone")
      .agg(count("*").as("trip_count"))
      .withColumn("stat_date", lit(current_date()))
      .filter(col("pu_zone") =!= "未知区域")
      .orderBy(col("pickup_hour").asc, col("trip_count").desc)
      .limit(500)
  }

  /**
   * 21. 供应商分析
   */
  def calculateVendorAnalysis(df: DataFrame): DataFrame = {
    val total = df.count()
    df.groupBy("vendor_name")
      .agg(
        count("*").as("trip_count"),
        round(sum("total_amount"), 2).as("total_revenue"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("trip_distance"), 2).as("avg_distance"),
        round(avg("tip_amount"), 2).as("avg_tip")
      )
      .withColumn("stat_date", lit(current_date()))
      .withColumn("percentage", round(col("trip_count") / total * 100, 2))
      .orderBy(col("trip_count").desc)
  }

  /**
   * 22. 费率代码分析
   */
  def calculateRatecodeAnalysis(df: DataFrame): DataFrame = {
    val total = df.count()
    df.groupBy("ratecode_name")
      .agg(
        count("*").as("trip_count"),
        round(sum("total_amount"), 2).as("total_revenue"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("trip_distance"), 2).as("avg_distance")
      )
      .withColumn("stat_date", lit(current_date()))
      .withColumn("percentage", round(col("trip_count") / total * 100, 2))
      .orderBy(col("trip_count").desc)
  }

  /**
   * 23. 机场行程分析
   */
  def calculateAirportAnalysis(df: DataFrame): DataFrame = {
    df.withColumn("airport_trip",
        when(col("pu_is_airport") === true, "机场出发")
          .when(col("do_is_airport") === true, "机场到达")
          .otherwise("非机场")
      ).groupBy("airport_trip")
      .agg(
        count("*").as("trip_count"),
        round(avg("total_amount"), 2).as("avg_amount"),
        round(avg("trip_distance"), 2).as("avg_distance"),
        round(sum("Airport_fee"), 2).as("total_airport_fees")
      )
      .withColumn("stat_date", lit(current_date()))
      .orderBy(col("trip_count").desc)
  }

  // ==================== 写入MySQL ====================

  def writeToMySQL(df: DataFrame, tableName: String): Unit = {
    try {
      df.write
        .mode(SaveMode.Append)
        .format("jdbc")
        .options(DataUtil.getJdbcOptions("taxi_analysis"))
        .option("dbtable", tableName)
        .save()

      println(s"   ✅ 写入成功: $tableName (${df.count()} 条)")
    } catch {
      case e: Exception => println(s"   ⚠️ 写入失败: $tableName - ${e.getMessage}")
    }
  }
}