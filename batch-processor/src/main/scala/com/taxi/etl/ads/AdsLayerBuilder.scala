package com.taxi.etl.ads

import com.taxi.etl.common.{ConfigManager, MetricsCollector, SmartCacheLite, SparkSessionFactory, Version}
import com.taxi.etl.models.JobContext
import com.taxi.etl.ads.base.{AdsConstants, BaseAdsWriter, ConsistencyValidator, DwsDataLoader, AdsTableOrchestrator}
import org.slf4j.LoggerFactory

object AdsLayerBuilder {

  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    Version.printVersion()

    // 1. 解析参数
    val (startDate, endDate) = parseArgs(args)

    // 2. 创建执行上下文
    val ctx = JobContext.fromArgs("ADS", args)
    ctx.setMDC()

    // 3. 创建指标收集器和缓存管理器
    val metrics = new MetricsCollector()
    val cache = SmartCacheLite()

    // 4. 创建 SparkSession（使用统一的工厂）
    val spark = SparkSessionFactory.create(s"ADS_Layer_${startDate.replace("-", "")}_${endDate.replace("-", "")}")

    val jobStartTime = System.currentTimeMillis()

    try {
      logger.info("=" * 80)
      logger.info(s"🏗️ ADS 层数据构建 - ${Version.VERSION}")
      logger.info("=" * 80)
      logger.info(s"执行ID: ${ctx.executionId}")
      logger.info(s"数据范围: $startDate 至 $endDate")
      logger.info(s"日期天数: ${getDaysBetween(startDate, endDate)} 天")
      logger.info(s"目标库: MySQL - ${BaseAdsWriter.getMysqlDatabase}")
      logger.info("=" * 80)

      // 5. 创建数据库
      BaseAdsWriter.createDatabaseIfNotExists(spark)
      BaseAdsWriter.resetQualityResults()

      // 6. 加载 DWS 数据（统一加载，一次扫描）
      val dwsData = DwsDataLoader.load(spark, startDate, endDate, cache, metrics)

      // 7. 一致性校验
      ConsistencyValidator.validate(dwsData, metrics)

      // 8. 构建所有 ADS 表
      val results = AdsTableOrchestrator.buildAll(spark, dwsData, startDate, endDate, ctx, metrics)

      // 9. 打印质量报告
      BaseAdsWriter.printQualityReport()

      // 10. 打印指标报告
      logger.info(metrics.generateReport())

      // 11. 打印最终统计
      val jobDuration = (System.currentTimeMillis() - jobStartTime) / 1000
      val successCount = results.count(_.success)
      val failedCount = results.count(!_.success)

      logger.info("\n" + "=" * 80)
      logger.info(s"✅ ADS 层构建完成！")
      logger.info(s"   总表数: ${results.size}")
      logger.info(s"   成功: $successCount")
      logger.info(s"   失败: $failedCount")
      logger.info(s"   总耗时: ${jobDuration} 秒")
      logger.info("=" * 80)

      if (failedCount > 0) {
        logger.warn(s"失败表: ${results.filter(!_.success).map(_.tableName).mkString(", ")}")
        System.exit(1)
      }

    } catch {
      case e: Exception =>
        logger.error(s"❌ ADS 构建失败: ${e.getMessage}", e)
        System.exit(1)
    } finally {
      cache.uncacheAll()
      ctx.clearMDC()
      spark.stop()
    }
  }

  private def parseArgs(args: Array[String]): (String, String) = {
    val defaultStart = ConfigManager.getStringOrDefault("common.default-start-date", "2025-01-01")
    val defaultEnd = ConfigManager.getStringOrDefault("common.default-end-date", "2025-03-31")

    val startDate = if (args.length > 0) args(0) else defaultStart
    val endDate = if (args.length > 1) args(1) else defaultEnd

    logger.info(s"使用日期范围: $startDate ~ $endDate")
    (startDate, endDate)
  }

  private def getDaysBetween(startDate: String, endDate: String): Int = {
    try {
      val start = java.time.LocalDate.parse(startDate)
      val end = java.time.LocalDate.parse(endDate)
      (end.toEpochDay - start.toEpochDay + 1).toInt
    } catch {
      case _: Exception => 1
    }
  }
}