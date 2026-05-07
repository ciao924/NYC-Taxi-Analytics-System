# 出租车大数据分析系统 - ETL 开发规范

**文档版本**：v1.0
**创建日期**：2026-04-26
**适用版本**：v4.0+

---

## 一、ETL 作业结构规范

### 1.1 目录结构

```
spark_taxi/
├── src/
│   └── main/
│       ├── java/
│       │   └── taxi-analysis/           # Java 作业（旧代码）
│       └── scala/
│           └── com/
│               └── taxi/
│                   └── etl/
│                       ├── ods/          # ODS 层加载器
│                       │   ├── GreenOdsLoader.scala
│                       │   ├── YellowOdsLoader.scala
│                       │   └── OdsValidator.scala
│                       ├── dwd/          # DWD 层构建器
│                       │   └── DwdLayerBuilder.scala
│                       ├── dws/          # DWS 层构建器
│                       │   └── DwsLayerBuilder.scala
│                       ├── ads/          # ADS 层构建器
│                       │   ├── AdsLayerBuilder.scala
│                       │   ├── base/     # 基础组件
│                       │   ├── daily/    # 日指标
│                       │   ├── fee/      # 费用指标
│                       │   ├── traffic/  # 流量指标
│                       │   └── distribution/  # 分布指标
│                       ├── common/       # 通用工具
│                       │   ├── ConfigManager.scala
│                       │   ├── MetricsCollector.scala
│                       │   └── QualityManager.scala
│                       ├── models/       # 数据模型
│                       └── utils/        # 工具类
├── pom.xml
└── logs/
```

### 1.2 作业类命名规范

| 作业类型 | 命名规则 | 示例 |
|----------|----------|------|
| ODS 加载器 | `{Source}OdsLoader` | GreenOdsLoader |
| ODS 验证器 | `OdsValidator` | OdsValidator |
| DWD 构建器 | `{主题}LayerBuilder` | DwdLayerBuilder |
| DWS 构建器 | `{主题}LayerBuilder` | DwsLayerBuilder |
| ADS 构建器 | `{指标类型}Builder` | KpiDailyBuilder |
| ADS 编排器 | `AdsLayerBuilder` | AdsLayerBuilder |

---

## 二、Spark ETL 作业模板

### 2.1 ODS 层加载器模板

```scala
package com.taxi.etl.ods

import com.taxi.etl.common.{SparkSessionFactory, MetricsCollector}
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{SparkSession, DataFrame}
import org.slf4j.LoggerFactory

/**
 * {数据表} ODS 加载器
 * 功能：{描述}
 * 数据源：{数据源}
 * 作者：{作者}
 * 日期：{日期}
 */
object {Source}OdsLoader {

  private val logger = LoggerFactory.getLogger(getClass)
  private val SOURCE_TABLE = "{source_table_name}"
  private val TARGET_TABLE = "{target_table_name}"

  def main(args: Array[String]): Unit = {
    val spark = SparkSessionFactory.getSparkSession()

    try {
      val ctx = JobContext(
        executionId = args.lift(0).getOrElse(java.util.UUID.randomUUID().toString),
        startDate = args.lift(1).getOrElse(throw new IllegalArgumentException("startDate is required")),
        endDate = args.lift(2).getOrElse(args.lift(1).getOrElse("")),
        tableName = TARGET_TABLE
      )

      val metrics = new MetricsCollector()

      logger.info(s"开始加载 ODS 数据: ${ctx.startDate} ~ ${ctx.endDate}")

      val result = load(spark, ctx, metrics)

      if (result.success) {
        logger.info(s"ODS 加载成功: ${result.rowCount} 条记录")
        System.exit(0)
      } else {
        logger.error(s"ODS 加载失败: ${result.errorMessage.getOrElse("Unknown error")}")
        System.exit(1)
      }

    } catch {
      case e: Exception =>
        logger.error(s"ODS 加载异常: ${e.getMessage}", e)
        System.exit(1)
    } finally {
      spark.stop()
    }
  }

  def load(
            spark: SparkSession,
            ctx: JobContext,
            metrics: MetricsCollector
          ): LoadResult = {
    import spark.implicits._

    try {
      // Step 1: 读取源数据
      val sourceDf = readSourceData(spark, ctx)

      // Step 2: 数据转换
      val transformedDf = transformData(sourceDf, ctx)

      // Step 3: 数据校验
      val validatedDf = validateData(transformedDf, ctx, metrics)

      // Step 4: 写入目标表
      val rowCount = writeToTarget(validatedDf, ctx, metrics)

      // Step 5: 记录质量指标
      recordQualityMetrics(ctx, metrics)

      LoadResult(success = true, rowCount = rowCount)

    } catch {
      case e: Exception =>
        logger.error(s"加载失败: ${e.getMessage}", e)
        LoadResult(success = false, errorMessage = Some(e.getMessage))
    }
  }

  /**
   * 读取源数据
   */
  private def readSourceData(spark: SparkSession, ctx: JobContext): DataFrame = {
    // TODO: 根据实际数据源实现
    ???
  }

  /**
   * 数据转换
   */
  private def transformData(df: DataFrame, ctx: JobContext): DataFrame = {
    // TODO: 根据实际需求实现
    ???
  }

  /**
   * 数据校验
   */
  private def validateData(
                            df: DataFrame,
                            ctx: JobContext,
                            metrics: MetricsCollector
                          ): DataFrame = {
    val totalCount = df.count()
    metrics.recordJobMetric("ODS", ctx.executionId, s"${TARGET_TABLE}_input_rows", totalCount.toDouble)

    // 校验记录数
    if (totalCount == 0) {
      logger.warn(s"警告: 源数据为空 (${ctx.startDate})")
    }

    df
  }

  /**
   * 写入目标表
   */
  private def writeToTarget(
                             df: DataFrame,
                             ctx: JobContext,
                             metrics: MetricsCollector
                           ): Long = {
    // TODO: 根据实际目标表实现
    ???
  }

  /**
   * 记录质量指标
   */
  private def recordQualityMetrics(
                                    ctx: JobContext,
                                    metrics: MetricsCollector
                                  ): Unit = {
    // TODO: 实现质量指标记录
  }
}

case class LoadResult(
                       success: Boolean,
                       rowCount: Long = 0,
                       errorMessage: Option[String] = None
                     )
```

### 2.2 DWD 层构建器模板

```scala
package com.taxi.etl.dwd

import com.taxi.etl.common.{SparkSessionFactory, MetricsCollector}
import com.taxi.etl.models.{JobContext, BuildResult}
import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.slf4j.LoggerFactory

/**
 * DWD 层构建器
 * 功能：数据清洗、标准化、维度关联
 * 依赖表：ods_taxi_green_trip, ods_taxi_yellow_trip
 * 输出表：dwd_taxi_trip_detail
 */
object DwdLayerBuilder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TARGET_TABLE = "dwd_taxi_trip_detail"

  def main(args: Array[String]): Unit = {
    val spark = SparkSessionFactory.getSparkSession()

    try {
      val ctx = JobContext(
        executionId = args.lift(0).getOrElse(java.util.UUID.randomUUID().toString),
        startDate = args.lift(1).getOrElse(throw new IllegalArgumentException("startDate is required")),
        endDate = args.lift(2).getOrElse(args.lift(1).getOrElse("")),
        tableName = TARGET_TABLE
      )

      val metrics = new MetricsCollector()

      logger.info(s"开始构建 DWD 层: ${ctx.startDate} ~ ${ctx.endDate}")

      val result = build(spark, ctx, metrics)

      if (result.success) {
        logger.info(s"DWD 层构建成功: ${result.rowCount} 条记录")
        System.exit(0)
      } else {
        logger.error(s"DWD 层构建失败: ${result.errorMessage.getOrElse("Unknown error")}")
        System.exit(1)
      }

    } catch {
      case e: Exception =>
        logger.error(s"DWD 层构建异常: ${e.getMessage}", e)
        System.exit(1)
    } finally {
      spark.stop()
    }
  }

  def build(
             spark: SparkSession,
             ctx: JobContext,
             metrics: MetricsCollector
           ): BuildResult = {
    import spark.implicits._

    try {
      // Step 1: 读取 ODS 数据
      val odsGreen = readOdsData(spark, "ods_taxi_green_trip", ctx.startDate, ctx.endDate)
      val odsYellow = readOdsData(spark, "ods_taxi_yellow_trip", ctx.startDate, ctx.endDate)
      val odsUnion = odsGreen.unionByName(odsYellow)

      // Step 2: 数据清洗
      val cleanedDf = cleanData(odsUnion)

      // Step 3: 维度关联
      val dimensionJoinedDf = joinDimensions(cleanedDf, spark)

      // Step 4: 生成业务字段
      val enrichedDf = enrichBusinessFields(dimensionJoinedDf)

      // Step 5: 质量校验
      val (validDf, qualityReport) = qualityCheck(enrichedDf, metrics, ctx)

      // Step 6: 写入目标表
      val rowCount = writeToTarget(validDf, ctx)

      metrics.recordJobMetric("DWD", ctx.executionId, s"${TARGET_TABLE}_rows", rowCount.toDouble)

      BuildResult(success = true, rowCount = rowCount)

    } catch {
      case e: Exception =>
        logger.error(s"DWD 层构建失败: ${e.getMessage}", e)
        BuildResult(success = false, errorMessage = Some(e.getMessage))
    }
  }

  /**
   * 读取 ODS 数据
   */
  private def readOdsData(
                           spark: SparkSession,
                           tableName: String,
                           startDate: String,
                           endDate: String
                         ): DataFrame = {
    spark.sql(s"""
      SELECT *
      FROM ${tableName}
      WHERE dt >= '${startDate}' AND dt <= '${endDate}'
    """)
  }

  /**
   * 数据清洗规则
   */
  private def cleanData(df: DataFrame): DataFrame = {
    df
      // 空值处理
      .withColumn("passenger_count",
        when(col("passenger_count") === 0, lit(null))
          .otherwise(col("passenger_count")))
      // 范围校验
      .filter(col("trip_distance") > 0 && col("trip_distance") < 500)
      .filter(col("fare_amount") >= 0)
      // 时间校验
      .filter(col("tpep_dropoff_datetime") > col("tpep_pickup_datetime"))
  }

  /**
   * 维度关联
   */
  private def joinDimensions(df: DataFrame, spark: SparkSession): DataFrame = {
    val dimPayment = spark.sql("SELECT * FROM dim_payment_type")
    val dimVendor = spark.sql("SELECT * FROM dim_vendor")

    df.join(dimPayment, df("payment_type") === dimPayment("payment_type_id"), "left")
      .join(dimVendor, df("vendor_id") === dimVendor("vendor_id"), "left")
  }

  /**
   * 业务字段生成
   */
  private def enrichBusinessFields(df: DataFrame): DataFrame = {
    df.withColumn("trip_duration_minutes",
      (unix_timestamp(col("dropoff_datetime")) - unix_timestamp(col("pickup_datetime"))) / 60)
      .withColumn("total_amount",
        col("fare_amount") + col("extra") + col("mta_tax") + col("tip_amount") +
          col("tolls_amount") + col("improvement_surcharge"))
  }

  /**
   * 质量校验
   */
  private def qualityCheck(
                            df: DataFrame,
                            metrics: MetricsCollector,
                            ctx: JobContext
                          ): (DataFrame, QualityReport) = {
    val totalCount = df.count()
    val validCount = df.filter(col("is_valid") === true).count()

    metrics.recordJobMetric("DWD", ctx.executionId, s"${TARGET_TABLE}_total", totalCount.toDouble)
    metrics.recordJobMetric("DWD", ctx.executionId, s"${TARGET_TABLE}_valid", validCount.toDouble)

    val report = QualityReport(
      tableName = TARGET_TABLE,
      totalRows = totalCount,
      validRows = validCount,
      invalidRows = totalCount - validCount,
      qualityScore = if (totalCount > 0) validCount.toDouble / totalCount else 0
    )

    (df.filter(col("is_valid") === true), report)
  }

  /**
   * 写入目标表
   */
  private def writeToTarget(df: DataFrame, ctx: JobContext): Long = {
    // 使用 Iceberg 写入
    df.write
      .format("iceberg")
      .mode("append")
      .option("target-table", TARGET_TABLE)
      .option("partition-spec", "dt")
      .save()

    df.count()
  }
}
```

### 2.3 ADS 层 Builder 模板

```scala
package com.taxi.etl.ads.{category}

import com.taxi.etl.ads.base.{BaseAdsWriter, AdsConstants, BuilderResult}
import com.taxi.etl.common.MetricsCollector
import com.taxi.etl.models.JobContext
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

/**
 * {指标名称}构建器
 * 功能：{描述}
 * 依赖表：dws_taxi_daily_stats
 * 输出表：analysis_{table_name}
 */
object {Metric}Builder {

  private val logger = LoggerFactory.getLogger(getClass)
  private val TABLE_NAME = AdsConstants.TableNames.{METRIC_NAME}
  private val UNIQUE_KEYS = AdsConstants.UniqueKeys.{UNIQUE_KEY}

  def build(
             spark: SparkSession,
             df: DataFrame,
             startDate: String,
             endDate: String,
             ctx: JobContext,
             metrics: MetricsCollector
           ): BuilderResult = {
    import spark.implicits._

    try {
      // Step 1: 数据过滤
      val filteredDf = df.filter(col("stat_date") >= startDate && col("stat_date") <= endDate)

      // Step 2: 数据转换
      val resultDf = filteredDf.select(
        // TODO: 添加需要的字段
      ).orderBy($"stat_date")

      // Step 3: 空值填充
      val filledDf = BaseAdsWriter.fillNullValues(
        resultDf,
        numericFields = AdsConstants.NumericFields.{METRIC_FIELDS},
        stringFields = Seq.empty
      )

      // Step 4: 质量检查
      val minExpectedRows = {min_expected_rows}
      val qualityPass = BaseAdsWriter.quickQualityCheck(
        df = filledDf,
        tableName = TABLE_NAME,
        requiredFields = Seq({required_fields}),
        keyFields = UNIQUE_KEYS,
        minExpectedRows = minExpectedRows
      )

      // Step 5: 写入 MySQL
      val rowCount = BaseAdsWriter.writeToMysqlIdempotent(
        df = filledDf,
        tableName = TABLE_NAME,
        uniqueKeys = UNIQUE_KEYS,
        dateField = "stat_date",
        startDate = startDate,
        endDate = endDate
      )

      metrics.recordJobMetric("ADS", ctx.executionId, s"${TABLE_NAME}_rows", rowCount.toDouble)
      metrics.recordJobMetric("ADS", ctx.executionId, s"${TABLE_NAME}_quality", if (qualityPass) 1.0 else 0.0)

      BuilderResult(success = true, rowCount = rowCount)

    } catch {
      case e: Exception =>
        logger.error(s"构建 $TABLE_NAME 失败: ${e.getMessage}", e)
        BuilderResult(success = false, errorMessage = Some(e.getMessage))
    }
  }
}
```

---

## 三、代码注释规范

### 3.1 类注释

```scala
/**
 * {类名}
 * 功能：{类的功能描述}
 * 依赖：
 *   - 输入：{输入表/数据源}
 *   - 输出：{输出表}
 * 配置项：{关键配置参数}
 * 作者：{作者}
 * 日期：{创建日期}
 * 修改记录：
 *   - {日期}: {修改内容}
 */
```

### 3.2 方法注释

```scala
/**
 * {方法名}
 * @param {参数名} {参数说明}
 * @param {参数名} {参数说明}
 * @return {返回值说明}
 * @throws {异常类型} {异常说明}
 */
```

### 3.3 代码行注释

```scala
// 单行注释使用双斜杠
/* 多行注释使用斜杠星号星币斜杠格式 */

/**
 * 复杂逻辑注释：
 * 1. 步骤一说明
 * 2. 步骤二说明
 * 3. 步骤三说明
 */
```

### 3.4 TODO 标记

```scala
// TODO: 未完成功能 (负责人: xxx, 日期: xxxx-xx-xx)
// TODO(优化): 性能优化点
// TODO(修复): 待修复问题
```

---

## 四、配置参数规范

### 4.1 application.conf 配置

```conf
# 数据源配置
datasource {
  ods {
    path = "/user/hive_local/warehouse/nyc_taxi_ods.db"
    format = "orc"
  }
  dwd {
    path = "/user/hive_local/warehouse/nyc_taxi_dwd.db"
    format = "iceberg"
  }
  dws {
    path = "/user/hive_local/warehouse/nyc_taxi_dws.db"
    format = "iceberg"
  }
}

# ETL 配置
etl {
  # 并行度配置
  parallelism = 4

  # Shuffle 配置
  shuffle {
    partitions = 200
    compress = true
  }

  # Checkpoint 配置
  checkpoint {
    enabled = true
    dir = "hdfs://hadoop102:8020/flink/checkpoints"
    interval = 60000  # 60秒
  }
}

# 质量配置
quality {
  # 空值率阈值
  nullRateThreshold = 0.3

  # 重复率阈值
  duplicateRateThreshold = 0.05

  # 最小预期记录数
  minExpectedRows = 100
}
```

### 4.2 命令行参数规范

```bash
# 标准参数顺序
spark-submit \
  --master yarn \
  --deploy-mode cluster \
  --class com.taxi.etl.ods.GreenOdsLoader \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.sql.adaptive.coalescePartitions.enabled=true \
  ${JAR_PATH} \
  ${EXECUTION_ID} \
  ${START_DATE} \
  ${END_DATE}
```

**参数说明**：

| 参数位置 | 参数名 | 必填 | 说明 |
|----------|--------|------|------|
| 1 | executionId | 否 | 执行唯一标识，默认自动生成 |
| 2 | startDate | 是 | 开始日期 (yyyy-MM-dd) |
| 3 | endDate | 否 | 结束日期，默认等于开始日期 |

---

## 五、日志规范

### 5.1 日志级别使用

| 级别 | 使用场景 |
|------|----------|
| ERROR | 程序异常终止的错误 |
| WARN | 需要关注但不影响流程的警告 |
| INFO | 关键业务流程节点 |
| DEBUG | 开发调试信息 |

### 5.2 日志格式

```scala
// 标准日志格式
logger.info(s"[${ctx.tableName}] 开始执行: ${ctx.startDate} ~ ${ctx.endDate}")
logger.info(s"[${ctx.tableName}] 数据加载完成: ${rowCount} 条记录")
logger.warn(s"[${ctx.tableName}] 数据质量异常: 空值率 ${nullRate}%")
logger.error(s"[${ctx.tableName}] 执行失败: ${e.getMessage}", e)
```

### 5.3 关键日志点

| 阶段 | 日志内容 |
|------|----------|
| 开始 | `[表名] 开始执行: 日期范围` |
| 数据读取 | `[表名] 读取数据: N 条` |
| 数据转换 | `[表名] 转换完成: N 条` |
| 质量校验 | `[表名] 质量检查: 通过/失败` |
| 数据写入 | `[表名] 写入完成: N 条` |
| 结束 | `[表名] 执行完成: 耗时 X ms` |
| 异常 | `[表名] 执行失败: 错误信息` |

---

## 六、异常处理规范

### 6.1 异常分类

| 异常类型 | 处理方式 | 示例 |
|----------|----------|------|
| 数据异常 | 记录日志，写入死信表 | 空值过多、格式错误 |
| 环境异常 | 重试后告警 | 连接超时、资源不足 |
| 业务异常 | 记录日志，跳过 | 缺少依赖数据 |

### 6.2 死信表写入

```scala
def writeToDeadLetter(
                       df: DataFrame,
                       errorReason: String,
                       ctx: JobContext
                     ): Unit = {
  val deadLetterDf = df.withColumn("error_reason", lit(errorReason))
    .withColumn("etl_time", current_timestamp())
    .withColumn("execution_id", lit(ctx.executionId))

  deadLetterDf.write
    .format("jdbc")
    .option("url", jdbcUrl)
    .option("dbtable", "dead_letter_queue")
    .mode("append")
    .save()
}
```

### 6.3 重试机制

```scala
def retry[T](
              fn: => T,
              maxRetries: Int = 3,
              retryInterval: Long = 5000
            ): T = {
  var attempts = 0
  var lastException: Exception = null

  while (attempts < maxRetries) {
    try {
      return fn
    } catch {
      case e: Exception =>
        attempts += 1
        lastException = e
        if (attempts < maxRetries) {
          logger.warn(s"执行失败，${retryInterval}ms 后重试 (${attempts}/${maxRetries})")
          Thread.sleep(retryInterval)
        }
    }
  }

  throw lastException
}
```

---

## 七、质量检查规范

### 7.1 必检项

| 检查项 | 阈值 | 处理方式 |
|--------|------|----------|
| 空值率 | < 30% | 告警 |
| 重复率 | < 5% | 告警 |
| 记录数 | >= 预期 80% | 告警 |
| 字段范围 | 符合业务定义 | 过滤 |

### 7.2 质量报告

```scala
case class QualityReport(
                           tableName: String,
                           totalRows: Long,
                           validRows: Long,
                           invalidRows: Long,
                           nullCounts: Map[String, Long],
                           duplicateCount: Long,
                           qualityScore: Double,
                           checkTime: Timestamp = current_timestamp()
                         )
```

---

## 八、提交规范

### 8.1 Spark 作业提交示例

```bash
# ODS 层加载
spark-submit --master yarn --deploy-mode cluster \
  --class com.taxi.etl.ods.GreenOdsLoader \
  --driver-memory 4g \
  --executor-memory 8g \
  --executor-cores 4 \
  --num-executors 4 \
  --conf spark.sql.adaptive.enabled=true \
  /opt/module/taxi-etl/taxi-etl-4.0-SNAPSHOT-jar-with-dependencies.jar \
  exec_001 2025-01-01 2025-01-31

# ADS 层构建
spark-submit --master yarn --deploy-mode cluster \
  --class com.taxi.etl.ads.AdsLayerBuilder \
  --driver-memory 4g \
  --executor-memory 8g \
  --executor-cores 4 \
  --num-executors 4 \
  /opt/module/taxi-etl/taxi-etl-4.0-SNAPSHOT-jar-with-dependencies.jar \
  exec_002 2025-01-31 2025-01-31
```

### 8.2 资源预估

| 作业类型 | Driver Memory | Executor Memory | Executor Cores | Executors |
|----------|---------------|-----------------|----------------|-----------|
| ODS 加载 | 4g | 8g | 4 | 4 |
| DWD 构建 | 4g | 8g | 4 | 4 |
| DWS 构建 | 4g | 8g | 4 | 4 |
| ADS 构建 | 4g | 8g | 4 | 4 |

---

**文档结束**
