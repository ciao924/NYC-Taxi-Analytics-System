package com.taxi.realtime.config

import java.io.{File, FileInputStream, InputStream}
import java.util.Properties

object ConfigManager {
  private var properties: Properties = _

  def init(args: Array[String]): Unit = {
    val props = new Properties()

    // 从 args 中解析 --config 参数
    var configFile: String = null
    var i = 0
    while (i < args.length) {
      if (args(i) == "--config" && i + 1 < args.length) {
        configFile = args(i + 1)
        i += 2
      } else {
        i += 1
      }
    }

    var input: InputStream = null
    if (configFile != null) {
      val file = new File(configFile)
      if (!file.exists()) {
        throw new RuntimeException(s"Specified config file does not exist: $configFile")
      }
      println(s"Loading configuration from external file: $configFile")
      input = new FileInputStream(file)
    } else {
      // 未指定 --config，尝试系统属性，最后 fallback classpath
      val sysConfig = System.getProperty("config.file")
      if (sysConfig != null && new File(sysConfig).exists()) {
        println(s"Loading configuration from system property: $sysConfig")
        input = new FileInputStream(sysConfig)
      } else {
        input = getClass.getClassLoader.getResourceAsStream("application.properties")
        if (input == null) {
          throw new RuntimeException("No configuration file found. Please specify --config <file> or place application.properties on classpath.")
        }
        println("Loading configuration from classpath: application.properties")
      }
    }

    props.load(input)
    input.close()

    // 打印关键配置用于调试
    println("=== Configuration Loaded ===")
    println(s"kafka.bootstrap.servers = ${props.getProperty("kafka.bootstrap.servers")}")
    println(s"kafka.topic = ${props.getProperty("kafka.topic")}")
    println(s"kafka.consumer.group.id = ${props.getProperty("kafka.consumer.group.id")}")
    println(s"hive.metastore.uris = ${props.getProperty("hive.metastore.uris")}")
    println("==========================")

    properties = props
  }

  def getKafkaBootstrapServers: String = {
    properties.getProperty("kafka.bootstrap.servers")
  }

  def getKafkaTopic: String = {
    properties.getProperty("kafka.topic")
  }

  def getKafkaTopics: Array[String] = {
    val topics = properties.getProperty("kafka.topics", properties.getProperty("kafka.topic"))
    topics.split(",").map(_.trim)
  }

  def getKafkaConsumerGroupId: String = {
    properties.getProperty("kafka.consumer.group.id")
  }

  def getKafkaConsumerAutoOffsetReset: String = {
    properties.getProperty("kafka.consumer.auto.offset.reset")
  }

  def getMysqlUrl: String = {
    properties.getProperty("mysql.url")
  }

  def getMysqlQualityUrl: String = {
    properties.getProperty("mysql.url.quality", getMysqlUrl)
  }

  def getMysqlUsername: String = {
    properties.getProperty("mysql.username")
  }

  def getMysqlPassword: String = {
    properties.getProperty("mysql.password")
  }

  def getMysqlTable: String = {
    properties.getProperty("mysql.table")
  }

  def getFlinkCheckpointInterval: Long = {
    properties.getProperty("flink.checkpoint.interval").toLong
  }

  def getFlinkCheckpointTimeout: Long = {
    properties.getProperty("flink.checkpoint.timeout").toLong
  }

  def getFlinkParallelism: Int = {
    properties.getProperty("flink.parallelism").toInt
  }

  def isDataQualityFilterEnabled: Boolean = {
    properties.getProperty("data.quality.filter.enabled").toBoolean
  }

  def getMysqlBatchSize: Int = {
    properties.getProperty("mysql.batch.size", "1000").toInt
  }

  def getMysqlBatchIntervalMs: Long = {
    properties.getProperty("mysql.batch.interval.ms", "5000").toLong
  }

  def getFlinkCheckpointStoragePath: String = {
    properties.getProperty("flink.checkpoint.storage.path", "hdfs://hadoop102:8020/flink/checkpoints")
  }

  def getKafkaSessionTimeoutMs: Int = {
    properties.getProperty("kafka.consumer.session.timeout.ms", "30000").toInt
  }

  def getKafkaMaxPollRecords: Int = {
    properties.getProperty("kafka.consumer.max.poll.records", "500").toInt
  }

  def getMysqlSinkParallelism: Int = {
    properties.getProperty("mysql.sink.parallelism", "1").toInt
  }

  def getFlinkExecutionBufferTimeout: Int = {
    properties.getProperty("flink.execution.buffer.timeout", "100").toInt
  }

  def getFlinkTaskmanagerNetworkMemoryFraction: Double = {
    properties.getProperty("flink.taskmanager.network.memory.fraction", "0.1").toDouble
  }

  // Hive Configuration
  def getHiveMetastoreUris: String = {
    require(properties != null, "ConfigManager has not been initialized. Call ConfigManager.init() first.")
    val uri = properties.getProperty("hive.metastore.uris")
    require(uri != null && uri.nonEmpty, "Missing required config: hive.metastore.uris")
    uri
  }

  def getHiveDatabase: String = {
    require(properties != null, "ConfigManager has not been initialized. Call ConfigManager.init() first.")
    val db = properties.getProperty("hive.database")
    require(db != null && db.nonEmpty, "Missing required config: hive.database")
    db
  }

  def getHiveTableGreen: String = {
    require(properties != null, "ConfigManager has not been initialized. Call ConfigManager.init() first.")
    val table = properties.getProperty("hive.table.green")
    require(table != null && table.nonEmpty, "Missing required config: hive.table.green")
    table
  }

  def getHiveTableYellow: String = {
    require(properties != null, "ConfigManager has not been initialized. Call ConfigManager.init() first.")
    val table = properties.getProperty("hive.table.yellow")
    require(table != null && table.nonEmpty, "Missing required config: hive.table.yellow")
    table
  }

  def getHiveBatchSize: Int = {
    properties.getProperty("hive.batch.size", "1000").toInt
  }

  def getHiveBatchIntervalMs: Long = {
    properties.getProperty("hive.batch.interval.ms", "5000").toLong
  }

  def getHiveWarehousePath: String = {
    properties.getProperty("hive.warehouse.path",
      "hdfs://hadoop102:8020/user/hive_local/warehouse/nyc_taxi_ods.db/taxi_trip_green_ods")
  }

  def getOdsDataPath: String = {
    properties.getProperty("data.ods.path",
      "hdfs://hadoop102:8020/user/hive_local/warehouse/nyc_taxi_ods.db/taxi_trip_yellow_ods")
  }

  def getOrcCompress: String = {
    properties.getProperty("hive.orc.compress", "SNAPPY")
  }

  def getOrcStripeSize: Long = {
    properties.getProperty("hive.orc.stripe.size", "268435456").toLong
  }

  def isDataQualityEnabled: Boolean = {
    properties.getProperty("data.quality.enabled", "true").toBoolean
  }

  def getDataQualityAlertRefreshSeconds: Int = {
    properties.getProperty("data.quality.alert.refresh.seconds", "60").toInt
  }

  def getQualityExpectedParseErrorRate: Double = {
    properties.getProperty("quality.expected.parse.error.rate", "0.05").toDouble
  }

  def getQualityExpectedCleanErrorRate: Double = {
    properties.getProperty("quality.expected.clean.error.rate", "0.10").toDouble
  }

  def getQualityExpectedOverallQualityRate: Double = {
    properties.getProperty("quality.expected.overall.quality.rate", "0.95").toDouble
  }
}