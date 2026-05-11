package com.taxi.etl.common

import org.apache.spark.sql.DataFrame
import org.apache.spark.storage.StorageLevel
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters._

/**
 * 智能缓存管理器（实例化版本）
 *
 * 每个 Job 独立实例，避免全局可变状态引发的并发问题
 *
 * 使用方式：
 *   val cache = SmartCacheLite()
 *   cache.markAccess("my_data")
 *   val cached = cache.cacheIfNeeded(df, "my_data")
 *   cache.uncacheAll()
 */
class SmartCacheLite private(
                              enabled: Boolean,
                              minReuseCount: Int
                            ) {

  private val logger = LoggerFactory.getLogger(getClass)

  // 线程安全的计数器
  private val accessCount = new ConcurrentHashMap[String, AtomicInteger]()
  // 线程安全的缓存引用
  private val cachedRefs = new ConcurrentHashMap[String, DataFrame]()

  def markAccess(name: String): Unit = {
    if (!enabled) return
    val counter = accessCount.computeIfAbsent(name, _ => new AtomicInteger(0))
    val count = counter.incrementAndGet()
    if (logger.isDebugEnabled) {
      logger.debug(s"标记访问: $name, 次数=$count")
    }
  }

  def getAccessCount(name: String): Int = {
    val counter = accessCount.get(name)
    if (counter == null) 0 else counter.get()
  }

  def cacheIfNeeded(df: DataFrame, name: String): DataFrame = {
    if (!enabled) return df
    val count = getAccessCount(name)

    if (count >= minReuseCount) {
      if (!cachedRefs.containsKey(name)) {
        logger.info(s"行为驱动缓存: $name (访问次数=$count, 阈值=$minReuseCount)")
        val storageLevel = if (estimateSizeMB(df) > 500) {
          StorageLevel.MEMORY_AND_DISK_SER
        } else {
          StorageLevel.MEMORY_AND_DISK
        }
        val cached = df.persist(storageLevel)
        cachedRefs.put(name, cached)
        MetricsCollector.record("cache", s"${name}_cached", 1.0)
        cached
      } else {
        cachedRefs.get(name)
      }
    } else {
      df
    }
  }

  def forceCache(df: DataFrame, name: String): DataFrame = {
    if (!enabled) return df
    if (!cachedRefs.containsKey(name)) {
      logger.info(s"强制缓存: $name")
      val cached = df.persist()
      cachedRefs.put(name, cached)
      cached
    } else {
      cachedRefs.get(name)
    }
  }

  def uncache(name: String): Unit = {
    val df = cachedRefs.remove(name)
    if (df != null) {
      logger.info(s"释放缓存: $name")
      df.unpersist()
    }
  }

  def uncacheAll(): Unit = {
    val keys = cachedRefs.keys().asScala.toArray
    keys.foreach { key =>
      uncache(key)
    }
  }

  def reset(): Unit = {
    accessCount.clear()
    uncacheAll()
  }

  def getCachedNames: Set[String] = {
    cachedRefs.keys().asScala.toSet
  }

  def isCached(name: String): Boolean = {
    cachedRefs.containsKey(name)
  }

  private def estimateSizeMB(df: DataFrame): Double = {
    try {
      val size = df.queryExecution.optimizedPlan.stats.sizeInBytes
      // 修复：统计信息不可靠时返回 Double.MaxValue，避免错误选择存储级别
      if (size <= 0 || size == Long.MaxValue) {
        Double.MaxValue
      } else {
        size.toDouble / (1024.0 * 1024.0)
      }
    } catch {
      case _: Exception => Double.MaxValue
    }
  }
}

/**
 * SmartCacheLite 伴生对象 - 提供工厂方法
 */
object SmartCacheLite {

  private val ENABLED_DEFAULT = ConfigManager.isCacheEnabled
  private val MIN_REUSE_COUNT_DEFAULT = ConfigManager.getCacheMinReuseCount

  /**
   * 创建新的缓存实例
   */
  def apply(): SmartCacheLite = {
    new SmartCacheLite(ENABLED_DEFAULT, MIN_REUSE_COUNT_DEFAULT)
  }

  /**
   * 创建自定义配置的缓存实例
   */
  def apply(enabled: Boolean, minReuseCount: Int): SmartCacheLite = {
    new SmartCacheLite(enabled, minReuseCount)
  }
}