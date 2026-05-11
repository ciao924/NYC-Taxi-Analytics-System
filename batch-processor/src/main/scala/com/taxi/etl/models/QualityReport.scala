package com.taxi.etl.models

import com.taxi.etl.utils.MonitorUtils

case class QualityReport(
                          tableName: String,
                          pass: Boolean,
                          originalCount: Long,
                          finalCount: Long,
                          retentionRate: Double,
                          anomalyRatio: Double,
                          uniquePass: Boolean,
                          rangePass: Boolean,
                          executionId: String
                        ) {

  def getFailureReason: String = {
    val failures = scala.collection.mutable.ListBuffer[String]()
    if (finalCount == 0) failures += "数据为空"
    // 放宽留存率期望范围（告警线，不是过滤线）
    if (retentionRate < 75.0 || retentionRate > 90.0)
      failures += f"留存率异常: $retentionRate%.2f%% (期望 75-90%%)"
    if (anomalyRatio > 5.0)
      failures += f"异常比例过高: $anomalyRatio%.2f%% (阈值 < 5%%)"
    if (!uniquePass) failures += "唯一性检测失败"
    if (!rangePass) failures += "范围检测失败"
    if (failures.isEmpty) "未知原因" else failures.mkString("; ")
  }

  def format(): String = {
    f"""
       |┌─────────────────────────────────────────────────────────────┐
       |│ 质量检测报告: $tableName
       |├─────────────────────────────────────────────────────────────┤
       |│ 原始记录数: ${MonitorUtils.formatNumber(originalCount)}
       |│ 最终记录数: ${MonitorUtils.formatNumber(finalCount)}
       |│ 留存率:     $retentionRate%.2f%% (期望 75-90%%)
       |│ 异常比例:   $anomalyRatio%.2f%% (阈值 < 5%%)
       |│ 唯一性:     ${if (uniquePass) "✅" else "❌"}
       |│ 范围检测:   ${if (rangePass) "✅" else "❌"}
       |│ 最终结果:   ${if (pass) "✅ 通过" else "❌ 失败"}
       |└─────────────────────────────────────────────────────────────┘
     """.stripMargin
  }
}