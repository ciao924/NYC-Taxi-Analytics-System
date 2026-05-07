# 最终方案：分两个 Job 实现实时数仓（解耦生产级）

根据您提供的分析报告（指出单 Job 存在背压耦合、ODS/ADS 语义污染、窗口一致性问题等隐患），我**完全同意拆分为两个独立 Flink Job**。这样既能满足核心需求，又能避免生产环境中的相互影响。

以下给出**分 Job 后的任务清单**，每个 Job 职责明确、边界清晰，且完全覆盖您的原始需求：

- **Job A**：实时指标 → MySQL（3 张 ADS 表）

- **Job B**：原始明细 → Hive ODS（绿 / 黄表，按事件时间分区）

---

# ✅ 一、整体架构（双 Job 解耦）

```plaintext
Kafka (taxi_trip_green, taxi_trip_yellow)
    ├─→ [Job A] 实时指标计算 → MySQL (realtime_order_metrics, realtime_hotspot_topn, realtime_fee_composition)
    └─→ [Job B] 实时明细落地 → Hive ODS (nyc_taxi_ods.taxi_trip_green_ods, taxi_trip_yellow_ods)
```

**关键优势**：

- ✅ 任意一个 Sink 慢 / 故障，不影响另一个 Job。

- ✅ 可以独立扩缩容、独立重启、独立升级。

- ✅ ODS 和 ADS 使用**独立的清洗逻辑**（ODS 轻清洗，ADS 强清洗），符合数仓规范。

- ✅ 避免单 Job 中多个窗口聚合互相争抢资源。

---

# 🔴 二、Job A：实时指标 → MySQL（服务大屏）

### 模块边界

|模块|职责|输入|输出|
|---|---|---|---|
|Source|消费 Kafka，JSON 解析|Kafka bytes|`DataStream\[GreenTripRecord\]`|
|Clean \(强\)|严格数据质量过滤|`GreenTripRecord`|`DataStream\[CleanTrip\]`|
|Aggregation|三个独立窗口聚合|`CleanTrip`|3 个指标流|
|MySQL Sink|幂等写入|指标流|MySQL 3 张表|

### 任务清单

#### 任务 A\.1：构建独立 Job 入口 `RealtimeMetricsJob\.scala`

**推荐结构**：

```scala
object RealtimeMetricsJob {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    // 配置 RocksDB, Checkpoint（参考之前）
    val source = KafkaSourceBuilder.build()
    val parsed = env.fromSource(source, ...).process(new JsonParseFunction)
    val cleaned = parsed.filter(record => DataCleaner.clean(Some(record)).isDefined)
      .assignTimestampsAndWatermarks(watermarkStrategy)
    
    // 三个独立聚合
    val orderMetrics = cleaned... // 同之前
    val hotspot = cleaned...      // 修复窗口一致性问题
    val fee = cleaned...          // 使用正确的 ProcessWindowFunction
    
    // 三个 Sink
    orderMetrics.addSink(MysqlSinkFactory.buildOrderMetricsSink())
    hotspot.addSink(MysqlSinkFactory.buildHotspotSink())
    fee.addSink(MysqlSinkFactory.buildFeeCompositionSink())
    
    env.execute("RealtimeMetricsJob")
  }
}
```

#### 任务 A\.2：修复热点 TopN 窗口一致性问题

**不推荐** `windowAll` 嵌套，改用 **KeyedProcessFunction \+ Timer** 或以下方案：

```scala
val zoneCounts = cleaned
  .keyBy(_.puLocationId.getOrElse(0))
  .window(TumblingEventTimeWindows.of(Time.minutes(5)))
  .aggregate(new CountAggregateFunction)
  .keyBy(_ => 0)  // 虚拟 key 聚到一个 Task
  .process(new TopNProcessFunction(10))
```

或在 `windowAll` 中直接处理，但确保输入是同一个窗口的聚合结果（通过先 `window` 再 `windowAll` 连续调用，实际上第二个窗口会等待第一个窗口完成，边界一致 —— 但为了保险，建议用 `process`）。

#### 任务 A\.3：修复 FeeComposition 窗口元数据获取

**必须使用 ****`ProcessWindowFunction`**：

```scala
feeComposition = cleaned
  .map(record => (record.paymentType.getOrElse(0).toString, record.totalAmount.getOrElse(0.0)))
  .keyBy(_._1)
  .window(TumblingEventTimeWindows.of(Time.minutes(5)))
  .aggregate(new FeeAggregateFunction, new FeeWindowFunction)

class FeeWindowFunction extends ProcessWindowFunction[Double, FeeComposition, String, TimeWindow] {
  override def process(key: String, ctx: Context, elements: Iterable[Double], out: Collector[FeeComposition]): Unit = {
    val total = elements.head
    out.collect(FeeComposition(ctx.window.getStart, ctx.window.getEnd, key, total))
  }
}
```

#### 任务 A\.4：配置 MySQL Sink 幂等写入（已存在，验证主键）

确保三张表主键与 `ON DUPLICATE KEY UPDATE` 匹配（参照 `mysql\.txt`）。

---

# 🔴 三、Job B：实时明细 → Hive ODS（服务离线数仓）

### 模块边界

|模块|职责|输入|输出|
|---|---|---|---|
|Source|消费 Kafka，JSON 解析|Kafka bytes|`DataStream\[GreenTripRecord\]`|
|Clean \(轻\)|仅字段映射 \+ 过滤完全无效记录|`GreenTripRecord`|`DataStream\[GreenTripRecord\]`|
|Partitioning|按事件时间提取 year/month|`GreenTripRecord`|带分区键的记录|
|Hive FileSink|ORC 格式写入 HDFS|记录|HDFS / Hive 分区|

### 任务清单

#### 任务 B\.1：构建独立 Job 入口 `RealtimeOdsJob\.scala`

**推荐结构**：

```scala
object RealtimeOdsJob {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    // 轻量配置：RocksDB 可选（ODS 无状态，可用 HashMap），Checkpoint 仍需要
    val source = KafkaSourceBuilder.build()
    val parsed = env.fromSource(source, ...).process(new JsonParseFunction)
    
    // 轻清洗：只过滤 JSON 解析失败的 record（不为 null 即可），不丢数据
    val odsStream = parsed.filter(_ != null)
      .assignTimestampsAndWatermarks(watermarkStrategy)
    
    // 分流：绿表 / 黄表
    val greenStream = odsStream.filter(_.taxiType.contains("green"))
    val yellowStream = odsStream.filter(_.taxiType.contains("yellow"))
    
    // 写入 Hive ODS
    greenStream.sinkTo(buildGreenOdsFileSink())
    yellowStream.sinkTo(buildYellowOdsFileSink())
    
    env.execute("RealtimeOdsJob")
  }
}
```

#### 任务 B\.2：实现事件时间分区 BucketAssigner

**自定义**（关键代码）：

```scala
class EventTimePartitionAssigner extends BucketAssigner[GreenTripRecord, String] {
  override def getBucketId(element: GreenTripRecord, context: Context): String = {
    val dt = element.pickupDatetime.get  // 这里需要确保非空（轻清洗保证）
    val year = dt.toLocalDateTime.getYear
    val month = dt.toLocalDateTime.getMonthValue
    s"year=$year/month=$month"
  }
  override def getSerializer = SimpleVersionedStringSerializer.INSTANCE
}
```

#### 任务 B\.3：构建 ORC FileSink（复用现有 Vectorizer）

```scala
def buildGreenOdsFileSink(): FileSink[GreenTripRecord] = {
  val hadoopConf = new Configuration()
  hadoopConf.set("orc.compress", "SNAPPY")
  val writerFactory = new OrcBulkWriterFactory(new GreenTripRecordVectorizer(), hadoopConf)
  FileSink.forBulkFormat(new Path("hdfs://.../nyc_taxi_ods.db/taxi_trip_green_ods"), writerFactory)
    .withBucketAssigner(new EventTimePartitionAssigner)
    .withRollingPolicy(OnCheckpointRollingPolicy.build())
    .withOutputFileConfig(OutputFileConfig.builder().withPartPrefix("green").withPartSuffix(".orc").build())
    .build()
}
// 黄表类似，只需改变路径和 Vectorizer（可复用同一个 Vectorizer，字段兼容）
```

#### 任务 B\.4：Hive 分区元数据同步（避免 MSCK REPAIR 延迟）

推荐 **Flink 写入后自动添加分区**：使用 `HivePartitionCommitTrigger` 或独立调度一个简单 Spark 任务（每 10 分钟）：

```scala
// Spark 代码（单独调度）
spark.sql("MSCK REPAIR TABLE nyc_taxi_ods.taxi_trip_green_ods")
```

#### 任务 B\.5：轻清洗规则定义

**ODS 不应丢数据**，只处理：

- 过滤 `pickupDatetime` 为 null 的记录（否则无法分区）

- 过滤 JSON 完全解析失败的记录

- **不**过滤 `fareAmount \&lt; 0` 等业务异常数据（留给离线数仓处理）

---

# 🔴 四、两个 Job 的公共组件与配置

### 1\. 共用 `KafkaSourceBuilder`、`JsonParseFunction`、`GreenTripRecord` 模型

### 2\. 各自独立的 `application\.properties`（或同一份，通过不同启动参数区分）

- Job A 使用 `flink\.metrics\.group` 等

- Job B 使用 `flink\.ods\.checkpoint\.dir`

### 3\. 资源分配建议

|Job|并行度|内存|状态后端|Checkpoint 间隔|
|---|---|---|---|---|
|Job A \(指标\)|2\~4|2GB|RocksDB|60s|
|Job B \(ODS\)|2|1GB|HashMap|120s（可更长，减少小文件）|

---

# 🔴 五、验证与监控清单

|验证项|方法|预期结果|
|---|---|---|
|**指标准确性**|对比 MySQL 中窗口数据与离线从 Hive 同窗口聚合|差异 \&lt; 1%|
|**ODS 分区完整性**|`hdfs dfs \-ls /\.\.\./taxi\_trip\_green\_ods/year=\*/month=\*/`|每个小时都有文件|
|**背压隔离**|模拟 MySQL 慢查询，观察 Job B 是否正常|Job B 不受影响|
|**重启恢复**|Kill Job A 后从 checkpoint 恢复，检查 MySQL 无重复|幂等写入生效|
|**延迟**|Flink UI 中 `currentOutputWatermark` 与当前时间差|\&lt; 3 分钟|

---

# ✅ 六、最终结论

**分两个 Job 是生产级最佳选择**，既能满足您的核心需求（实时大屏 \+ 离线 ODS），又能避免单 Job 耦合带来的风险。
**任务清单已按模块拆分清楚**

> （注：文档部分内容可能由 AI 生成）
