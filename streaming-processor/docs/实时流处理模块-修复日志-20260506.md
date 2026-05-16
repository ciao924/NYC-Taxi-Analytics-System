# 实时模块修复日志 - 2026年05月06日

## 问题修复

### 问题：`InvalidTypesException` - Flink类型推断失败

**错误信息**：
```
org.apache.flink.api.common.functions.InvalidTypesException: The generic type parameters of 'Tuple2' are missing.
```

**问题位置**：`RealtimeMetricsJob.scala` 第 89 行

**根本原因**：Flink 的 Scala API 中，lambda 表达式返回元组类型时，类型提取器无法正确推断泛型参数。

**修复方案**：将 lambda 表达式替换为显式的 `KeySelector` 匿名类。

### 修改内容

**文件**: `src/main/scala/com/taxi/realtime/RealtimeMetricsJob.scala`

**修改前**：
```scala
val orderMetrics = eventTimeStream
  .keyBy( (t: Trip) => (t.pickupZone, t.paymentType) )
```

**修改后**：
```scala
val orderMetrics = eventTimeStream
  .keyBy(new KeySelector[Trip, (String, String)] {
    override def getKey(t: Trip): (String, String) = (t.pickupZone, t.paymentType)
  })
```

> 注：`hotspotTopN` 中的第二个 `keyBy` 已在之前修复，本次修复 `orderMetrics` 中的 `keyBy`。

### 配置优化

**文件**: `src/main/resources/application-metrics.properties`

添加缺失的配置项，消除警告日志：
```
kafka.consumer.group.id=flink-taxi-metrics-group
```

## 编译打包验证

- ✅ Maven编译成功 (`mvn clean compile`)
- ✅ Maven打包成功 (`mvn package -DskipTests`)

## 部署说明

将新 JAR 包上传到 `/opt/flink/jobs/`，然后重新提交作业：

```bash
./flink run -d -p 2 \
  -c com.taxi.realtime.RealtimeMetricsJob \
  /opt/flink/jobs/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config /opt/flink/jobs/application-metrics.properties
```

## 验证

提交后确保 Flink Web UI 中作业状态为 `RUNNING`，且无类型推断相关错误日志。

---

## 问题修复二：实时聚合无输出问题

### 问题描述

作业运行10分钟以上，三个结果表（订单指标、热点区域、费用构成）仍然为空。

**根本原因**：原始代码使用事件时间窗口（`TumblingEventTimeWindows`），基于数据中的 `pickup_datetime`（2025年）分配时间戳和水印。由于当前系统时间为2026年，水印无法推进到窗口结束时间，导致窗口永不触发，聚合结果无法写入 MySQL。

**解决方案**：全部改用处理时间窗口（`TumblingProcessingTimeWindows`），基于数据到达算子的系统时间触发窗口。

### 修改内容

**文件**: `src/main/scala/com/taxi/realtime/RealtimeMetricsJob.scala`

**1. 删除事件时间水印策略**：
```scala
// 删除以下代码
val watermarkStrategy = WatermarkStrategy
  .forBoundedOutOfOrderness[Trip](Duration.ofMinutes(2))
  .withTimestampAssigner(...)

val eventTimeStream = tripStream.assignTimestampsAndWatermarks(watermarkStrategy)
```

**2. 将所有窗口类型改为处理时间窗口**：

| 聚合算子 | 修改前 | 修改后 |
|----------|--------|--------|
| orderMetrics | `TumblingEventTimeWindows.of(Time.minutes(5))` | `TumblingProcessingTimeWindows.of(Time.minutes(5))` |
| hotspotTopN | `TumblingEventTimeWindows.of(Time.minutes(5))` | `TumblingProcessingTimeWindows.of(Time.minutes(5))` |
| feeComposition | `TumblingEventTimeWindows.of(Time.minutes(5))` | `TumblingProcessingTimeWindows.of(Time.minutes(5))` |

**3. 调整热点TopN定时器**：
- 将 `HotspotTopNEventTimeProcessFunction` 重命名为 `HotspotTopNProcessFunction`
- 将 `registerEventTimeTimer` 改为 `registerProcessingTimeTimer`

**4. 更新导入语句**：
- 删除 `import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}`
- 删除 `import java.time.Duration`
- 添加 `import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows`

### 时间语义变化

| 维度 | 事件时间窗口 | 处理时间窗口 |
|------|--------------|--------------|
| 触发依据 | 数据中的业务时间戳 | 数据到达的系统时间 |
| 乱序处理 | 支持迟到数据修正 | 不处理乱序 |
| 适用场景 | 对时间精度要求高 | 对时间精度要求不高 |
| 窗口边界 | 整5分钟边界（基于业务时间） | 整5分钟边界（基于系统时间） |

### 编译打包验证（更新）

- ✅ Maven编译成功 (`mvn clean compile`)
- ✅ Maven打包成功 (`mvn package -DskipTests`)

### 部署说明

将新 JAR 包上传到 `/opt/flink/jobs/`，然后重新提交作业：

```bash
# 停止旧作业（如有）
./flink stop <job-id>

# 启动新作业
./flink run -d -p 2 \
  -c com.taxi.realtime.RealtimeMetricsJob \
  /opt/flink/jobs/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config /opt/flink/jobs/application-metrics.properties
```

### 验证

1. 作业提交后状态为 `RUNNING`
2. 等待5分钟后检查MySQL指标表是否有数据写入
3. 确认三个结果表（`realtime_order_metrics`、`realtime_hotspot_topn`、`realtime_fee_composition`）均有数据

---

## 问题修复三：MySQL写入失败（rank关键字冲突）

### 问题描述

作业运行时出现 SQL 语法错误，导致 checkpoint 失败：

```
SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'rank) VALUES ...'
```

**根本原因**：`realtime_hotspot_topn` 表的 `rank` 字段是 MySQL 保留关键字，在 INSERT 语句中未用反引号转义，导致 SQL 解析失败。

### 修改内容

**文件**: `src/main/scala/com/taxi/realtime/sink/MysqlSinkFactory.scala`

**修改前**：
```scala
val sql =
  """INSERT INTO realtime_hotspot_topn (window_start, window_end, zone, cnt, rank)
    |VALUES (?, ?, ?, ?, ?)
    |ON DUPLICATE KEY UPDATE
    |  cnt = VALUES(cnt),
    |  rank = VALUES(rank)
    |""".stripMargin
```

**修改后**：
```scala
val sql =
  """INSERT INTO realtime_hotspot_topn (window_start, window_end, zone, cnt, `rank`)
    |VALUES (?, ?, ?, ?, ?)
    |ON DUPLICATE KEY UPDATE
    |  cnt = VALUES(cnt),
    |  `rank` = VALUES(`rank`)
    |""".stripMargin
```

> **注意**：`ON DUPLICATE KEY UPDATE` 中的 `rank` 同样需要反引号转义。

---

## 问题修复四：多Topic消费验证

### 问题描述

配置文件中已配置 `kafka.topics=taxi_trip_green,taxi_trip_yellow`，但作业可能只消费了一个主题。

### 解决方案

**文件**: `src/main/scala/com/taxi/realtime/RealtimeMetricsJob.scala`

在 `main` 方法中添加配置验证日志：

```scala
val topics = ConfigManager.getKafkaTopics
println("=== Topics from config: " + topics.mkString(","))
LoggerUtil.info("=== Topics from config: " + topics.mkString(","))
```

### 验证步骤

1. 启动作业后查看 TaskManager 日志，确认输出包含两个 topic：`taxi_trip_green,taxi_trip_yellow`
2. 检查 Flink UI 中 Kafka Source 的 metrics（`records-in`）确认两个主题都有数据流入
3. 查看死信队列是否有大量 yellow 解析错误

### 编译打包验证（最终）

- ✅ Maven编译成功 (`mvn clean compile`)
- ✅ Maven打包成功 (`mvn package -DskipTests`)

### 部署说明

将新 JAR 包上传到 `/opt/flink/jobs/`，然后重新提交作业：

```bash
# 停止旧作业（如有）
./flink stop <job-id>

# 启动新作业
./flink run -d -p 2 \
  -c com.taxi.realtime.RealtimeMetricsJob \
  /opt/flink/jobs/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config /opt/flink/jobs/application-metrics.properties
```

### 验证

1. 作业提交后状态为 `RUNNING`
2. 检查日志确认配置正确加载（包含两个 topic）
3. 等待5分钟后检查MySQL三个结果表均有数据写入
4. 确认 checkpoint 正常完成

---

## 问题修复五：更换 Consumer Group ID（强制从头消费）

### 问题描述

作业只消费了 `taxi_trip_green` 一个主题的数据，`taxi_trip_yellow` 没有被消费。

### 解决方案

修改 `application-metrics.properties` 中的 `kafka.consumer.group.id`，使用新的消费组确保从头开始消费：

**文件**: `src/main/resources/application-metrics.properties`

**修改前**：
```properties
kafka.consumer.group.id=flink-taxi-metrics-group
```

**修改后**：
```properties
kafka.consumer.group.id=flink-taxi-metrics-group-v2
```

### 原因说明

- 原消费组 `flink-taxi-metrics-group` 可能有已提交的 offset，导致新启动的作业不会从最早消息开始消费
- 创建新的消费组 `flink-taxi-metrics-group-v2` 后，由于没有已提交的 offset，会严格按照 `kafka.consumer.auto.offset.reset=earliest` 配置从两个主题的最早消息开始消费
- 这样可以确保不会遗漏 yellow 主题的历史数据

### 编译打包验证

- ✅ Maven编译成功 (`mvn clean compile`)
- ✅ Maven打包成功 (`mvn package -DskipTests`)

### 部署说明

将新 JAR 包上传到 `/opt/flink/jobs/`，然后重新提交作业：

```bash
# 停止旧作业
./flink stop <job-id>

# 启动新作业（使用新的 consumer group）
./flink run -d -p 2 \
  -c com.taxi.realtime.RealtimeMetricsJob \
  /opt/flink/jobs/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config /opt/flink/jobs/application-metrics.properties
```

### 验证

1. 作业提交后状态为 `RUNNING`
2. 检查日志确认输出：`=== Topics from config: taxi_trip_green,taxi_trip_yellow`
3. 在 Flink Web UI 中确认 Kafka Source 的 `records-in` metrics 显示两个主题都有数据流入
4. 等待5分钟后检查MySQL三个结果表均有数据写入
5. 确认 checkpoint 正常完成

---

## 问题修复六：修复 group.id 硬编码问题

### 问题描述

作业使用硬编码的 group.id (`flink-taxi-metrics-group`)，导致配置文件中设置的 `kafka.consumer.group.id=flink-taxi-metrics-group-v2` 从未生效。因此，作业始终使用旧的消费组，该消费组在 `taxi_trip_yellow` 主题上的 offset 可能已经位于最新位置，从而表现为没有消费 yellow 数据。

### 修改内容

**文件**: `src/main/scala/com/taxi/realtime/RealtimeMetricsJob.scala`

**修改前**：
```scala
val source = env.fromSource(
  KafkaSourceBuilder.build("flink-taxi-metrics-group"),
  WatermarkStrategy.noWatermarks(),
  "Kafka Source"
)
```

**修改后**：
```scala
val source = env.fromSource(
  KafkaSourceBuilder.build(ConfigManager.getKafkaConsumerGroupId),
  WatermarkStrategy.noWatermarks(),
  "Kafka Source"
)
```

### 原因说明

- `RealtimeMetricsJob` 中硬编码了 group.id，导致配置文件中的 `kafka.consumer.group.id` 配置无法生效
- 修改后，group.id 将从配置文件中读取，确保新的消费组 `flink-taxi-metrics-group-v2` 能够生效
- 新消费组没有已提交的 offset，会严格按照 `earliest` 策略从两个主题的最早消息开始消费

### 编译打包验证

- ✅ Maven编译成功 (`mvn clean compile`)
- ✅ Maven打包成功 (`mvn package -DskipTests`)

### 部署说明

将新 JAR 包上传到 `/opt/flink/jobs/`，然后重新提交作业：

```bash
# 停止旧作业
./flink stop <job-id>

# 启动新作业（使用配置文件中的 group.id）
./flink run -d -p 2 \
  -c com.taxi.realtime.RealtimeMetricsJob \
  /opt/flink/jobs/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config /opt/flink/jobs/application-metrics.properties
```

### 验证

1. 作业提交后状态为 `RUNNING`
2. 检查日志确认配置文件中的 group.id 已生效
3. 在 Flink Web UI 中确认 Kafka Source 的 `records-in` metrics 显示两个主题都有数据流入
4. 等待5分钟后检查MySQL三个结果表均有数据写入
5. 确认 checkpoint 正常完成