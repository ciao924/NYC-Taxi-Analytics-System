package com.taxi.etl.common

import org.apache.spark.sql.DataFrame
import org.apache.spark.storage.StorageLevel
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._

/**
 * 缓存管理器 - 提供简单的缓存管理功能
 *
 * 注意：CacheManager 是全局单例，适用于简单的缓存场景。
 * 对于复杂的多 Job 场景，建议使用 SmartCacheLite 的实例化版本。
 */
object CacheManager {

  private val logger = LoggerFactory.getLogger(getClass)
  private val ENABLED: Boolean = ConfigManager.isCacheEnabled

  // 追踪已缓存的 DataFrame，便于释放（解决内存泄漏风险）
  private val cachedRefs = new ConcurrentHashMap[String, DataFrame]()

  def cache(df: DataFrame, name: String = ""): DataFrame = {
    if (ENABLED) {
      logger.info(s"强制缓存: $name")
      val cached = df.persist(StorageLevel.MEMORY_AND_DISK_SER)
      if (name.nonEmpty) {
        // 记录缓存引用，便于后续释放
        val existing = cachedRefs.put(name, cached)
        if (existing != null) {
          logger.warn(s"缓存名称 $name 已存在，将被覆盖")
        }
      }
      cached
    } else {
      df
    }
  }

  def uncache(df: DataFrame, name: String = ""): Unit = {
    if (ENABLED && df != null) {
      logger.debug(s"释放缓存: $name")
      df.unpersist()
      if (name.nonEmpty) {
        cachedRefs.remove(name)
      }
    }
  }

  def uncacheAll(dfs: (DataFrame, String)*): Unit = {
    dfs.foreach { case (df, name) => uncache(df, name) }
  }

  /**
   * 释放所有被追踪的缓存
   */
  def uncacheTracked(): Unit = {
    val keys = cachedRefs.keys().asScala.toArray
    keys.foreach { key =>
      val df = cachedRefs.remove(key)
      if (df != null) {
        logger.info(s"释放被追踪的缓存: $key")
        df.unpersist()
      }
    }
  }

  /**
   * 获取被追踪的缓存名称列表
   */
  def getTrackedCacheNames: Set[String] = {
    cachedRefs.keys().asScala.toSet
  }
}