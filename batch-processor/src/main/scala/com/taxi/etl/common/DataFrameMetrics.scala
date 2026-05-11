package com.taxi.etl.common

import com.taxi.etl.utils.MonitorUtils
import org.apache.spark.sql.DataFrame
import org.apache.spark.storage.StorageLevel
import org.slf4j.LoggerFactory

object DataFrameMetrics {

  private val logger = LoggerFactory.getLogger(getClass)

  /**
   * 缓存并计数
   *
   * 注意：此方法会触发一次 Action（count），适用于需要立即知道数据量的场景
   *
   * @param df 要缓存的 DataFrame
   * @param name 缓存名称（用于日志）
   * @param storageLevel 存储级别
   * @return (缓存的DataFrame, 记录数)
   */
  def withCountAndCache(
                         df: DataFrame,
                         name: String = "",
                         storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER
                       ): (DataFrame, Long) = {
    logger.info(s"开始统计并缓存: $name")
    val cached = df.persist(storageLevel)
    val count = cached.count() // 触发 Action，同时触发缓存
    logger.info(s"✅ $name 记录数: ${MonitorUtils.formatNumber(count)}")
    MetricsCollector.record("dataframe", s"${name}_count", count)
    (cached, count)
  }

  /**
   * 仅缓存，不立即计数
   *
   * 注意：persist 只是标记，需要后续 Action 才会真正缓存。
   * 适用于后续有 Action（如 count、write）的场景，避免重复扫描。
   *
   * 使用场景示例：
   *   val cached = DataFrameMetrics.cacheOnly(df, "my_data")
   *   val result = cached.transform(...)  // 后续操作会触发缓存
   *   val count = cached.count()          // 此时已缓存，count 很快
   *
   * @param df 要缓存的 DataFrame
   * @param name 缓存名称（用于日志）
   * @param storageLevel 存储级别
   * @return 缓存的DataFrame
   */
  def cacheOnly(
                 df: DataFrame,
                 name: String = "",
                 storageLevel: StorageLevel = StorageLevel.MEMORY_AND_DISK_SER
               ): DataFrame = {
    logger.info(s"缓存（不计数）: $name")
    df.persist(storageLevel)
  }

  /**
   * 聚合指标计算（不缓存）
   * 只扫描一次，计算多个指标
   */
  def aggregateMetrics(df: DataFrame): AggregatedMetrics = {
    val stats = df.agg(
      org.apache.spark.sql.functions.count("*").as("total"),
      org.apache.spark.sql.functions.sum(
        org.apache.spark.sql.functions.when(org.apache.spark.sql.functions.col("trip_distance") <= 0, 1).otherwise(0)
      ).as("bad_distance"),
      org.apache.spark.sql.functions.sum(
        org.apache.spark.sql.functions.when(org.apache.spark.sql.functions.col("total_amount") <= 0, 1).otherwise(0)
      ).as("bad_amount"),
      org.apache.spark.sql.functions.approx_count_distinct(org.apache.spark.sql.functions.col("trip_id")).as("distinct_trip_id")
    ).collect()(0)

    AggregatedMetrics(
      totalCount = stats.getLong(0),
      badDistanceCount = if (stats.isNullAt(1)) 0 else stats.getLong(1),
      badAmountCount = if (stats.isNullAt(2)) 0 else stats.getLong(2),
      distinctTripId = if (stats.isNullAt(3)) 0 else stats.getLong(3)
    )
  }

  /**
   * 快速统计（仅计数，不缓存）
   */
  def quickCount(df: DataFrame, name: String = ""): Long = {
    val count = df.count()
    logger.info(s"📊 $name: ${MonitorUtils.formatNumber(count)} 条记录")
    count
  }

  case class AggregatedMetrics(
                                totalCount: Long,
                                badDistanceCount: Long,
                                badAmountCount: Long,
                                distinctTripId: Long
                              ) {
    def badRatio: Double = if (totalCount > 0) (badDistanceCount + badAmountCount).toDouble / totalCount else 0.0
    def isUnique: Boolean = totalCount == distinctTripId
  }
}