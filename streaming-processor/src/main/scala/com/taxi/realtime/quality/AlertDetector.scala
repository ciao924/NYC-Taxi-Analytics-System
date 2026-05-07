package com.taxi.realtime.quality

import com.taxi.realtime.config.ConfigManager
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import java.sql.{Connection, Date, DriverManager, PreparedStatement, ResultSet}
import java.util

class AlertDetector extends KeyedProcessFunction[String, QualityWindowResult, AlertRecord] {
  private var alertConfigs: ListState[AlertConfig] = _
  private var lastRefreshTime: Long = 0L
  private val refreshIntervalMs = 60000L

  override def open(parameters: Configuration): Unit = {
    val stateDesc = new ListStateDescriptor[AlertConfig]("alertConfigs", classOf[AlertConfig])
    alertConfigs = getRuntimeContext.getListState(stateDesc)
    loadAlertConfigs()
  }

  override def processElement(value: QualityWindowResult, ctx: KeyedProcessFunction[String, QualityWindowResult, AlertRecord]#Context, out: Collector[AlertRecord]): Unit = {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastRefreshTime > refreshIntervalMs) {
      loadAlertConfigs()
    }

    import scala.collection.JavaConverters._
    val configs = alertConfigs.get().asScala.toList

    configs.foreach { config =>
      val actual = config.checkType match {
        case "json_parse_error_rate" => value.parseErrorRate
        case "data_clean_error_rate" => value.cleanErrorRate
        case "timestamp_error_rate" => value.timestampErrorRate
        case "overall_quality_rate" => value.overallQualityRate
        case _ => return
      }

      val threshold = if (config.thresholdType == "WARNING") config.warningThreshold else config.criticalThreshold
      val isTriggered = config.checkType match {
        case "overall_quality_rate" => actual < threshold
        case _ => actual > threshold
      }

      if (isTriggered) {
        out.collect(AlertRecord(
          alertConfigId = config.id,
          alertLevel = config.thresholdType,
          alertContent = s"${config.alertName}: ${config.checkType} 实际值 ${actual.setScale(4, scala.math.BigDecimal.RoundingMode.HALF_UP)} 超过阈值 ${threshold}",
          checkDate = new Date(System.currentTimeMillis()),
          tableName = "realtime_trip",
          checkType = config.checkType,
          actualValue = actual,
          thresholdValue = threshold
        ))
      }
    }
  }

  private def loadAlertConfigs(): Unit = {
    var conn: Connection = null
    var stmt: PreparedStatement = null
    var rs: ResultSet = null

    try {
      conn = DriverManager.getConnection(
        ConfigManager.getMysqlUrl,
        ConfigManager.getMysqlUsername,
        ConfigManager.getMysqlPassword
      )
      stmt = conn.prepareStatement("SELECT id, alert_name, check_type, table_name, threshold_type, warning_threshold, critical_threshold, enabled, webhook_url, email_recipients FROM quality_alert_config WHERE enabled = 1")
      rs = stmt.executeQuery()

      val configs = new util.ArrayList[AlertConfig]()
      while (rs.next()) {
        configs.add(AlertConfig(
          id = rs.getLong("id"),
          alertName = rs.getString("alert_name"),
          checkType = rs.getString("check_type"),
          tableName = Option(rs.getString("table_name")),
          thresholdType = rs.getString("threshold_type"),
          warningThreshold = rs.getBigDecimal("warning_threshold"),
          criticalThreshold = rs.getBigDecimal("critical_threshold"),
          enabled = rs.getBoolean("enabled"),
          webhookUrl = Option(rs.getString("webhook_url")),
          emailRecipients = Option(rs.getString("email_recipients"))
        ))
      }

      alertConfigs.clear()
      alertConfigs.addAll(configs)
      lastRefreshTime = System.currentTimeMillis()

    } catch {
      case e: Exception =>
        org.slf4j.LoggerFactory.getLogger(classOf[AlertDetector]).error("Failed to load alert configs", e)
    } finally {
      if (rs != null) rs.close()
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }
  }
}