# 实时模块与离线模块衔接分析报告

## 概述

本报告分析 **实时模块 (flink_consumer/RealtimeOdsJob)** 与 **离线模块 (spark_taxi)** 之间的数据衔接情况，包括数据格式、分区策略、字段映射等关键要素。

---

## 一、架构对比

### 1.1 实时模块 (RealtimeOdsJob)

```
Kafka (taxi_trip_green/taxi_trip_yellow)
    │
    ▼
┌─────────────────────────────────────┐
│      RealtimeOdsJob (Flink)          │
│  - JSON 解析                         │
│  - 数据清洗                           │
│  - 时间戳校验                         │
└─────────────────────────────────────┘
    │
    ▼
HDFS ODS 层 (ORC + SNAPPY)
路径: /user/hive_local/warehouse/nyc_taxi_ods.db/taxi_trip_green_ods
分区: year=YYYY/month=MM
```

### 1.2 离线模块数据流

```
Parquet 文件 (本地/HDFS)
    │
    ▼
┌─────────────────────────────────────┐
│      GreenOdsLoader (Spark)          │
│  - 读取 Parquet                      │
│  - 添加 year/month 分区字段           │
│  - 写入 Hive 表                      │
└─────────────────────────────────────┘
    │
    ▼
Hive ODS 表 (ORC + SNAPPY)
表: nyc_taxi_ods.taxi_trip_green_ods
分区: year INT, month INT
    │
    ▼
┌─────────────────────────────────────┐
│      DwdLayerBuilder (Spark)         │
│  - 读取 ODS 数据                      │
│  - 数据清洗和质量检测                  │
│  - 维度关联                           │
└─────────────────────────────────────┘
    │
    ▼
Hive DWD 表 (Iceberg)
表: nyc_taxi_dwd.fact_taxi_trips
```

---

## 二、衔接关键要素对比

### 2.1 存储路径对比

| 属性 | 实时模块 (RealtimeOdsJob) | 离线模块 (GreenOdsLoader) | 是否一致 |
|------|---------------------------|---------------------------|----------|
| HDFS 根路径 | `hdfs://192.168.127.102:8020/user/hive_local/warehouse` | `hdfs://192.168.127.102:8020/user/hive_local/warehouse` | ✅ 一致 |
| 数据库 | `nyc_taxi_ods` | `nyc_taxi_ods` | ✅ 一致 |
| 绿色表名 | `taxi_trip_green_ods` | `taxi_trip_green_ods` | ✅ 一致 |
| 黄色表名 | `taxi_trip_yellow_ods` | `taxi_trip_yellow_ods` | ✅ 一致 |

**结论**：存储路径完全一致，可以实现数据共享。

### 2.2 数据格式对比

| 属性 | 实时模块 (RealtimeOdsJob) | 离线模块 (GreenOdsLoader) | 是否一致 |
|------|---------------------------|---------------------------|----------|
| 存储格式 | ORC | ORC | ✅ 一致 |
| 压缩方式 | SNAPPY | SNAPPY | ✅ 一致 |
| 分区方式 | year/month (目录) | year/month (Hive 分区) | ✅ 一致 |

**结论**：数据格式和压缩方式完全一致。

### 2.3 字段映射对比

#### 绿色出租车字段映射

| 原始字段 (Parquet) | 实时模块 GreenTripRecord | 离线模块 ODS 表字段 | 映射说明 |
|---------------------|-------------------------|-------------------|----------|
| `VendorID` | `vendorId` | `VendorID` | ✅ 直接映射 |
| `lpep_pickup_datetime` | `pickupDatetime` | `lpep_pickup_datetime` | ✅ 时间戳字段 |
| `lpep_dropoff_datetime` | `dropoffDatetime` | `lpep_dropoff_datetime` | ✅ 时间戳字段 |
| `passenger_count` | `passengerCount` | `passenger_count` | ✅ 直接映射 |
| `trip_distance` | `tripDistance` | `trip_distance` | ✅ 直接映射 |
| `PULocationID` | `puLocationId` | `PULocationID` | ✅ 直接映射 |
| `DOLocationID` | `doLocationId` | `DOLocationID` | ✅ 直接映射 |
| `fare_amount` | `fareAmount` | `fare_amount` | ✅ 直接映射 |
| `tip_amount` | `tipAmount` | `tip_amount` | ✅ 直接映射 |
| `total_amount` | `totalAmount` | `total_amount` | ✅ 直接映射 |

**结论**：核心字段完全兼容。

---

## 三、存在的问题

### 3.1 问题一：ORC Writer 字段名映射问题

**问题描述**：
实时模块使用 `OrcBulkWriterFactory` 写入 ORC 文件时，字段名采用 Java Bean 规范（驼峰式），但离线模块期望的字段名是下划线分隔。

**实时模块写入的字段名**：
```
id, vendorId, pickupDatetime, dropoffDatetime, passengerCount, tripDistance,
puLocationId, doLocationId, fareAmount, tipAmount, totalAmount, processTime, taxiType
```

**离线模块期望的字段名**：
```
VendorID, lpep_pickup_datetime, lpep_dropoff_datetime, passenger_count, trip_distance,
PULocationID, DOLocationID, fare_amount, tip_amount, total_amount
```

**解决方案**：

方案 A：在 RealtimeOdsJob 中使用 TableSchema 或 SQL DDL 定义字段映射

```scala
val schema = TableSchema.builder()
  .field("VendorID", Types.INT)
  .field("lpep_pickup_datetime", Types.SQL_TIMESTAMP)
  .field("lpep_dropoff_datetime", Types.SQL_TIMESTAMP)
  .field("passenger_count", Types.INT)
  .field("trip_distance", Types.DOUBLE)
  .field("PULocationID", Types.INT)
  .field("DOLocationID", Types.INT)
  .field("fare_amount", Types.DOUBLE)
  .field("tip_amount", Types.DOUBLE)
  .field("total_amount", Types.DOUBLE)
  .build()

val writerFactory = new OrcBulkWriterFactory[GreenTripRecord](
  new GreenTripRecordVectorizer(schema), hadoopConf)
```

方案 B：在离线模块读取时使用列名映射

```scala
val odsDf = spark.read.orc(odsPath)
  .withColumnRenamed("vendorId", "VendorID")
  .withColumnRenamed("pickupDatetime", "lpep_pickup_datetime")
  // ... 其他字段映射
```

### 3.2 问题二：分区目录结构

**问题描述**：
实时模块使用 `GreenTripRecordBucketAssigner` 进行分区，可能产生不同的目录结构。

**实时模块分区逻辑**：
```scala
// GreenTripRecordBucketAssigner
def getBucketId(record: GreenTripRecord): String = {
  val year = record.pickupDatetime.map(_.toString.substring(0, 4)).getOrElse("unknown")
  val month = record.pickupDatetime.map(_.toString.substring(5, 7)).getOrElse("unknown")
  s"year=$year/month=$month"
}
```

**离线模块期望的分区**：
```
year=2025/month=04/
```

**解决方案**：分区逻辑一致，无需修改。

### 3.3 问题三：Hive 分区元数据同步

**问题描述**：
实时模块写入 HDFS 文件后，新分区不会自动注册到 Hive Metastore。

**解决方案**：

实时模块写入后执行：
```bash
# 方法1：使用 MSCK REPAIR TABLE
hive -e "MSCK REPAIR TABLE nyc_taxi_ods.taxi_trip_green_ods;"

# 方法2：手动添加分区
hive -e "ALTER TABLE nyc_taxi_ods.taxi_trip_green_ods ADD PARTITION (year=2025, month=4);"
```

建议在 RealtimeOdsJob 中添加定期刷新分区的逻辑。

---

## 四、推荐衔接方案

### 4.1 方案一：实时优先写入 ODS，离线读取（推荐）

```
时间线：
─────────────────────────────────────────────────────────────►

实时数据 ──► RealtimeOdsJob ──► HDFS ODS (实时写入)
                                      │
                                      │ 每小时执行 MSCK REPAIR TABLE
                                      ▼
离线作业 ──────────────────────► Hive ODS 表 ◄── 分区元数据同步
                                      │
                                      ▼
                               DwdLayerBuilder
```

**实施步骤**：

1. **确保数据路径一致**
   ```properties
   # flink_consumer/src/main/resources/application-ods.properties
   data.ods.path=hdfs://192.168.127.102:8020/user/hive_local/warehouse/nyc_taxi_ods.db/taxi_trip_yellow_ods
   ```

2. **配置离线模块读取实时 ODS 数据**
   ```bash
   # 设置环境变量
   export DATA_ODS_PATH=hdfs://192.168.127.102:8020/user/hive_local/warehouse/nyc_taxi_ods.db/taxi_trip_green_ods
   ```

3. **定期同步 Hive 分区**
   ```bash
   # 在实时作业中添加或使用定时任务
   hive -e "MSCK REPAIR TABLE nyc_taxi_ods.taxi_trip_green_ods;"
   ```

### 4.2 方案二：实时与离线数据并行写入

```
                    ┌─────────────────┐
                    │  Parquet 文件   │
                    └────────┬────────┘
                             │
          ┌──────────────────┼──────────────────┐
          │                  │                  │
          ▼                  ▼                  ▼
   ┌────────────┐    ┌────────────┐    ┌────────────┐
   │ 实时模块    │    │ 离线模块    │    │  离线模块   │
   │ Realtime   │    │ GreenOds   │    │ DwdLayer   │
   │ OdsJob     │    │ Loader     │    │ Builder    │
   └─────┬──────┘    └─────┬──────┘    └─────┬──────┘
         │                 │                 │
         ▼                 ▼                 ▼
   ┌───────────┐    ┌───────────┐    ┌───────────┐
   │ HDFS ODS  │    │ Hive ODS  │ ──►│ Hive DWD  │
   │ (ORC)     │    │ (ORC)     │    │ (Iceberg) │
   └───────────┘    └───────────┘    └───────────┘
```

---

## 五、字段映射详细表

### 5.1 GreenTripRecord → Hive ODS 字段映射

| GreenTripRecord (驼峰) | Hive ODS (下划线) | 数据类型 | 说明 |
|------------------------|-------------------|----------|------|
| `vendorId` | `VendorID` | INT | 供应商ID |
| `pickupDatetime` | `lpep_pickup_datetime` | TIMESTAMP | 上车时间 |
| `dropoffDatetime` | `lpep_dropoff_datetime` | TIMESTAMP | 下车时间 |
| `passengerCount` | `passenger_count` | INT | 乘客数 |
| `tripDistance` | `trip_distance` | DOUBLE | 行程距离 |
| `puLocationId` | `PULocationID` | INT | 上车区域ID |
| `doLocationId` | `DOLocationID` | INT | 下车区域ID |
| `fareAmount` | `fare_amount` | DOUBLE | 车费 |
| `tipAmount` | `tip_amount` | DOUBLE | 小费 |
| `totalAmount` | `total_amount` | DOUBLE | 总金额 |
| `taxiType` | (分区字段) | STRING | 出租车类型 |
| (自动) | `year` | INT | 年份分区 |
| (自动) | `month` | INT | 月份分区 |

### 5.2 YellowTripRecord 特殊字段

| YellowTripRecord | Hive ODS | 说明 |
|------------------|----------|------|
| `tpep_pickup_datetime` | `tpep_pickup_datetime` | 黄色出租车字段名不同 |
| `tpep_dropoff_datetime` | `tpep_dropoff_datetime` | 黄色出租车字段名不同 |

**注意**：DwdLayerBuilder 已经处理了绿色/黄色出租车的字段统一：
```scala
val yellowDf = spark.table("nyc_taxi_ods.taxi_trip_yellow_ods")
  .withColumn("taxi_type", lit("yellow"))
  .withColumn("pickup_datetime", col("tpep_pickup_datetime"))

val greenDf = spark.table("nyc_taxi_ods.taxi_trip_green_ods")
  .withColumn("taxi_type", lit("green"))
  .withColumn("pickup_datetime", col("lpep_pickup_datetime"))
```

---

## 六、配置一致性检查清单

### 6.1 Kafka 配置

| 配置项 | 实时模块 | 离线模块 | 说明 |
|--------|----------|----------|------|
| `bootstrap.servers` | `hadoop102:9092,hadoop103:9092,hadoop104:9092` | N/A | Kafka 连接 |
| `topic` | `taxi_trip_green`, `taxi_trip_yellow` | N/A | 数据源主题 |
| `consumer.group.id` | `flink-taxi-ods-group-v2` | N/A | 消费者组 |

### 6.2 Hive/HDFS 配置

| 配置项 | 实时模块 | 离线模块 | 说明 |
|--------|----------|----------|------|
| `hive.metastore.uris` | `thrift://192.168.127.102:9083` | `thrift://192.168.127.102:9083` | ✅ 一致 |
| `warehouse.dir` | `hdfs://192.168.127.102:8020/user/hive_local/warehouse` | `hdfs://192.168.127.102:8020/user/hive_local/warehouse` | ✅ 一致 |
| `ods.path` | `nyc_taxi_ods.db/taxi_trip_green_ods` | `nyc_taxi_ods.db/taxi_trip_green_ods` | ✅ 一致 |

### 6.3 数据质量配置

| 规则 | 实时模块 | 离线模块 (DwdLayerBuilder) | 是否一致 |
|------|----------|---------------------------|----------|
| `trip_distance` 范围 | (0, 120] | (0, 120] | ✅ 一致 |
| `passenger_count` 范围 | [1, 6] | [1, 6] | ✅ 一致 |
| `total_amount` 范围 | [0, 800] | [0, 800] | ✅ 一致 |
| 时间校验 | `dropoff > pickup` | `dropoff > pickup` | ✅ 一致 |

---

## 七、结论与建议

### 7.1 衔接可行性

| 维度 | 评估 | 说明 |
|------|------|------|
| 存储路径 | ✅ 完全兼容 | HDFS 路径完全一致 |
| 数据格式 | ✅ 完全兼容 | 都是 ORC + SNAPPY |
| 分区策略 | ✅ 完全兼容 | 都使用 year/month 分区 |
| 字段映射 | ⚠️ 需调整 | 字段名风格不同（驼峰 vs 下划线） |

### 7.2 关键风险点

1. **字段名风格不一致**：实时模块输出驼峰命名，离线模块期望下划线命名
2. **分区元数据同步**：实时写入后 Hive Metastore 不会自动感知新分区

### 7.3 实施建议

1. **立即可执行**：
   - 存储路径、格式、分区策略已完全一致
   - 配置离线模块 `DATA_ODS_PATH` 环境变量指向实时 ODS 路径
   - 添加定期 `MSCK REPAIR TABLE` 任务

2. **短期优化**：
   - 在 RealtimeOdsJob 中添加字段名映射，使用 `TableSchema` 或 `Row` 类型
   - 或在 GreenOdsLoader/DwdLayerBuilder 读取时做列名映射

3. **长期规划**：
   - 统一命名规范，整个项目使用下划线命名
   - 考虑使用 Iceberg 表替代纯 ORC 文件，支持更好的元数据管理

---

## 八、附录：测试验证步骤

### 8.1 验证实时数据写入

```bash
# 1. 启动实时作业
cd flink_consumer
flink run -c com.taxi.realtime.RealtimeOdsJob \
  target/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config application-ods.properties

# 2. 检查 HDFS 文件
hdfs dfs -ls /user/hive_local/warehouse/nyc_taxi_ods.db/taxi_trip_green_ods/

# 3. 查看 ORC 文件内容
hive -e "SELECT * FROM nyc_taxi_ods.taxi_trip_green_ods LIMIT 10;"
```

### 8.2 验证离线模块读取

```bash
# 1. 设置环境变量
export DATA_ODS_PATH=hdfs://192.168.127.102:8020/user/hive_local/warehouse/nyc_taxi_ods.db/taxi_trip_green_ods

# 2. 同步分区
hive -e "MSCK REPAIR TABLE nyc_taxi_ods.taxi_trip_green_ods;"

# 3. 运行 DWD 作业
cd spark_taxi
spark-submit --class com.taxi.etl.dwd.DwdLayerBuilder \
  target/scala-2.12/taxi-analytics-assembly.jar \
  --target-year 2025 --target-months 1,2,3
```