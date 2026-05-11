package com.taxi.etl.dwd

import com.taxi.etl.common._
import com.taxi.etl.models._
import com.taxi.etl.exception.QualityCheckException
import com.taxi.etl.utils.MonitorUtils
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{IntegerType, StringType}
import org.slf4j.LoggerFactory

object DwdLayerBuilder {

  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    Version.printVersion()

    val result = build(args)
    if (!result.isSuccess) {
      logger.error(s"❌ DWD 构建失败: ${result.errorMessage}")
      System.exit(1)
    }
    logger.info(s"✅ DWD 构建成功: ${result.summary}")
  }

  def build(args: Array[String]): BuildResult = {
    val spark = SparkSessionFactory.create("DWD_Layer_Builder_v4_lite")
    val ctx = JobContext.fromArgs("DWD", args)
    ctx.setMDC()

    val startTime = System.currentTimeMillis()
    val smartCache = SmartCacheLite()

    val metrics = new MetricsCollector()

    try {
      val targetYear = ConfigManager.getInt("common.target-year")
      val targetMonths = ConfigManager.getIntList("common.target-months")

      spark.sql("CREATE DATABASE IF NOT EXISTS nyc_taxi_dwd")

      logger.info("=" * 80)
      logger.info(s"🏗️ DWD 层数据构建 - ${Version.VERSION}")
      logger.info("=" * 80)
      logger.info(s"执行ID: ${ctx.executionId}")
      logger.info("策略: 小表广播 | 行为缓存 | AQE倾斜处理")
      logger.info("=" * 80)

      // ==================== 阶段1: 读取 ODS 数据并合并 ====================
      val yellowDf = spark.table("nyc_taxi_ods.taxi_trip_yellow_ods")
        .filter(col("year") === targetYear)
        .filter(col("month").isin(targetMonths: _*))
        .withColumn("taxi_type", lit("yellow"))
        .withColumn("pickup_datetime", col("tpep_pickup_datetime"))
        .withColumn("dropoff_datetime", col("tpep_dropoff_datetime"))

      val greenDf = spark.table("nyc_taxi_ods.taxi_trip_green_ods")
        .filter(col("year") === targetYear)
        .filter(col("month").isin(targetMonths: _*))
        .withColumn("taxi_type", lit("green"))
        .withColumn("pickup_datetime", col("lpep_pickup_datetime"))
        .withColumn("dropoff_datetime", col("lpep_dropoff_datetime"))

      val unified = yellowDf
        .select(
          col("VendorID"), col("pickup_datetime"), col("dropoff_datetime"),
          col("passenger_count"), col("trip_distance"), col("RatecodeID"),
          col("store_and_fwd_flag"), col("PULocationID"), col("DOLocationID"),
          col("payment_type"), col("fare_amount"), col("extra"), col("mta_tax"),
          col("tip_amount"), col("tolls_amount"), col("improvement_surcharge"),
          col("total_amount"), col("congestion_surcharge"), col("Airport_fee"),
          col("cbd_congestion_fee"), col("taxi_type")
        )
        .unionByName(
          greenDf.select(
            col("VendorID"), col("pickup_datetime"), col("dropoff_datetime"),
            col("passenger_count"), col("trip_distance"), col("RatecodeID"),
            col("store_and_fwd_flag"), col("PULocationID"), col("DOLocationID"),
            col("payment_type"), col("fare_amount"), col("extra"), col("mta_tax"),
            col("tip_amount"), col("tolls_amount"), col("improvement_surcharge"),
            col("total_amount"), col("congestion_surcharge"),
            lit(0.0).as("Airport_fee"),
            col("cbd_congestion_fee"), col("taxi_type")
          )
        )

      val (cachedUnified, originalCount) = DataFrameMetrics.withCountAndCache(unified, "ODS原始数据")
      metrics.recordJobMetric("DWD", ctx.executionId, "original_count", originalCount)
      logger.info(s"阶段1-原始数据: ${MonitorUtils.formatNumber(originalCount)} 条")

      // ==================== 阶段2: 分层空值处理 ====================
      val withP0Filtered = cachedUnified
        .filter(col("VendorID").isNotNull)
        .filter(col("pickup_datetime").isNotNull)
        .filter(col("dropoff_datetime").isNotNull)
        .filter(col("PULocationID").isNotNull)
        .filter(col("DOLocationID").isNotNull)
        .filter(col("total_amount").isNotNull)

      val withP1Filtered = withP0Filtered
        .filter(col("trip_distance").isNotNull)
        .filter(col("passenger_count").isNotNull)
        .filter(col("payment_type").isNotNull)
        .filter(col("RatecodeID").isNotNull)
        .filter(col("fare_amount").isNotNull)

      val withP2Filled = withP1Filtered
        .withColumn("store_and_fwd_flag",
          when(col("store_and_fwd_flag").isNotNull, col("store_and_fwd_flag")).otherwise(lit("N")))
        .withColumn("congestion_surcharge",
          when(col("congestion_surcharge").isNotNull, col("congestion_surcharge")).otherwise(lit(0.0)))
        .withColumn("Airport_fee",
          when(col("Airport_fee").isNotNull, col("Airport_fee")).otherwise(lit(0.0)))
        .withColumn("improvement_surcharge",
          when(col("improvement_surcharge").isNotNull, col("improvement_surcharge")).otherwise(lit(0.0)))

      val withP3Filled = withP2Filled
        .withColumn("extra", when(col("extra").isNotNull, col("extra")).otherwise(lit(0.0)))
        .withColumn("mta_tax", when(col("mta_tax").isNotNull, col("mta_tax")).otherwise(lit(0.0)))
        .withColumn("tip_amount", when(col("tip_amount").isNotNull, col("tip_amount")).otherwise(lit(0.0)))
        .withColumn("tolls_amount", when(col("tolls_amount").isNotNull, col("tolls_amount")).otherwise(lit(0.0)))
        .withColumn("cbd_congestion_fee",
          when(col("cbd_congestion_fee").isNotNull, col("cbd_congestion_fee")).otherwise(lit(0.0)))

      // ==================== 阶段3: 值域过滤 ====================
      val maxDistance = ConfigManager.getDoubleOrDefault("quality.max-trip-distance", 120.0)
      val maxAmount = ConfigManager.getDoubleOrDefault("quality.max-total-amount", 800.0)
      val maxPassenger = ConfigManager.getIntOrDefault("quality.max-passenger-count", 6)
      val minPassenger = ConfigManager.getIntOrDefault("quality.min-passenger-count", 1)
      val maxDuration = ConfigManager.getDoubleOrDefault("quality.max-duration-minutes", 240.0)
      val minDuration = ConfigManager.getDoubleOrDefault("quality.min-duration-minutes", 1.0)

      val withRangeFiltered = withP3Filled
        .filter(col("trip_distance") > 0 && col("trip_distance") <= maxDistance)
        .filter(col("total_amount") > 0 && col("total_amount") <= maxAmount)
        .filter(col("passenger_count") >= minPassenger && col("passenger_count") <= maxPassenger)
        .filter(col("dropoff_datetime") > col("pickup_datetime"))
        .withColumn("trip_duration_minutes",
          (unix_timestamp(col("dropoff_datetime")) - unix_timestamp(col("pickup_datetime"))) / 60)
        .filter(col("trip_duration_minutes") > minDuration && col("trip_duration_minutes") <= maxDuration)

      val cachedFiltered = DataFrameMetrics.cacheOnly(withRangeFiltered, "值域过滤后")

      // ==================== 阶段4: 维度关联 ====================
      val paymentDim = spark.table("nyc_taxi_dim.dim_payment")
      val vendorDim = spark.table("nyc_taxi_dim.dim_vendor")
      val locationDim = spark.table("nyc_taxi_dim.dim_location")
      val ratecodeDim = spark.table("nyc_taxi_dim.dim_ratecode")
      val storageFlagDim = spark.table("nyc_taxi_dim.dim_storage_flag")

      val paymentDimSelected = paymentDim.select(
        col("payment_type"),
        col("payment_name_zh"),
        col("is_cashless")
      )

      val vendorDimSelected = vendorDim.select(
        col("vendor_id").as("VendorID"),
        col("vendor_name_zh")
      )

      val ratecodeDimSelected = ratecodeDim.select(
        col("ratecode_id").as("RatecodeID"),
        col("ratecode_name_zh"),
        col("is_airport").as("ratecode_is_airport")
      )

      val storageFlagDimSelected = storageFlagDim.select(
        col("store_and_fwd_flag"),
        col("flag_name_zh")
      )

      // 上车区域维度（只选择需要的字段，不创建名称字段）
      val locationDimPu = locationDim.select(
        col("location_id"),
        col("is_airport").as("pu_is_airport"),
        col("is_manhattan_core").as("pu_is_manhattan_core")
      )

      // 下车区域维度（只选择location_id用于关联）
      val locationDimDo = locationDim.select(
        col("location_id")
      )

      // 1. 关联上车区域（只关联标志字段）
      val withPuLocation = cachedFiltered
        .join(broadcast(locationDimPu).as("pu"), col("PULocationID") === col("pu.location_id"), "left")
        .withColumn("is_airport", coalesce(col("pu_is_airport"), lit("False")))
        .withColumn("is_manhattan_core", coalesce(col("pu_is_manhattan_core"), lit("False")))
        .drop("pu")
        .drop("pu_is_airport")
        .drop("pu_is_manhattan_core")
        .drop("location_id")

      // 2. 关联下车区域（仅关联，不产生新字段）
      val withDoLocation = withPuLocation
        .join(broadcast(locationDimDo).as("do"), col("DOLocationID") === col("do.location_id"), "left")
        .drop("do")
        .drop("location_id")

      // 3. 关联支付维度
      val withPayment = withDoLocation
        .join(broadcast(paymentDimSelected), Seq("payment_type"), "left")
        .withColumn("payment_name", coalesce(col("payment_name_zh"), lit("未知支付")))
        .withColumn("is_cashless", coalesce(col("is_cashless"), lit("False")))
        .drop("payment_name_zh")

      // 4. 关联供应商维度
      val withVendor = withPayment
        .join(broadcast(vendorDimSelected), Seq("VendorID"), "left")
        .withColumn("vendor_name", coalesce(col("vendor_name_zh"), lit("未知供应商")))
        .drop("vendor_name_zh")

      // 5. 关联费率维度
      val withRatecode = withVendor
        .join(broadcast(ratecodeDimSelected), Seq("RatecodeID"), "left")
        .withColumn("ratecode_name", coalesce(col("ratecode_name_zh"), lit("未知费率")))
        .drop("ratecode_name_zh")
        .drop("ratecode_is_airport")

      // 6. 关联存储标志维度
      val withStorageFlag = withRatecode
        .join(broadcast(storageFlagDimSelected), Seq("store_and_fwd_flag"), "left")
        .withColumn("flag_name", coalesce(col("flag_name_zh"), lit("未知")))
        .drop("flag_name_zh")

      // ==================== 阶段5: 逻辑修复 ====================
      val withLogicFixed = withStorageFlag
        .withColumn("tip_amount",
          when(col("payment_type") === 2, lit(0.0)).otherwise(col("tip_amount")))
        .withColumn("Airport_fee",
          when(col("is_airport") === "False" && col("Airport_fee") > 0, lit(0.0))
            .otherwise(col("Airport_fee")))
        .withColumn("congestion_surcharge",
          when(col("is_manhattan_core") === "False" && col("congestion_surcharge") > 0, lit(0.0))
            .otherwise(col("congestion_surcharge")))

      // ==================== 阶段6: 派生字段 ====================
      // 【修正2】添加 row_number 确保唯一性
      val withRowNumber = withLogicFixed
        .withColumn("_row_num", monotonically_increasing_id())

      val enriched = withRowNumber
        .withColumn("trip_id",
          sha2(concat_ws("_",
            col("VendorID").cast("string"),
            col("pickup_datetime").cast("string"),
            col("dropoff_datetime").cast("string"),
            col("PULocationID").cast("string"),
            col("DOLocationID").cast("string"),
            col("_row_num").cast("string")
          ), 256)
        )
        .withColumn("pickup_date", to_date(col("pickup_datetime")))
        .withColumn("pickup_hour", hour(col("pickup_datetime")))
        .withColumn("pickup_dayofweek", dayofweek(col("pickup_datetime")))
        .withColumn("is_weekend",
          when(col("pickup_dayofweek").isin(1, 7), lit(1)).otherwise(lit(0)))
        .withColumn("year", year(col("pickup_datetime")))
        .withColumn("month", month(col("pickup_datetime")))
        .withColumn("trip_type", deriveTripType(col("RatecodeID"), col("is_airport"), col("is_manhattan_core")))
        .drop("_row_num")

      // 兜底去重：使用 trip_id 去重
      val deduplicated = enriched.dropDuplicates("trip_id")

      val cachedEnriched = DataFrameMetrics.cacheOnly(deduplicated, "最终数据")

      val finalCount = cachedEnriched.count()
      logger.info(s"DWD 最终记录数: ${MonitorUtils.formatNumber(finalCount)}")
      val retentionRate = if (originalCount > 0) finalCount.toDouble / originalCount * 100 else 0.0
      logger.info(s"📊 清洗后保留率: ${retentionRate}%.2f%%")

      // ==================== 阶段7: 质量检测 ====================
      val qualityReport = QualityManager.fullCheck(
        spark, cachedEnriched, "fact_taxi_trips",
        originalCount, ctx.executionId
      )

      if (!qualityReport.pass) {
        logger.warn(s"质量检测未完全通过: ${qualityReport.getFailureReason}")
        if (ConfigManager.isQualityFailOnFailure) {
          throw new QualityCheckException(s"DWD 质量检测失败: ${qualityReport.getFailureReason}")
        }
      }

      // ==================== 阶段8: 原子写入 ====================
      IcebergTableManager.atomicOverwrite(
        spark, cachedEnriched, "nyc_taxi_dwd", "fact_taxi_trips",
        Seq("year", "month", "taxi_type")
      )

      metrics.recordJobMetric("DWD", ctx.executionId, "final_count", finalCount)
      metrics.recordJobMetric("DWD", ctx.executionId, "retention_rate", retentionRate)

      smartCache.uncacheAll()
      CacheManager.uncacheAll(
        (cachedUnified, "ODS原始数据"),
        (cachedFiltered, "值域过滤后"),
        (cachedEnriched, "最终数据")
      )

      val duration = (System.currentTimeMillis() - startTime) / 1000
      logger.info(metrics.generateReport())
      SparkListenerMetrics.getAndPrintReport()

      BuildResult.success("fact_taxi_trips", finalCount, duration, qualityReport.format())

    } catch {
      case e: Exception =>
        logger.error(s"DWD 构建失败: ${e.getMessage}", e)
        BuildResult.failure("fact_taxi_trips", e.getMessage, (System.currentTimeMillis() - startTime) / 1000)
    } finally {
      smartCache.uncacheAll()
      CacheManager.uncacheTracked()
      ctx.clearMDC()
      spark.stop()
    }
  }

  private def deriveTripType = udf((ratecodeId: Int, isAirport: String, isManhattanCore: String) => {
    (ratecodeId, isAirport, isManhattanCore) match {
      case (2, _, _) => "机场行程"
      case (3, _, _) => "机场行程"
      case (6, _, _) => "拼车行程"
      case (5, _, _) => "议价行程"
      case (_, "True", _) => "机场行程"
      case (_, _, "True") => "曼哈顿核心区行程"
      case (4, _, _) => "跨区行程"
      case (99, _, _) => "未知行程"
      case _ => "标准行程"
    }
  })
}