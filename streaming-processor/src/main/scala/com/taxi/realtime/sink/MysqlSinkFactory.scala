package com.taxi.realtime.sink

import com.taxi.realtime.config.ConfigManager
import com.taxi.realtime.model.{FeeComposition, HotspotResult, OrderMetric}
import org.apache.flink.connector.jdbc.{JdbcConnectionOptions, JdbcExecutionOptions, JdbcSink}
import org.apache.flink.streaming.api.functions.sink.SinkFunction

import java.io.Serializable
import java.sql.{PreparedStatement, Timestamp}

object MysqlSinkFactory {

  def buildOrderMetricsSink(): SinkFunction[OrderMetric] = {
    val sql =
      """INSERT INTO realtime_order_metrics (window_start, window_end, city, order_count, total_fare, avg_fare)
        |VALUES (?, ?, ?, ?, ?, ?)
        |ON DUPLICATE KEY UPDATE
        |  order_count = VALUES(order_count),
        |  total_fare = VALUES(total_fare),
        |  avg_fare = VALUES(avg_fare)
        |""".stripMargin

    JdbcSink.sink(
      sql,
      new org.apache.flink.connector.jdbc.JdbcStatementBuilder[OrderMetric] with Serializable {
        override def accept(ps: PreparedStatement, metric: OrderMetric): Unit = {
          ps.setTimestamp(1, new Timestamp(metric.windowStart))
          ps.setTimestamp(2, new Timestamp(metric.windowEnd))
          ps.setString(3, metric.city)
          ps.setLong(4, metric.orderCount)
          ps.setDouble(5, metric.totalFare)
          ps.setDouble(6, metric.avgFare)
        }
      },
      JdbcExecutionOptions.builder()
        .withBatchSize(1000)
        .withBatchIntervalMs(5000)
        .withMaxRetries(3)
        .build(),
      new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
        .withUrl(ConfigManager.getMysqlUrl)
        .withUsername(ConfigManager.getMysqlUsername)
        .withPassword(ConfigManager.getMysqlPassword)
        .withDriverName("com.mysql.cj.jdbc.Driver")
        .build()
    )
  }

  def buildHotspotSink(): SinkFunction[HotspotResult] = {
    val sql =
      """INSERT INTO realtime_hotspot_topn (window_start, window_end, zone, cnt, `rank`)
        |VALUES (?, ?, ?, ?, ?)
        |ON DUPLICATE KEY UPDATE
        |  cnt = VALUES(cnt),
        |  `rank` = VALUES(`rank`)
        |""".stripMargin

    JdbcSink.sink(
      sql,
      new org.apache.flink.connector.jdbc.JdbcStatementBuilder[HotspotResult] with Serializable {
        override def accept(ps: PreparedStatement, result: HotspotResult): Unit = {
          ps.setTimestamp(1, new Timestamp(result.windowStart))
          ps.setTimestamp(2, new Timestamp(result.windowEnd))
          ps.setString(3, result.zone)
          ps.setLong(4, result.cnt)
          ps.setInt(5, result.rank)
        }
      },
      JdbcExecutionOptions.builder()
        .withBatchSize(1000)
        .withBatchIntervalMs(5000)
        .withMaxRetries(3)
        .build(),
      new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
        .withUrl(ConfigManager.getMysqlUrl)
        .withUsername(ConfigManager.getMysqlUsername)
        .withPassword(ConfigManager.getMysqlPassword)
        .withDriverName("com.mysql.cj.jdbc.Driver")
        .build()
    )
  }

  def buildFeeCompositionSink(): SinkFunction[FeeComposition] = {
    val sql =
      """INSERT INTO realtime_fee_composition (window_start, window_end, payment_type, total_amount)
        |VALUES (?, ?, ?, ?)
        |ON DUPLICATE KEY UPDATE
        |  total_amount = VALUES(total_amount)
        |""".stripMargin

    JdbcSink.sink(
      sql,
      new org.apache.flink.connector.jdbc.JdbcStatementBuilder[FeeComposition] with Serializable {
        override def accept(ps: PreparedStatement, composition: FeeComposition): Unit = {
          ps.setTimestamp(1, new Timestamp(composition.windowStart))
          ps.setTimestamp(2, new Timestamp(composition.windowEnd))
          ps.setString(3, composition.paymentType)
          ps.setDouble(4, composition.totalAmount)
        }
      },
      JdbcExecutionOptions.builder()
        .withBatchSize(1000)
        .withBatchIntervalMs(5000)
        .withMaxRetries(3)
        .build(),
      new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
        .withUrl(ConfigManager.getMysqlUrl)
        .withUsername(ConfigManager.getMysqlUsername)
        .withPassword(ConfigManager.getMysqlPassword)
        .withDriverName("com.mysql.cj.jdbc.Driver")
        .build()
    )
  }
}