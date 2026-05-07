# Streaming Processor Module

实时流处理模块 - 基于 Apache Flink 的实时数据处理引擎。

## 📌 模块定位

本模块是实时数据处理 Pipeline 的**核心处理层**，承接上游数据生产模块的数据输入，进行实时清洗、质量检测和指标计算。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **Kafka 消费** | 从 Kafka 主题消费实时出租车数据 |
| **JSON 解析** | 将 JSON 消息转换为结构化数据对象 |
| **数据清洗** | 过滤无效数据，确保数据质量 |
| **质量检测** | 实时监控数据质量指标，支持告警触发 |
| **指标计算** | 实时计算核心业务指标（订单量、热点区域等） |
| **多 Sink 输出** | 支持 HDFS、MySQL、告警等多种输出方式 |
| **死信队列** | 处理解析和清洗失败的数据 |
| **状态管理** | 支持 Flink 检查点和状态恢复 |

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Apache Flink | 1.17.2 | 实时流处理引擎 |
| Apache Kafka | 3.4.0 | 消息队列 |
| MySQL | 8.0+ | 数据存储 |
| Scala | 2.12.17 | 开发语言 |
| Maven | 3.6+ | 构建工具 |

## 📁 目录结构

```
streaming-processor/
├── src/main/scala/com/taxi/realtime/
│   ├── config/              # 配置管理
│   ├── model/               # 数据模型
│   ├── quality/             # 质量检测模块
│   ├── sink/                # 数据输出模块
│   ├── source/              # 数据输入模块
│   ├── utils/               # 工具类
│   ├── RealtimeMetricsJob.scala   # 实时指标计算作业
│   └── RealtimeOdsJob.scala       # 实时 ODS 写入作业
├── src/main/resources/       # 配置文件
│   ├── application.properties
│   ├── application-ods.properties
│   ├── application-metrics.properties
│   └── flink-conf.yaml
├── scripts/                  # 脚本文件
│   ├── deploy.bat
│   ├── stop.bat
│   └── savepoint.sh
├── pom.xml                   # Maven 配置
└── README.md                 # 本说明文档
```

## 🚀 快速开始

### 1. 编译打包

```bash
cd streaming-processor
mvn clean package -DskipTests
```

### 2. 启动作业

```bash
# 启动 ODS 作业（实时写入明细数据）
flink run -c com.taxi.realtime.RealtimeOdsJob \
  target/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config application-ods.properties

# 启动指标作业（实时计算指标）
flink run -c com.taxi.realtime.RealtimeMetricsJob \
  target/flink-realtime-consumer-1.0-SNAPSHOT.jar \
  --config application-metrics.properties
```

## 📊 数据处理流程

```
Kafka Source → JSON 解析 → 数据清洗 → 质量检测 → 多 Sink 输出
                                    │
                                    ▼
                              死信队列
```

## 📋 数据质量规则

| 规则 | 字段 | 校验条件 |
|------|------|----------|
| 非空校验 | `VendorID` | 不为 null |
| 时间校验 | `dropoff_datetime` | 大于 `pickup_datetime` |
| 范围校验 | `trip_distance` | (0, 120] |
| 范围校验 | `passenger_count` | [1, 6] |

## 📝 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2025-04-01 | 初始版本 |
| v1.1 | 2025-04-15 | 添加实时指标计算作业 |
| v1.2 | 2025-04-30 | 优化数据质量检测和告警机制 |