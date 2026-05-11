package com.taxi.etl.models

import com.taxi.etl.utils.MonitorUtils

case class BuildResult(
                        success: Boolean,
                        tableName: String,
                        rowCount: Long,
                        errorMessage: String,
                        durationSeconds: Long,
                        qualityReport: String = "",
                        metrics: Map[String, Any] = Map.empty
                      ) {
  def isSuccess: Boolean = success && rowCount > 0
  def summary: String = s"$tableName: ${if (success) "✅" else "❌"} ${MonitorUtils.formatNumber(rowCount)}条, ${durationSeconds}s"
}

object BuildResult {
  def success(tableName: String, rowCount: Long, durationSeconds: Long, qualityReport: String = ""): BuildResult =
    BuildResult(true, tableName, rowCount, "", durationSeconds, qualityReport)
  def failure(tableName: String, errorMessage: String, durationSeconds: Long): BuildResult =
    BuildResult(false, tableName, 0, errorMessage, durationSeconds)
}