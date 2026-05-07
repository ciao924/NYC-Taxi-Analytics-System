package com.taxi.realtime.sink

import com.taxi.realtime.config.ConfigManager
import com.taxi.realtime.model.ErrorRecordJava
import com.taxi.realtime.utils.LoggerUtil
import org.apache.flink.connector.jdbc.{JdbcConnectionOptions, JdbcExecutionOptions, JdbcSink}
import org.apache.flink.streaming.api.functions.sink.SinkFunction

import java.io.Serializable
import java.sql.PreparedStatement

object DeadLetterSinkBuilder {
  def build(): SinkFunction[ErrorRecordJava] = {
    try {
      val url = ConfigManager.getMysqlUrl
      val username = ConfigManager.getMysqlUsername
      val password = ConfigManager.getMysqlPassword
      val table = "dead_letter_queue"
      val batchSize = ConfigManager.getMysqlBatchSize
      val batchIntervalMs = ConfigManager.getMysqlBatchIntervalMs

      val insertSql = s"INSERT INTO $table " +
        "(original_message, error_reason, process_time) " +
        "VALUES (?, ?, NOW())"

      val executionOptions = JdbcExecutionOptions.builder()
        .withBatchSize(batchSize)
        .withBatchIntervalMs(batchIntervalMs)
        .withMaxRetries(3)
        .build()

      val connectionOptions = new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
        .withUrl(url)
        .withUsername(username)
        .withPassword(password)
        .withDriverName("com.mysql.cj.jdbc.Driver")
        .build()

      // 使用可序列化的匿名内部类替代 lambda
      JdbcSink.sink[ErrorRecordJava](
        insertSql,
        new org.apache.flink.connector.jdbc.JdbcStatementBuilder[ErrorRecordJava] with Serializable {
          override def accept(ps: PreparedStatement, record: ErrorRecordJava): Unit = {
            val originalMsg = if (record != null) record.getOriginalData else ""
            val errorReason = if (record != null) record.getErrorReason else "Null record"
            ps.setString(1, if (originalMsg != null) originalMsg else "")
            ps.setString(2, if (errorReason != null) errorReason else "")
          }
        },
        executionOptions,
        connectionOptions
      )
    } catch {
      case e: Exception =>
        LoggerUtil.error("Failed to build Dead Letter sink", e)
        throw new RuntimeException("Failed to build Dead Letter sink", e)
    }
  }
}