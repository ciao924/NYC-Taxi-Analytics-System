package com.taxi.etl.common

import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory

object SparkSessionFactory {

  private val logger = LoggerFactory.getLogger(getClass)

  def create(appName: String, enableHive: Boolean = true): SparkSession = {
    val fullAppName = s"$appName-${Version.VERSION}"

    val warehouseDir = ConfigManager.getStringOrDefault("spark.warehouse.dir",
      "hdfs://192.168.127.102:8020/user/hive_local/warehouse")
    val metastoreUris = ConfigManager.getStringOrDefault("hive.metastore.uris",
      "thrift://192.168.127.102:9083")
    val shufflePartitions = ConfigManager.getIntOrDefault("spark.shuffle-partitions", 200)

    val isLocal = sys.props.get("spark.master").exists(_.startsWith("local")) ||
      sys.props.get("spark.submit.deployMode").isEmpty

    logger.info("=" * 80)
    logger.info("Spark Session 配置信息:")
    logger.info(s"  应用名称: $fullAppName")
    logger.info(s"  Warehouse目录: $warehouseDir")
    logger.info(s"  Metastore URI: $metastoreUris")
    logger.info(s"  Shuffle分区数: $shufflePartitions")
    logger.info(s"  本地模式: $isLocal")
    logger.info("=" * 80)

    val builder = SparkSession.builder()
      .appName(fullAppName)

    if (isLocal) {
      logger.info("设置 master = local[*]")
      builder.master("local[*]")
      // 【修正】本地模式增加内存配置
      builder.config("spark.driver.memory", "8g")
      builder.config("spark.driver.memoryOverhead", "2g")
    } else {
      // 【修正】集群模式配置
      builder.config("spark.executor.memory", "8g")
      builder.config("spark.executor.memoryOverhead", "2g")
      builder.config("spark.executor.cores", "4")
      builder.config("spark.driver.memory", "4g")
    }

    // 基础配置
    builder
      .config("spark.sql.adaptive.enabled", "true")
      .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
      .config("spark.sql.adaptive.skewJoin.enabled", "true")
      .config("spark.sql.adaptive.skewJoin.skewedPartitionThresholdInBytes", "64MB")
      .config("spark.sql.adaptive.advisoryPartitionSizeInBytes", "128MB")
      .config("spark.sql.shuffle.partitions", shufflePartitions)
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .config("spark.kryoserializer.buffer.max", "512m")
      .config("spark.sql.warehouse.dir", warehouseDir)
      .config("hive.metastore.uris", metastoreUris)
      // 【修正】增加缓存内存比例
      .config("spark.memory.fraction", "0.8")
      .config("spark.memory.storageFraction", "0.5")
      .config("spark.sql.autoBroadcastJoinThreshold", "50MB")

    if (!isLocal) {
      builder
        .config("spark.dynamicAllocation.enabled", "true")
        .config("spark.dynamicAllocation.maxExecutors", "50")
        .config("spark.dynamicAllocation.minExecutors", "5")
    }

    val spark: SparkSession = if (enableHive) {
      logger.info("启用 Hive 支持")
      builder.enableHiveSupport().getOrCreate()
    } else {
      builder.getOrCreate()
    }

    try {
      spark.sql("SHOW DATABASES").show()
      logger.info("✅ Hive 连接成功")
    } catch {
      case e: Exception =>
        logger.warn(s"Hive 连接失败: ${e.getMessage}")
    }

    SparkListenerMetrics.register(spark)
    logger.info("✅ Spark Listener 已注册")

    try {
      Class.forName("org.apache.iceberg.spark.SparkCatalog")
      IcebergTableManager.initCatalog(spark)
      logger.info("✅ Iceberg Catalog 初始化成功")
    } catch {
      case _: Exception => logger.info("Iceberg 未启用")
    }

    logger.info(s"✅ Spark Session 创建成功: $fullAppName")
    logger.info(s"   版本: ${Version.VERSION}")
    logger.info(s"   Master: ${spark.sparkContext.master}")
    logger.info(s"   Warehouse: ${spark.conf.get("spark.sql.warehouse.dir")}")
    logger.info(s"   Metastore: ${spark.conf.get("hive.metastore.uris")}")

    spark
  }
}