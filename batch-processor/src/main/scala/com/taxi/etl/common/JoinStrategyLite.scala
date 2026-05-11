package com.taxi.etl.common

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.broadcast
import org.slf4j.LoggerFactory

object JoinStrategyLite {

  private val logger = LoggerFactory.getLogger(getClass)
  private val BROADCAST_THRESHOLD_MB = ConfigManager.getBroadcastThresholdMB

  def smartJoin(
                 fact: DataFrame,
                 dim: DataFrame,
                 joinCol: String,
                 factName: String = "fact",
                 dimName: String = "dim"
               ): DataFrame = {
    val dimSizeMB = estimateSizeMB(dim)

    if (dimSizeMB > 0 && dimSizeMB < BROADCAST_THRESHOLD_MB) {
      logger.info(f"Broadcast Join: $dimName ($dimSizeMB%.2fMB < $BROADCAST_THRESHOLD_MB%dMB)")
      fact.join(broadcast(dim), joinCol)
    } else {
      logger.info(f"Shuffle Join: $dimName ($dimSizeMB%.2fMB) - 交给 AQE 优化")
      fact.join(dim, joinCol)
    }
  }

  def smartJoinMulti(
                      fact: DataFrame,
                      dim: DataFrame,
                      joinCols: Seq[String],
                      factName: String = "fact",
                      dimName: String = "dim"
                    ): DataFrame = {
    val dimSizeMB = estimateSizeMB(dim)

    if (dimSizeMB > 0 && dimSizeMB < BROADCAST_THRESHOLD_MB) {
      logger.info(f"Broadcast Join: $dimName ($dimSizeMB%.2fMB < $BROADCAST_THRESHOLD_MB%dMB)")
      fact.join(broadcast(dim), joinCols)
    } else {
      logger.info(f"Shuffle Join: $dimName ($dimSizeMB%.2fMB) - 交给 AQE 优化")
      fact.join(dim, joinCols)
    }
  }

  private def estimateSizeMB(df: DataFrame): Double = {
    try {
      val size: BigInt = df.queryExecution.optimizedPlan.stats.sizeInBytes
      val sizeInMB: Double = size.toDouble / (1024.0 * 1024.0)
      if (sizeInMB <= 0) Double.MaxValue else sizeInMB
    } catch {
      case _: Exception => Double.MaxValue
    }
  }
}