package com.taxi.realtime.sink

import com.taxi.realtime.config.ConfigManager
import com.taxi.realtime.quality.QualityWindowResult
import org.apache.flink.connector.jdbc.{JdbcConnectionOptions, JdbcExecutionOptions, JdbcSink, JdbcStatementBuilder}
import org.apache.flink.streaming.api.functions.sink.SinkFunction

import java.io.Serializable
import java.sql.{Date, PreparedStatement}
import java.time.Instant
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object QualitySinkBuilder {

  def build(): SinkFunction[QualityWindowResult] = {
    val insertSql =
      """
        |INSERT INTO data_quality_daily
        |(check_date, table_name, check_type, check_status, expected_value, actual_value, deviation_rate, detail_json)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      """.stripMargin

    JdbcSink.sink(
      insertSql,
      new QualityStatementBuilder(),
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

class QualityStatementBuilder extends JdbcStatementBuilder[QualityWindowResult] with Serializable {

  override def accept(ps: PreparedStatement, result: QualityWindowResult): Unit = {
    val checkDate = new Date(Instant.now.toEpochMilli)
    val metrics = Seq(
      ("json_parse_error_rate", result.parseErrorRate, BigDecimal(5.0)),
      ("data_clean_error_rate", result.cleanErrorRate, BigDecimal(10.0)),
      ("timestamp_error_rate", result.timestampErrorRate, BigDecimal(5.0)),
      ("overall_quality_rate", result.overallQualityRate, BigDecimal(95.0))
    )
    metrics.foreach { case (checkType, actualValue, expectedValue) =>
      val status = checkType match {
        case "overall_quality_rate" => if (actualValue >= expectedValue) "PASS" else "FAIL"
        case _ => if (actualValue <= expectedValue) "PASS" else "FAIL"
      }
      val deviation = if (expectedValue != BigDecimal(0)) (actualValue - expectedValue) / expectedValue else BigDecimal(0)
      val detail = QualityStatementBuilder.mapper.writeValueAsString(Map(
        "windowStart" -> result.windowStart,
        "windowEnd" -> result.windowEnd,
        "totalRecords" -> result.totalRecords,
        "parseErrors" -> result.parseErrors,
        "cleanErrors" -> result.cleanErrors,
        "timestampErrors" -> result.timestampErrors,
        "validRecords" -> result.validRecords
      ))
      ps.setDate(1, checkDate)
      ps.setString(2, "realtime_trip")
      ps.setString(3, checkType)
      ps.setString(4, status)
      ps.setBigDecimal(5, expectedValue.bigDecimal)
      ps.setBigDecimal(6, actualValue.bigDecimal)
      ps.setBigDecimal(7, deviation.bigDecimal)
      ps.setString(8, detail)
      ps.addBatch()
    }
    ps.executeBatch()
  }
}

object QualityStatementBuilder {
  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
}