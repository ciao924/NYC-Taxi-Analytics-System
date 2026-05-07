package com.taxi.realtime.utils

object Constants {
  // Kafka Topics
  val KAFKA_TOPIC_GREEN = "taxi_trip_green"
  val KAFKA_TOPIC_YELLOW = "taxi_trip_yellow"

  // MySQL Tables
  val MYSQL_TABLE_GREEN = "realtime_trip_green"
  val MYSQL_TABLE_YELLOW = "realtime_trip_yellow"

  // Error Messages
  val ERROR_JSON_PARSING = "Error parsing JSON message"
  val ERROR_DATA_QUALITY = "Data quality check failed"
  val ERROR_WRITING_TO_MYSQL = "Error writing to MySQL"

  // Data Quality Rules
  val MIN_TRIP_DISTANCE = 0.0
  val MAX_TRIP_DISTANCE = 120.0
  val MIN_PASSENGER_COUNT = 1
  val MAX_PASSENGER_COUNT = 6
  val MIN_FARE_AMOUNT = 0.0
  val MAX_FARE_AMOUNT = 800.0

  // Job Names
  val JOB_NAME_KAFKA_TO_MYSQL = "KafkaToMySQL"
  val JOB_NAME_KAFKA_TO_HIVE = "KafkaToHive"
}
