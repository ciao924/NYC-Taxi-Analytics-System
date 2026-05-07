package com.taxi.realtime.source

import com.taxi.realtime.config.ConfigManager
import com.taxi.realtime.utils.LoggerUtil
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.kafka.clients.consumer.OffsetResetStrategy

import java.util.Properties

object KafkaSourceBuilder {
  def build(): KafkaSource[String] = {
    build(ConfigManager.getKafkaConsumerGroupId)
  }

  def build(groupId: String): KafkaSource[String] = {
    try {
      val bootstrapServers = ConfigManager.getKafkaBootstrapServers
      val topics = ConfigManager.getKafkaTopics
      val sessionTimeoutMs = ConfigManager.getKafkaSessionTimeoutMs
      val maxPollRecords = ConfigManager.getKafkaMaxPollRecords

      val properties = new Properties()
      properties.setProperty("bootstrap.servers", bootstrapServers)
      properties.setProperty("group.id", groupId)
      properties.setProperty("session.timeout.ms", sessionTimeoutMs.toString)
      properties.setProperty("max.poll.records", maxPollRecords.toString)

      val autoOffsetReset = ConfigManager.getKafkaConsumerAutoOffsetReset
      val offsetResetStrategy = autoOffsetReset.toLowerCase match {
        case "earliest" => OffsetResetStrategy.EARLIEST
        case "latest" => OffsetResetStrategy.LATEST
        case _ => OffsetResetStrategy.EARLIEST
      }

      val source = KafkaSource.builder[String]
        .setBootstrapServers(bootstrapServers)
        .setTopics(topics:_*)
        .setGroupId(groupId)
        .setStartingOffsets(OffsetsInitializer.earliest())
        .setValueOnlyDeserializer(new SimpleStringSchema())
        .setProperties(properties)
        .build()

      LoggerUtil.info(s"Kafka source built successfully with bootstrap servers: $bootstrapServers, groupId: $groupId, topics: ${topics.mkString(", ")}")
      source
    } catch {
      case e: Exception =>
        LoggerUtil.error("Failed to build Kafka source", e)
        throw new RuntimeException("Failed to build Kafka source", e)
    }
  }
}
