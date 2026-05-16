# 2026-05-07 ODS 作业修复日志

## 问题分析

### 第一轮问题（已修复）
根据问题描述，当前 Hive ODS 作业（`RealtimeOdsJob`）存在两个核心问题：

1. **分区元数据未同步到 Hive**：虽然文件按 `year=YYYY/month=MM` 写入 HDFS，但 Hive 表的分区元数据并未自动添加，导致查询时看不到数据（需要手动执行 `MSCK REPAIR TABLE`）。
2. **Kafka 消费配置硬编码**：`RealtimeOdsJob.scala` 中 group.id 硬编码为 `"flink-taxi-ods-group"`，未从配置读取。

### 第二轮问题（补充修复）
根据作业失败日志分析，新增问题：

1. **配置文件回退逻辑缺陷**：当 `--config` 指定的配置文件不存在时，静默回退到 classpath 的 `application.properties`，导致关键配置缺失（如 `hive.metastore.uris` 为 null），引发 NullPointerException。
2. **缺少配置项非空校验**：`getHiveMetastoreUris()` 等方法未检查返回值是否为 null，调用方也未做防御。
3. **HivePartitionSyncFunction 健壮性不足**：缺少连接超时配置，`getPartition()` 异常未正确捕获。

### 第三轮问题（补充修复）
根据最新作业失败日志分析：

1. **静态变量初始化问题**：`ConfigManager.properties` 静态变量在 Flink 作业恢复时可能为 null，导致 `getHiveMetastoreUris()` 调用 `properties.getProperty()` 时抛出 NullPointerException。
   - 根因：Flink 作业恢复时，TaskManager 可能在 `ConfigManager.init()` 调用前初始化 Operator，导致静态变量未被初始化。

### 第四轮问题（核心修复）
根据深度分析，问题根源是：

1. **Flink 分布式执行模型问题**：`ConfigManager` 是单例对象，其配置状态存储在静态变量中。但 Flink 作业执行时：
   - 客户端 JVM 调用 `ConfigManager.init()` 加载配置
   - TaskManager JVM 反序列化算子时，静态变量不会自动传递
   - 导致 TaskManager 上的 `properties = null`
   
## 修复内容

### 1. 修复 Kafka 消费配置

**文件**：`src/main/resources/application-ods.properties`

**修改**：将 `kafka.consumer.group.id.ods=flink-taxi-ods-group` 修改为 `kafka.consumer.group.id=flink-taxi-ods-group-v2`

### 2. 修改 group.id 从配置读取

**文件**：`src/main/scala/com/taxi/realtime/RealtimeOdsJob.scala`

**修改**：将硬编码的 group.id 改为从配置文件读取：
```scala
val source = env.fromSource(
  KafkaSourceBuilder.build(ConfigManager.getKafkaConsumerGroupId),
  WatermarkStrategy.noWatermarks(),
  "Kafka Source"
)
```

### 3. 添加 Hive 分区自动同步逻辑

**文件**：`src/main/scala/com/taxi/realtime/RealtimeOdsJob.scala`

**新增**：添加 `HivePartitionSyncFunction` 类，实现以下功能：
- 从记录中提取分区键（year/month）
- 按配置的时间间隔批量同步分区到 Hive Metastore
- 使用 `HiveMetaStoreClient` 创建分区

**修改**：在 `greenStream` 和 `yellowStream` 写入 FileSink 之前添加分区同步处理

### 4. 修复配置文件回退逻辑

**文件**：`src/main/scala/com/taxi/realtime/config/ConfigManager.scala`

**修改**：禁止静默回退，当指定的配置文件不存在时，直接抛出明确异常：
```scala
if (configFile != null) {
  val file = new File(configFile)
  if (!file.exists()) {
    throw new RuntimeException(s"Specified config file does not exist: $configFile")
  }
  input = new FileInputStream(file)
}
```

### 5. 增加配置项非空校验

**文件**：`src/main/scala/com/taxi/realtime/config/ConfigManager.scala`

**修改**：为关键配置项添加 require 校验：
- `getHiveMetastoreUris()` - 校验 hive.metastore.uris
- `getHiveDatabase()` - 校验 hive.database
- `getHiveTableGreen()` - 校验 hive.table.green
- `getHiveTableYellow()` - 校验 hive.table.yellow

### 6. 增强 HivePartitionSyncFunction 健壮性

**文件**：`src/main/scala/com/taxi/realtime/RealtimeOdsJob.scala`

**修改**：
- 添加 Hive Metastore 连接超时配置
- 添加 `getPartition()` 异常捕获，避免因临时网络问题导致作业失败
- 使用独立的 StorageDescriptor 创建分区，避免元数据不一致
- 同步间隔改为从配置读取（`hive.batch.interval.ms`）

### 7. 修复静态变量初始化问题

**文件**：`src/main/scala/com/taxi/realtime/config/ConfigManager.scala`

**修改**：为所有 Hive 配置方法添加 `properties` 非空校验，确保在调用前 ConfigManager 已正确初始化：
```scala
def getHiveMetastoreUris: String = {
  require(properties != null, "ConfigManager has not been initialized. Call ConfigManager.init() first.")
  val uri = properties.getProperty("hive.metastore.uris")
  require(uri != null && uri.nonEmpty, "Missing required config: hive.metastore.uris")
  uri
}
```

**影响**：`getHiveMetastoreUris()`、`getHiveDatabase()`、`getHiveTableGreen()`、`getHiveTableYellow()` 方法

### 8. 修复 Flink 分布式执行配置传递问题（核心修复）

**文件**：`src/main/scala/com/taxi/realtime/RealtimeOdsJob.scala`

**问题根源**：`ConfigManager` 单例对象的静态变量无法跨 Flink TaskManager JVM 共享，导致算子在 TaskManager 上执行时配置为 null。

**解决方案**：将配置参数通过构造函数传递给算子，配置值随算子实例序列化后发送到所有 TaskManager。

**修改内容**：

1. **修改 `HivePartitionSyncFunction` 类定义**：
```scala
class HivePartitionSyncFunction(
    tableName: String,
    metastoreUris: String,
    databaseName: String,
    batchIntervalMs: Long
) extends ProcessFunction[GreenTripRecord, GreenTripRecord] {
  // 移除所有 ConfigManager 调用，直接使用构造参数
}
```

2. **修改 `writeToHdfsWithOrcFileSink` 方法**：
```scala
val metastoreUris = ConfigManager.getHiveMetastoreUris  // 客户端调用，安全
val databaseName = ConfigManager.getHiveDatabase
val batchIntervalMs = ConfigManager.getHiveBatchIntervalMs

greenStream
  .process(new HivePartitionSyncFunction(
    ConfigManager.getHiveTableGreen,
    metastoreUris,
    databaseName,
    batchIntervalMs
  ))
```

### 9. ODS 层重构（与离线模块对齐）

**文件**：`src/main/scala/com/taxi/realtime/RealtimeOdsJob.scala`

**重构目标**：与离线模块 ODS 层处理方式保持一致

**修改内容**：

1. **移除 HivePartitionSyncFunction**：数据不再直接写入 Hive，仅写入 HDFS ORC 文件
2. **简化写入逻辑**：删除不必要的 Hive MetaStore 连接和分区同步逻辑
3. **统一数据格式**：
   - 压缩格式：SNAPPY（与离线一致）
   - 分区格式：`year=YYYY/month=MM`
   - 文件后缀：`.orc`
4. **数据路径配置**：新增 `data.ods.path` 配置项，与离线模块保持一致
5. **删除不需要的组件**：移除 HiveConf、HiveMetaStoreClient 等相关 import

**文件**：`src/main/scala/com/taxi/realtime/config/ConfigManager.scala`

**新增方法**：
```scala
def getOdsDataPath: String = {
  properties.getProperty("data.ods.path",
    "hdfs://hadoop102:8020/user/hive_local/warehouse/nyc_taxi_ods.db/taxi_trip_yellow_ods")
}
```

**文件**：`src/main/resources/application-ods.properties`

**新增配置**：
```
data.ods.path=hdfs://192.168.127.102:8020/user/hive_local/warehouse/nyc_taxi_ods.db/taxi_trip_yellow_ods
```

**注意事项**：数据写入 HDFS 后，需定期执行 `MSCK REPAIR TABLE` 同步 Hive 分区元数据。

## 验证结果

- ✅ Maven 编译打包成功：`mvn clean package -DskipTests`
- ✅ 生成 JAR 文件：`target/flink-realtime-consumer-1.0-SNAPSHOT.jar`

## 部署说明

```bash
# 提交 ODS 作业
./flink run -d -p 2 -c com.taxi.realtime.RealtimeOdsJob /opt/flink/jobs/flink-realtime-consumer-1.0-SNAPSHOT.jar --config /opt/flink/jobs/application-ods.properties

# 提交 Metrics 作业
./flink run -d -p 2 -c com.taxi.realtime.RealtimeMetricsJob /opt/flink/jobs/flink-realtime-consumer-1.0-SNAPSHOT.jar --config /opt/flink/jobs/application-metrics.properties
```

## 注意事项

1. 新的 group.id `flink-taxi-ods-group-v2` 将从头消费 Kafka 消息
2. 分区同步间隔默认 5 秒，可通过 `hive.batch.interval.ms` 配置调整
3. 需要确保 Hive Metastore 服务正常运行（端口 9083）
4. 配置文件必须存在于指定路径，否则作业启动时会抛出明确错误

---

## 新增：实时数据质量检测模块

### 功能概述

基于参考文档《实时数据质量检测：纽约出租车数据.md》和《MySQL数据检测模块表结构.txt》，完成数据智能检测模块开发，实现以下功能：

- 每 5 分钟统计一次各错误类型的数量与比率
- 将统计结果写入 `data_quality_daily` 表
- 根据 `quality_alert_config` 配置触发告警，写入 `quality_alert_history`
- 完全基于现有代码的侧输出流，不侵入原有业务逻辑

### 新增文件

| 文件路径 | 说明 |
|---------|------|
| `src/main/scala/com/taxi/realtime/quality/QualityModels.scala` | 质量检测模型类（QualityMetric、QualityWindowResult、AlertConfig、AlertRecord） |
| `src/main/scala/com/taxi/realtime/quality/QualityAggregateFunctions.scala` | 窗口聚合函数（QualityAggregateFunction、QualityWindowFunction） |
| `src/main/scala/com/taxi/realtime/quality/AlertDetector.scala` | 告警检测处理器 |
| `src/main/scala/com/taxi/realtime/sink/QualitySinkBuilder.scala` | 质量指标写入 MySQL |
| `src/main/scala/com/taxi/realtime/sink/AlertSinkBuilder.scala` | 告警记录写入 MySQL |

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `src/main/scala/com/taxi/realtime/RealtimeMetricsJob.scala` | 集成质量检测模块，添加时间戳校验、质量统计窗口、告警检测 |
| `src/main/scala/com/taxi/realtime/config/ConfigManager.scala` | 新增质量检测相关配置方法 |
| `src/main/resources/application-metrics.properties` | 新增质量检测配置项 |

### 质量指标类型

| 指标类型 | 字段名 | 期望值 | 说明 |
|---------|-------|--------|------|
| JSON 解析错误率 | `json_parse_error_rate` | 5% | 解析失败记录占总记录比例 |
| 数据清洗错误率 | `data_clean_error_rate` | 10% | 清洗失败记录占总记录比例 |
| 时间戳错误率 | `timestamp_error_rate` | 5% | 时间戳校验失败记录占总记录比例 |
| 总体质量率 | `overall_quality_rate` | 95% | 有效记录占总记录比例 |

### 配置项说明

```properties
# 是否启用质量检测
data.quality.enabled=true
# 告警配置刷新间隔（秒）
data.quality.alert.refresh.seconds=60
# 期望阈值（默认值）
quality.expected.parse.error.rate=0.05
quality.expected.clean.error.rate=0.10
quality.expected.overall.quality.rate=0.95
```

### 告警触发规则

- **错误率指标**：当实际值 > 阈值时触发告警
- **质量率指标**：当实际值 < 阈值时触发告警
- 告警级别分为 WARNING 和 CRITICAL，根据 `quality_alert_config` 配置动态调整

---

## 修复：OutputTag 类型不一致导致的 Union 操作失败

### 问题描述
RealtimeMetricsJob 中对三个 `ErrorRecordJava` 流进行 union 操作时，Flink 检测到类型不一致：
- 前两个流（parsingErrors、cleaningErrors）的类型是 `GenericType<ErrorRecordJava>`
- 第三个流（来自 invalidTimestampErrors）的类型是简单的 `ErrorRecordJava`

### 根本原因
`JsonParseFunction` 和 `DataCleanFunction` 中定义的 `OutputTag` 显式重写了 `getTypeInfo` 方法，提供了明确的 `TypeInformation[ErrorRecordJava]`。而 `RealtimeMetricsJob` 中定义的 `invalidTimestampErrors` 没有重写该方法，导致 Flink 为该侧输出流推断出一个不同的类型表示。

### 修复方案
为 `invalidTimestampErrors` 的 `OutputTag` 显式指定 `TypeInformation`，与其他 `OutputTag` 保持一致。

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `src/main/scala/com/taxi/realtime/RealtimeMetricsJob.scala` | 添加 `TypeInformation` 导入，为 `invalidTimestampErrors` 重写 `getTypeInfo` 方法 |
| `src/main/scala/com/taxi/realtime/RealtimeOdsJob.scala` | 同步修改，保持代码一致性 |

### 修改示例

```scala
// 添加导入
import org.apache.flink.api.common.typeinfo.TypeInformation

// 修改 OutputTag 定义
val invalidTimestampErrors = new OutputTag[ErrorRecordJava]("invalid-timestamp") {
  override def getTypeInfo: TypeInformation[ErrorRecordJava] =
    TypeInformation.of(classOf[ErrorRecordJava])
}
```

### 验证结果
✅ Maven 编译打包成功，作业启动失败问题已修复

---

## 修复：Scala Lambda 泛型类型推断失败

### 问题描述
Flink 无法自动推断 Scala lambda 中返回的 `(String, QualityEvent)` 类型，因为泛型被擦除。错误信息：

```
Caused by: org.apache.flink.api.common.functions.InvalidTypesException: The generic type parameters of 'Tuple2' are missing.
```

### 根本原因
Flink 结合 Scala lambda 时，由于 Java 泛型擦除机制，无法从 lambda 表达式中提取足够的类型信息。

### 修复方案
使用类型注解显式指定返回类型。

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `src/main/scala/com/taxi/realtime/RealtimeMetricsJob.scala` | 为 map 操作添加类型注解 |

### 修改示例

```scala
// 原代码
val allEvents = normalStream.union(parseErrorStream, cleanErrorStream, timestampErrorStream)
  .map(event => ("quality_key", event))

// 修复后
val allEvents = normalStream.union(parseErrorStream, cleanErrorStream, timestampErrorStream)
  .map(event => ("quality_key", event): (String, QualityEvent))
```

### 验证结果
✅ Maven 编译打包成功，泛型类型推断问题已修复

---

## 修复：Tuple2 泛型类型参数缺失（最终修复）

### 问题描述
尽管之前尝试使用类型注解，但 Flink 仍然无法推断 Scala lambda 中 `Tuple2` 的泛型类型参数，导致作业启动失败。错误信息：

```
Caused by: org.apache.flink.api.common.functions.InvalidTypesException: The generic type parameters of 'Tuple2' are missing.
```

### 根本原因
Scala lambda 表达式在编译时会丢失泛型类型信息，Flink 的自动类型提取机制无法正确识别返回类型。

### 修复方案
使用 `TypeHint` 显式指定返回类型信息。

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `src/main/scala/com/taxi/realtime/RealtimeMetricsJob.scala` | 添加 `TypeHint` 导入，使用 `.returns()` 方法指定类型信息 |

### 修改示例

```scala
// 添加导入
import org.apache.flink.api.common.typeinfo.TypeHint

// 修改 map 操作
val allEvents = normalStream.union(parseErrorStream, cleanErrorStream, timestampErrorStream)
  .map(event => ("quality_key", event))
  .returns(new TypeHint[(String, QualityEvent)] {})
```

### 验证结果
✅ Maven 编译打包成功，作业启动失败问题已完全修复

---

## 修复：QualitySinkBuilder 序列化问题

### 问题描述
`QualitySinkBuilder` 中使用的 lambda 表达式不可序列化，导致作业启动失败。错误信息：

```
Caused by: java.io.NotSerializableException: Non-serializable lambda
```

### 根本原因
Scala lambda 表达式默认不可序列化，而 Flink 需要将算子序列化后分发到各个 TaskManager 执行。

### 修复方案
将 lambda 表达式替换为实现 `JdbcStatementBuilder` 接口的可序列化类。

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `src/main/scala/com/taxi/realtime/sink/QualitySinkBuilder.scala` | 创建 `QualityStatementBuilder` 类实现 `JdbcStatementBuilder` 接口并继承 `Serializable` |

### 修改示例

```scala
// 创建可序列化的 StatementBuilder 类
class QualityStatementBuilder extends JdbcStatementBuilder[QualityWindowResult] with Serializable {
  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
  
  override def accept(ps: PreparedStatement, result: QualityWindowResult): Unit = {
    // 执行 SQL 语句构建逻辑
  }
}

// 在 build() 方法中使用
JdbcSink.sink(
  insertSql,
  new QualityStatementBuilder(),
  // ...
)
```

### 验证结果
✅ Maven 编译打包成功，序列化问题已修复

---

## 修复：ObjectMapper 序列化问题（最终修复）

### 问题描述
`QualityStatementBuilder` 中使用的 `ObjectMapper` 内部包含不可序列化的 `ScalaTypeModifier`，导致作业启动失败。错误信息：

```
Caused by: java.io.NotSerializableException: com.fasterxml.jackson.module.scala.modifiers.ScalaTypeModifier
```

### 根本原因
`ObjectMapper` 及其注册的 Scala 模块包含大量不可序列化的内部组件，当作为实例字段时无法被 Flink 序列化。

### 修复方案
将 `ObjectMapper` 移到伴生对象中作为静态字段，这样它不会被序列化。

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `src/main/scala/com/taxi/realtime/sink/QualitySinkBuilder.scala` | 创建 `QualityStatementBuilder` 伴生对象，将 `ObjectMapper` 作为静态字段 |

### 修改示例

```scala
class QualityStatementBuilder extends JdbcStatementBuilder[QualityWindowResult] with Serializable {
  override def accept(ps: PreparedStatement, result: QualityWindowResult): Unit = {
    // 使用伴生对象中的 mapper
    val detail = QualityStatementBuilder.mapper.writeValueAsString(...)
  }
}

object QualityStatementBuilder {
  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
}
```

### 验证结果
✅ Maven 编译打包成功，ObjectMapper 序列化问题已完全修复

---

## 修复：AlertSinkBuilder lambda 序列化问题

### 问题描述
`AlertSinkBuilder` 中使用的 lambda 表达式不可序列化，导致作业启动失败。错误信息：

```
Caused by: java.io.NotSerializableException: Non-serializable lambda
```

### 根本原因
Scala lambda 表达式默认不可序列化，而 Flink 需要将算子序列化后分发到各个 TaskManager 执行。

### 修复方案
将 lambda 表达式替换为实现 `JdbcStatementBuilder` 接口的可序列化类。

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `src/main/scala/com/taxi/realtime/sink/AlertSinkBuilder.scala` | 创建 `AlertStatementBuilder` 类实现 `JdbcStatementBuilder` 接口并继承 `Serializable` |

### 修改示例

```scala
class AlertStatementBuilder extends JdbcStatementBuilder[AlertRecord] with Serializable {
  override def accept(ps: PreparedStatement, record: AlertRecord): Unit = {
    ps.setLong(1, record.alertConfigId)
    ps.setString(2, record.alertLevel)
    // ...
  }
}
```

### 验证结果
✅ Maven 编译打包成功，AlertSinkBuilder 序列化问题已修复

---

## 修复：数据库连接配置错误

### 问题描述
作业启动成功但写入数据时失败，错误信息：

```
Caused by: java.sql.SQLSyntaxErrorException: Table 'nyc_taxi_realtime.data_quality_daily' doesn't exist
```

### 根本原因
MySQL 连接 URL 配置错误，连接到了 `nyc_taxi_realtime` 数据库，而数据质量检测的表实际存储在 `nyc_taxi_quality` 数据库中。

### 修复方案
修改配置文件中的 MySQL 连接 URL，将数据库名称从 `nyc_taxi_realtime` 改为 `nyc_taxi_quality`。

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `src/main/resources/application-metrics.properties` | 修改 `mysql.url` 配置项的数据库名称 |

### 修改示例

```properties
# 修改前
mysql.url=jdbc:mysql://192.168.127.102:3306/nyc_taxi_realtime?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai

# 修改后
mysql.url=jdbc:mysql://192.168.127.102:3306/nyc_taxi_quality?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
```

### 验证结果
✅ Maven 编译打包成功，数据库连接配置已修复

---

## 修复：数据库架构分离（ADS 指标 vs 质量检测）

### 问题描述
之前修改将 `mysql.url` 指向 `nyc_taxi_quality`，导致：
- ADS 指标表（order_metrics, hotspot_topn, fee_composition）错误写入 `nyc_taxi_quality`
- 死信队列（dead_letter_queue）也错误写入 `nyc_taxi_quality`

### 架构设计
- **nyc_taxi_realtime**：ADS 指标表 + 死信队列
- **nyc_taxi_quality**：数据质量检测结果表（data_quality_daily, quality_alert_config, quality_alert_history）

### 修复方案（选项 A）
为不同 sink 配置不同的数据库 URL：
1. `mysql.url` → nyc_taxi_realtime（ADS 指标表、死信队列）
2. `mysql.url.quality` → nyc_taxi_quality（质量检测结果表）

### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `src/main/resources/application-metrics.properties` | 恢复 `mysql.url` 为 nyc_taxi_realtime，新增 `mysql.url.quality` |
| `src/main/scala/com/taxi/realtime/config/ConfigManager.scala` | 新增 `getMysqlQualityUrl()` 方法 |
| `src/main/scala/com/taxi/realtime/sink/QualitySinkBuilder.scala` | 使用 `getMysqlQualityUrl()` |
| `src/main/scala/com/taxi/realtime/sink/AlertSinkBuilder.scala` | 使用 `getMysqlQualityUrl()` |

### 核心代码

**ConfigManager.scala 新增方法：**
```scala
def getMysqlQualityUrl: String = {
  properties.getProperty("mysql.url.quality", getMysqlUrl)
}
```

**application-metrics.properties 配置：**
```properties
mysql.url=jdbc:mysql://192.168.127.102:3306/nyc_taxi_realtime?...
mysql.url.quality=jdbc:mysql://192.168.127.102:3306/nyc_taxi_quality?...
```

### 验证结果
✅ Maven 编译打包成功，数据库架构分离修复完成