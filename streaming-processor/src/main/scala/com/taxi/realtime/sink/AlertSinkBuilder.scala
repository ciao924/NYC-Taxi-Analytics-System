package com.taxi.realtime.sink

import com.taxi.realtime.config.ConfigManager
import com.taxi.realtime.quality.AlertRecord
import org.apache.flink.connector.jdbc.{JdbcConnectionOptions, JdbcExecutionOptions, JdbcSink, JdbcStatementBuilder}
import org.apache.flink.streaming.api.functions.sink.SinkFunction

import java.io.Serializable
import java.sql.{PreparedStatement, Timestamp}

object AlertSinkBuilder {

  def build(): SinkFunction[AlertRecord] = {
    val insertSql =
      """
        |INSERT INTO quality_alert_history
        |(alert_config_id, alert_level, alert_content, check_date, table_name, check_type, actual_value, threshold_value, is_resolved, resolve_time, create_time)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """.stripMargin

    JdbcSink.sink(
      insertSql,
      new AlertStatementBuilder(),
      JdbcExecutionOptions.builder()
        .withBatchSize(1000)
        .withBatchIntervalMs(5000)
        .withMaxRetries(3)
        .build(),
      new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
        .withUrl(ConfigManager.getMysqlQualityUrl)
        .withUsername(ConfigManager.getMysqlUsername)
        .withPassword(ConfigManager.getMysqlPassword)
        .withDriverName("com.mysql.cj.jdbc.Driver")
        .build()
    )
  }
}

class AlertStatementBuilder extends JdbcStatementBuilder[AlertRecord] with Serializable {

  override def accept(ps: PreparedStatement, record: AlertRecord): Unit = {
    ps.setLong(1, record.alertConfigId)
    ps.setString(2, record.alertLevel)
    ps.setString(3, record.alertContent)
    ps.setDate(4, record.checkDate)
    ps.setString(5, record.tableName)
    ps.setString(6, record.checkType)
    ps.setBigDecimal(7, record.actualValue.bigDecimal)
    ps.setBigDecimal(8, record.thresholdValue.bigDecimal)
    ps.setInt(9, if (record.isResolved) 1 else 0)
    if (record.resolveTime.isDefined) {
      ps.setTimestamp(10, record.resolveTime.get)
    } else {
      ps.setNull(10, java.sql.Types.TIMESTAMP)
    }
    ps.setTimestamp(11, record.createTime)
  }
}