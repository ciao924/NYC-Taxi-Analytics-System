# 实时数据处理 Pipeline 整合指南

## 概述

本文档详细描述 **py_taxi（数据模拟模块）** 与 **flink_consumer（实时处理模块）** 的协同工作方式，帮助用户快速搭建完整的实时数据处理 Pipeline。

---

## 整体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         实时数据处理 Pipeline                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐    ┌──────────────┐    ┌──────────────────────┐      │
│  │   py_taxi        │    │   Kafka      │    │   flink_consumer     │      │
│  │  (数据模拟模块)   │───>│  (消息队列)  │───>│   (实时处理模块)     │      │
│  └──────────────────┘    └──────────────┘    └──────────────────────┘      │
│         │                      │                      │                   │
│         ▼                      ▼                      ▼                   │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────────┐      │
│  │ Parquet文件  │    │ taxi_trip_   │    │   MySQL 数据库       │      │
│  │ (历史数据)   │    │ green/yellow │    │   (ODS + 指标表)     │      │
│  └──────────────┘    └──────────────┘    └──────────────────────┘      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 模块职责划分

| 模块 | 职责 | 核心功能 |
|------|------|----------|
| **py_taxi** | 数据生产层 | 读取 Parquet 文件，按可控速率写入 Kafka |
| **Kafka** | 消息传输层 | 解耦生产和消费，提供可靠的消息传递 |
| **flink_consumer** | 数据处理层 | 消费数据、清洗、质量检测、指标计算 |
| **MySQL** | 数据存储层 | 存储实时明细数据和计算指标 |

---

## 数据流程详解

### 阶段一：数据生产（py_taxi）

```
1. 加载配置
   └─> 读取 config/kafka_config.py
   └─> 读取 .env 环境变量

2. 初始化生产者
   └─> 创建 KafkaProducer 实例
   └─> 配置序列化器和重试策略

3. 读取数据
   └─> 使用 pandas 读取 Parquet 文件
   └─> 按 pickup_datetime 排序

4. 数据校验与发送
   └─> 校验关键字段完整性
   └─> 按指定速率发送到 Kafka
   └─> 每100条记录保存断点
```

### 阶段二：消息传输（Kafka）

| 主题 | 用途 | 分区数建议 |
|------|------|------------|
| `taxi_trip_green` | 绿色出租车数据 | 6 |
| `taxi_trip_yellow` | 黄色出租车数据 | 6 |

### 阶段三：实时处理（flink_consumer）

```
1. Kafka Source 消费数据
   └─> 从 taxi_trip_green/yellow 主题消费

2. JSON 解析
   └─> JsonParseFunction 将 JSON 转换为 GreenTripRecord
   └─> 解析失败 → 死信队列

3. 数据清洗
   └─> DataCleanFunction 执行质量校验
   └─> 清洗失败 → 死信队列

4. 数据输出
   └─> 有效数据 → MySQL ODS 表
   └─> 质量指标 → MySQL 质量表

5. 指标计算（并行作业）
   └─> 订单指标计算
   └─> 热点区域分析
   └─> 费用构成统计
```

---

## 部署与运行流程

### 前置条件

1. **Kafka 集群**：确保 Kafka 服务已启动，主题已创建
2. **MySQL 数据库**：确保数据库和表已创建
3. **Flink 集群**：确保 Flink 集群已启动

### 步骤一：启动 py_taxi 数据生产者

```bash
# 进入 py_taxi 目录
cd py_taxi

# 安装依赖（首次运行）
pip install kafka-python pandas pyarrow python-dotenv

# 配置环境变量
cp .env.example .env
# 修改 .env 中的 Kafka 地址配置

# 启动绿色出租车数据生产者（速率：10条/秒）
python scripts/kafka_producer.py --taxi-type green --rate 10

# 可选：启动黄色出租车数据生产者（新开终端）
python scripts/kafka_producer.py --taxi-type yellow --rate 10
```

### 步骤二：启动 Flink ODS 作业

```bash
# 进入 flink_consumer 目录
cd flink_consumer

# 编译打包（首次运行）
mvn clean package -DskipTests

# 启动 ODS 作业
flink run -c com.taxi.realtime.RealtimeOdsJob \
  target/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config application-ods.properties
```

### 步骤三：启动 Flink 指标作业

```bash
# 启动指标计算作业
flink run -c com.taxi.realtime.RealtimeMetricsJob \
  target/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config application-metrics.properties
```

### 步骤四：验证数据

```sql
-- 连接 MySQL 验证数据
mysql -u root -p taxi_realtime

-- 查看 ODS 表数据
SELECT * FROM green_trip_ods LIMIT 10;

-- 查看指标表数据
SELECT * FROM order_metrics ORDER BY window_end DESC LIMIT 10;

-- 查看死信队列（如有）
SELECT * FROM dead_letter_queue LIMIT 10;
```

---

## 配置一致性要求

### Kafka 配置一致性

| py_taxi (config/kafka_config.py) | flink_consumer (application.properties) |
|----------------------------------|----------------------------------------|
| `bootstrap_servers` | `kafka.bootstrap.servers` |
| `TOPICS['green_taxi']` | `kafka.topic.green` |
| `TOPICS['yellow_taxi']` | `kafka.topic.yellow` |

**配置示例：**

```python
# py_taxi/config/kafka_config.py
KAFKA_CONFIG = {
    'bootstrap_servers': ['hadoop102:9092', 'hadoop103:9092', 'hadoop104:9092']
}
TOPICS = {
    'green_taxi': 'taxi_trip_green',
    'yellow_taxi': 'taxi_trip_yellow'
}
```

```properties
# flink_consumer/src/main/resources/application.properties
kafka.bootstrap.servers=hadoop102:9092,hadoop103:9092,hadoop104:9092
kafka.topic.green=taxi_trip_green
kafka.topic.yellow=taxi_trip_yellow
```

---

## 监控与运维

### 整体监控架构

```
┌──────────────────────────────────────────────────────────────────┐
│                         监控体系                                │
├──────────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │ py_taxi  │    │  Kafka   │    │  Flink   │    │  MySQL   │  │
│  │ 日志监控 │───>│  Lag监控 │───>│ Dashboard│───>│ 慢查询   │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘  │
│         │                │                │                     │
│         ▼                ▼                ▼                     │
│  ┌───────────────────────────────────────────┐                 │
│  │           Prometheus + Grafana            │                 │
│  │         (统一监控仪表盘)                   │                 │
│  └───────────────────────────────────────────┘                 │
│                                                                │
└──────────────────────────────────────────────────────────────────┘
```

### 关键监控指标

| 模块 | 指标 | 监控方式 |
|------|------|----------|
| py_taxi | 发送速率、失败率、进度 | 日志 + Prometheus |
| Kafka | 消费 Lag、分区分布 | kafka-lag-exporter |
| Flink | 吞吐量、延迟、检查点 | Flink Dashboard |
| MySQL | 写入延迟、连接数 | MySQL 慢查询日志 |

### 告警策略

| 告警项 | 阈值 | 触发动作 |
|--------|------|----------|
| Kafka Lag > 10000 | 持续 5 分钟 | 通知运维人员 |
| 数据质量失败率 > 10% | 持续 1 分钟 | 触发告警 |
| Flink 作业失败 | 立即 | 自动重启 + 通知 |
| MySQL 写入失败 > 100 次 | 持续 1 分钟 | 通知运维人员 |

---

## 故障排查指南

### 常见问题及解决方法

| 问题现象 | 可能原因 | 排查步骤 |
|----------|----------|----------|
| py_taxi 无法连接 Kafka | Kafka 服务未启动或网络不通 | 1. `ping hadoop102`<br>2. `kafka-topics.sh --list --bootstrap-server hadoop102:9092` |
| Flink 作业无法消费数据 | 消费者组问题或主题不存在 | 1. 检查 Kafka 主题是否存在<br>2. 检查消费者组配置 |
| 数据写入 MySQL 失败 | 连接配置错误或表不存在 | 1. 检查 MySQL 连接配置<br>2. 确认表已创建 |
| 数据质量告警频繁 | 上游数据质量问题 | 1. 查看死信队列数据<br>2. 检查 py_taxi 数据源 |
| Flink 检查点失败 | 状态过大或 HDFS 问题 | 1. 检查 HDFS 存储<br>2. 调整检查点间隔 |

### 日志位置

| 模块 | 日志路径 |
|------|----------|
| py_taxi | `py_taxi/logs/kafka_producer.log` |
| Flink JobManager | `${FLINK_HOME}/log/flink-*-jobmanager-*.log` |
| Flink TaskManager | `${FLINK_HOME}/log/flink-*-taskmanager-*.log` |

---

## 性能调优建议

### 1. 吞吐量优化

```bash
# py_taxi: 调整发送速率
python scripts/kafka_producer.py --rate 50

# Flink: 调整并行度
flink run -p 8 -c com.taxi.realtime.RealtimeOdsJob ...
```

### 2. Kafka 配置优化

```python
# py_taxi/config/kafka_config.py
KAFKA_CONFIG = {
    'batch_size': 32768,      # 增大批处理
    'linger_ms': 50,          # 减少等待时间
    'acks': '1'               # 降低确认级别（牺牲可靠性换性能）
}
```

### 3. Flink 状态后端配置

```yaml
# flink-conf.yaml
state.backend: rocksdb
state.backend.rocksdb.localdir: /tmp/flink-rocksdb
state.checkpoints.dir: hdfs:///flink/checkpoints
```

---

## 扩展方案

### 1. 添加新的数据源

```python
# 在 py_taxi 中添加新的出租车类型支持
# 修改 scripts/kafka_producer.py
if args.taxi_type == 'uber':
    if args.file_path is None:
        args.file_path = CONFIG['data']['uber_tripdata_path']
    if args.topic is None:
        args.topic = CONFIG['topics']['uber_taxi']
```

### 2. 添加新的指标计算

```scala
// 在 flink_consumer 中添加新的指标
// 修改 RealtimeMetricsJob.scala
val newMetricStream = tripStream
    .keyBy(_.payment_type)
    .window(TumblingEventTimeWindows.of(Time.minutes(5)))
    .aggregate(new PaymentTypeAggregateFunction)
```

---

## 版本兼容性

| py_taxi 版本 | flink_consumer 版本 | Kafka 版本 | Flink 版本 |
|--------------|---------------------|------------|------------|
| v1.2+ | v1.3+ | 3.4.0+ | 1.17.2+ |

---

## 附录：完整启动脚本

```bash
#!/bin/bash
# start_pipeline.sh - 一键启动完整 Pipeline

echo "=== 启动实时数据处理 Pipeline ==="

# 1. 启动 py_taxi 生产者
echo "1. 启动数据生产者..."
cd py_taxi
python scripts/kafka_producer.py --taxi-type green --rate 10 &
GREEN_PID=$!
echo "   绿色出租车生产者 PID: $GREEN_PID"

python scripts/kafka_producer.py --taxi-type yellow --rate 10 &
YELLOW_PID=$!
echo "   黄色出租车生产者 PID: $YELLOW_PID"

# 2. 启动 Flink ODS 作业
echo "2. 启动 Flink ODS 作业..."
cd ../flink_consumer
flink run -d -c com.taxi.realtime.RealtimeOdsJob \
  target/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config application-ods.properties

# 3. 启动 Flink 指标作业
echo "3. 启动 Flink 指标作业..."
flink run -d -c com.taxi.realtime.RealtimeMetricsJob \
  target/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config application-metrics.properties

echo "=== Pipeline 启动完成 ==="
echo "查看状态: flink list"
```