package com.taxi.etl.common

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.Try

object ConfigManager {

  private val logger = LoggerFactory.getLogger(getClass)

  // 直接使用环境变量，不修改 System.setProperty
  private val env = sys.env.getOrElse("ENVIRONMENT", sys.props.getOrElse("environment", "dev"))
  logger.info(s"加载配置环境: $env")

  private val config: Config = {
    try {
      ConfigFactory.load(s"application-$env.conf")
    } catch {
      case e: ConfigException.Missing =>
        logger.warn(s"未找到 application-$env.conf，使用默认配置")
        ConfigFactory.load()
      case e: Exception =>
        logger.error(s"加载配置文件失败: ${e.getMessage}", e)
        throw e
    }
  }

  // 合并默认配置
  private val finalConfig = config.withFallback(ConfigFactory.load())

  def getString(path: String): String = {
    try {
      finalConfig.getString(path)
    } catch {
      case e: ConfigException.Missing =>
        logger.error(s"配置项缺失: $path")
        throw e
    }
  }

  def getInt(path: String): Int = finalConfig.getInt(path)
  def getLong(path: String): Long = finalConfig.getLong(path)
  def getBoolean(path: String): Boolean = finalConfig.getBoolean(path)
  def getDouble(path: String): Double = finalConfig.getDouble(path)

  def getIntList(path: String): List[Int] = {
    Try(finalConfig.getIntList(path).asScala.toList.map(_.toInt)).getOrElse(List.empty)
  }

  def hasPath(path: String): Boolean = finalConfig.hasPath(path)

  def getIntOrDefault(path: String, default: Int): Int = {
    if (hasPath(path)) getInt(path) else default
  }

  def getLongOrDefault(path: String, default: Long): Long = {
    if (hasPath(path)) getLong(path) else default
  }

  def getDoubleOrDefault(path: String, default: Double): Double = {
    if (hasPath(path)) getDouble(path) else default
  }

  def getBooleanOrDefault(path: String, default: Boolean): Boolean = {
    if (hasPath(path)) getBoolean(path) else default
  }

  def getStringOrDefault(path: String, default: String): String = {
    if (hasPath(path)) getString(path) else default
  }

  // ==================== 配置项 Getter ====================

  def getQualityDeadLetterMaxRecords: Int = {
    val configured = getIntOrDefault("quality.dead-letter-max-records", 1000)
    math.min(configured, 10000)
  }

  def getQualityRangeCheckSampleRate: Double = {
    getDoubleOrDefault("quality.range-check-sample-rate", 0.1)
  }

  def getQualityAnomalyThreshold: Double = {
    getDoubleOrDefault("quality.anomaly-threshold", 0.05)  // 改为 5%
  }

  def getQualityRangeThreshold: Double = {
    getDoubleOrDefault("quality.range-threshold", 2.0)
  }

  def getQualityExpectedRetentionMin: Double = {
    getDoubleOrDefault("quality.expected-retention-rate-min", 75.0) / 100  // 改为 75%
  }

  def getQualityExpectedRetentionMax: Double = {
    getDoubleOrDefault("quality.expected-retention-rate-max", 90.0) / 100  // 改为 90%
  }

  def isQualityFailOnFailure: Boolean = {
    getBooleanOrDefault("quality.fail-on-failure", false)
  }

  def isQualityDeadLetterEnabled: Boolean = {
    getBooleanOrDefault("quality.dead-letter-enabled", true)
  }

  def isCacheEnabled: Boolean = {
    getBooleanOrDefault("cache.enabled", true)
  }

  def getCacheMinReuseCount: Int = {
    getIntOrDefault("cache.min-reuse-count", 2)
  }

  def getBroadcastThresholdMB: Long = {
    getLongOrDefault("join.broadcast-threshold-mb", 10)
  }

  def getDefaultWritePartitions: Int = {
    getIntOrDefault("data.write.default-partitions", 40)
  }

  def validateRequiredConfigs(): Unit = {
    val requiredConfigs = Seq("spark.warehouse.dir", "hive.metastore.uris")
    requiredConfigs.foreach { key =>
      if (!hasPath(key)) {
        logger.warn(s"缺少必要配置: $key，将使用默认值")
      } else {
        logger.info(s"✅ 配置验证通过: $key = ${getString(key)}")
      }
    }
    try {
      val targetYear = getInt("common.target-year")
      val targetMonths = getIntList("common.target-months")
      logger.info(s"目标数据范围: $targetYear 年 Q1季度")
      if (targetMonths.isEmpty) {
        logger.warn("common.target-months 为空，使用默认月份 [1,2,3]")
      }
    } catch {
      case e: ConfigException.Missing =>
        logger.warn(s"目标数据范围配置缺失: ${e.getMessage}")
    }
  }
}