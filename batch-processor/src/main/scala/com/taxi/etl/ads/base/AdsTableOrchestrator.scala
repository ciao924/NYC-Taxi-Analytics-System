package com.taxi.etl.ads.base

import com.taxi.etl.common.{MetricsCollector}
import com.taxi.etl.ads.daily._
import com.taxi.etl.ads.fee._
import com.taxi.etl.ads.traffic._
import com.taxi.etl.ads.distribution._
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory

case class TableBuildResult(
                             tableName: String,
                             success: Boolean,
                             rowCount: Long = 0,
                             durationSeconds: Long = 0,
                             errorMessage: Option[String] = None
                           )

case class BuilderResult(
                          success: Boolean,
                          rowCount: Long = 0,
                          errorMessage: Option[String] = None
                        )

object AdsTableOrchestrator {

  private val logger = LoggerFactory.getLogger(getClass)

  def buildAll(
                spark: SparkSession,
                data: DwsDataBundle,
                startDate: String,
                endDate: String,
                ctx: JobContext,
                metrics: MetricsCollector
              ): Seq[TableBuildResult] = {

    logger.info("\n" + "=" * 80)
    logger.info("📊 开始构建 ADS 各层表")
    logger.info("=" * 80)

    val results = scala.collection.mutable.ListBuffer[TableBuildResult]()

    // 日常指标模块
    results ++= buildDailyModule(spark, data, startDate, endDate, ctx, metrics)

    // 费用分析模块
    results ++= buildFeeModule(spark, data, startDate, endDate, ctx, metrics)

    // 交通流量模块
    results ++= buildTrafficModule(spark, data, startDate, endDate, ctx, metrics)

    // 分布分析模块
    results ++= buildDistributionModule(spark, data, startDate, endDate, ctx, metrics)

    printSummary(results)
    results.toSeq
  }

  private def buildDailyModule(
                                spark: SparkSession,
                                data: DwsDataBundle,
                                startDate: String,
                                endDate: String,
                                ctx: JobContext,
                                metrics: MetricsCollector
                              ): Seq[TableBuildResult] = {
    val results = scala.collection.mutable.ListBuffer[TableBuildResult]()

    results += buildWithTimer("KpiDaily", () => {
      val result = KpiDailyBuilder.build(spark, data.dailyDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.KPI_DAILY, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("HourlyDistribution", () => {
      val result = HourlyDistributionBuilder.build(spark, data.hourlyDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.HOURLY_DISTRIBUTION, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("WeekdayAnalysis", () => {
      val result = WeekdayAnalysisBuilder.build(spark, data.dailyDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.WEEKDAY_ANALYSIS, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("PaymentAnalysis", () => {
      val result = PaymentAnalysisBuilder.build(spark, data.feeDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.PAYMENT_ANALYSIS, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("AirportAnalysis", () => {
      val result = AirportAnalysisBuilder.build(spark, data.dailyDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.AIRPORT, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("VendorAnalysis", () => {
      val result = VendorAnalysisBuilder.build(spark, data.vendorDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.VENDOR, result.success, result.rowCount, 0, result.errorMessage)
    })

    results.toSeq
  }

  private def buildFeeModule(
                              spark: SparkSession,
                              data: DwsDataBundle,
                              startDate: String,
                              endDate: String,
                              ctx: JobContext,
                              metrics: MetricsCollector
                            ): Seq[TableBuildResult] = {
    val results = scala.collection.mutable.ListBuffer[TableBuildResult]()

    results += buildWithTimer("FeeComposition", () => {
      val result = FeeCompositionBuilder.build(spark, data.feeDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.FEE_COMPOSITION, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("FeePercentage", () => {
      val result = FeePercentageBuilder.build(spark, data.feeDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.FEE_PERCENTAGE, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("FeeByBorough", () => {
      val result = FeeByBoroughBuilder.build(spark, data.feeDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.FEE_BY_BOROUGH, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("FeeTrend", () => {
      val result = FeeTrendBuilder.build(spark, data.feeDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.FEE_TREND, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("FeeByTaxiType", () => {
      val result = FeeByTaxiTypeBuilder.build(spark, data.feeDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.FEE_BY_TAXI_TYPE, result.success, result.rowCount, 0, result.errorMessage)
    })

    results.toSeq
  }

  private def buildTrafficModule(
                                  spark: SparkSession,
                                  data: DwsDataBundle,
                                  startDate: String,
                                  endDate: String,
                                  ctx: JobContext,
                                  metrics: MetricsCollector
                                ): Seq[TableBuildResult] = {
    val results = scala.collection.mutable.ListBuffer[TableBuildResult]()

    results += buildWithTimer("PickupHotspots", () => {
      val result = PickupHotspotsBuilder.build(spark, data.zoneDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.PICKUP_HOTSPOTS, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("DropoffHotspots", () => {
      val result = DropoffHotspotsBuilder.build(spark, data.zoneDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.DROPOFF_HOTSPOTS, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("BoroughFlow", () => {
      val result = BoroughFlowBuilder.build(spark, data.zoneDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.BOROUGH_FLOW, result.success, result.rowCount, 0, result.errorMessage)
    })

    results.toSeq
  }

  private def buildDistributionModule(
                                       spark: SparkSession,
                                       data: DwsDataBundle,
                                       startDate: String,
                                       endDate: String,
                                       ctx: JobContext,
                                       metrics: MetricsCollector
                                     ): Seq[TableBuildResult] = {
    val results = scala.collection.mutable.ListBuffer[TableBuildResult]()

    results += buildWithTimer("DistanceDistribution", () => {
      val result = DistanceDistributionBuilder.build(spark, data.dailyDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.DISTANCE_DISTRIBUTION, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("DurationDistribution", () => {
      val result = DurationDistributionBuilder.build(spark, data.dailyDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.DURATION_DISTRIBUTION, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("PassengerDistribution", () => {
      val result = PassengerDistributionBuilder.build(spark, data.dailyDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.PASSENGER_DISTRIBUTION, result.success, result.rowCount, 0, result.errorMessage)
    })

    results += buildWithTimer("RevenueContribution", () => {
      val result = RevenueContributionBuilder.build(spark, data.zoneDf, startDate, endDate, ctx, metrics)
      TableBuildResult(AdsConstants.TableNames.REVENUE_CONTRIBUTION, result.success, result.rowCount, 0, result.errorMessage)
    })

    results.toSeq
  }

  private def buildWithTimer(
                              name: String,
                              builder: () => TableBuildResult
                            ): TableBuildResult = {
    val startTime = System.currentTimeMillis()
    try {
      logger.info(s"\n📊 构建表: $name")
      val result = builder()
      val duration = (System.currentTimeMillis() - startTime) / 1000
      val finalResult = result.copy(durationSeconds = duration)

      if (finalResult.success) {
        logger.info(s"  ✅ $name 构建成功，写入 ${formatNumber(finalResult.rowCount)} 条，耗时 ${duration}s")
      } else {
        logger.error(s"  ❌ $name 构建失败: ${finalResult.errorMessage.getOrElse("未知错误")}")
      }

      finalResult
    } catch {
      case e: Exception =>
        logger.error(s"  ❌ $name 构建异常: ${e.getMessage}", e)
        TableBuildResult(name, false, 0, 0, Some(e.getMessage))
    }
  }

  private def printSummary(results: Seq[TableBuildResult]): Unit = {
    val passedCount = results.count(_.success)
    val failedCount = results.count(!_.success)
    val totalRows = results.filter(_.success).map(_.rowCount).sum

    logger.info("\n" + "=" * 80)
    logger.info("📊 ADS 层构建完成统计")
    logger.info("=" * 80)
    logger.info(s"  总表数: ${results.size}")
    logger.info(s"  ✅ 成功: $passedCount")
    logger.info(s"  ❌ 失败: $failedCount")
    logger.info(s"  📈 总写入行数: ${formatNumber(totalRows)}")

    if (failedCount > 0) {
      logger.warn(s"  失败表: ${results.filter(!_.success).map(_.tableName).mkString(", ")}")
    }
    logger.info("=" * 80)
  }

  private def formatNumber(num: Long): String = {
    if (num >= 1000000) f"${num / 1000000.0}%.1fM"
    else if (num >= 1000) f"${num / 1000.0}%.1fK"
    else num.toString
  }
}