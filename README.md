# NYC Taxi Analytics System

纽约市出租车数据分析系统 - 基于 Apache Flink、Spark、Kafka 的实时+离线一体化数据处理平台。

## 📊 项目概述

本项目是一个完整的出租车数据分析系统，包含实时流处理和离线批处理两条数据链路，用于处理纽约市出租车运营数据。

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        NYC Taxi Analytics System                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────┐     ┌──────────────┐     ┌──────────────────────┐     │
│  │  Data Producer  │────▶│    Kafka     │────▶│  Streaming Processor │     │
│  │   (Python)      │     │   (Topic)    │     │     (Flink)          │     │
│  └────────┬────────┘     └──────────────┘     └──────────┬───────────┘     │
│           │                                             │                  │
│           ▼                                             ▼                  │
│  ┌─────────────────┐                          ┌──────────────────────┐     │
│  │  Parquet Files  │                          │       HDFS ODS       │     │
│  └────────┬────────┘                          └──────────┬───────────┘     │
│           │                                             │                  │
│           ▼                                             ▼                  │
│  ┌─────────────────┐                          ┌──────────────────────┐     │
│  │  Batch Processor│◀─────────────────────────│     Hive Metastore   │     │
│  │    (Spark)      │                          └──────────────────────┘     │
│  └────────┬────────┘                                                       │
│           ▼                                                                │
│  ┌─────────────────┐     ┌──────────────┐     ┌──────────────────────┐     │
│  │   DWD/DWS Layer │────▶│   Iceberg    │────▶│   MySQL Dashboard   │     │
│  │  (Hive Tables)  │     │  (Data Lake) │     │   (BI Reporting)    │     │
│  └─────────────────┘     └──────────────┘     └──────────────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 📁 项目结构

```
spark_test/
├── data-producer/              # 数据生产模块 (Python + Kafka)
│   ├── config/                 # 配置文件
│   ├── scripts/                # Python 脚本
│   ├── data/                   # 数据源文件
│   ├── checkpoint/             # 断点续传
│   ├── logs/                   # 日志文件
│   └── README.md               # 模块说明
├── streaming-processor/        # 实时流处理模块 (Flink + Scala)
│   ├── src/main/scala/         # Scala 源代码
│   ├── src/main/resources/     # 配置资源
│   ├── scripts/                # 部署脚本
│   └── README.md               # 模块说明
├── batch-processor/            # 离线批处理模块 (Spark + Scala)
│   ├── src/main/scala/         # Scala 源代码
│   ├── src/main/resources/     # 配置资源
│   └── README.md               # 模块说明
├── docs/                       # 项目文档
│   ├── architecture.md         # 架构设计文档
│   ├── etl-spec.md             # ETL 开发规范
│   ├── naming-spec.md          # 命名规范
│   └── data-dictionary.md      # 数据字典
├── .gitignore                  # Git 忽略配置
├── INTEGRATION_GUIDE.md        # 模块整合指南
├── REALTIME_OFFLINE_INTEGRATION.md  # 实时离线衔接分析
└── README.md                   # 项目说明
```

## 🚀 模块说明

### 1. Data Producer (数据生产模块)

**职责**：将历史 Parquet 数据以可控速率写入 Kafka，模拟实时数据流

**主要功能**：
- 支持 Green/Yellow 两种出租车数据
- 速率控制（可配置发送速率）
- 断点续传（自动保存发送进度）
- 数据校验（过滤无效记录）

**技术栈**：Python 3.8+, kafka-python, pandas, pyarrow

**启动命令**：
```bash
cd data-producer
python scripts/kafka_producer.py --taxi-type green --rate 10
```

### 2. Streaming Processor (实时流处理模块)

**职责**：从 Kafka 消费实时数据，进行清洗、质量检测和指标计算

**主要功能**：
- 实时数据消费（Kafka Source）
- JSON 解析和数据清洗
- 数据质量检测和告警
- 实时指标计算
- 多 Sink 输出（HDFS、MySQL）

**技术栈**：Apache Flink 1.17.2, Scala 2.12

**启动命令**：
```bash
cd streaming-processor
mvn clean package -DskipTests
flink run -c com.taxi.realtime.RealtimeOdsJob target/flink-realtime-consumer-1.0-SNAPSHOT.jar --config application-ods.properties
```

### 3. Batch Processor (离线批处理模块)

**职责**：处理历史数据，构建数仓分层（ODS → DWD → DWS）

**主要功能**：
- 批量数据读取和转换
- 数仓分层构建
- 维度表关联
- 数据质量检查

**技术栈**：Apache Spark 3.5+, Scala 2.12, Apache Iceberg

**启动命令**：
```bash
cd batch-processor
mvn clean package -DskipTests
spark-submit --class com.taxi.etl.dwd.DwdLayerBuilder target/taxi-analytics-assembly.jar
```

## 🛠️ 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 1.8+ | 运行环境 |
| Scala | 2.12.17 | Flink/Spark 编译 |
| Python | 3.8+ | 数据生产模块 |
| Apache Flink | 1.17.2 | 实时流处理 |
| Apache Spark | 3.5+ | 离线批处理 |
| Apache Kafka | 3.4.0+ | 消息队列 |
| Apache Hive | 3.1.2+ | 元数据管理 |
| MySQL | 8.0+ | 指标存储 |

## 🔧 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/ciao924/NYC.git
cd NYC/spark_test
```

### 2. 数据生产模块

```bash
cd data-producer
pip install -r requirements.txt
python scripts/kafka_producer.py --taxi-type green --rate 10
```

### 3. 实时流处理模块

```bash
cd streaming-processor
mvn clean package -DskipTests
flink run -c com.taxi.realtime.RealtimeOdsJob target/flink-realtime-consumer-1.0-SNAPSHOT.jar --config application-ods.properties
```

### 4. 离线批处理模块

```bash
cd batch-processor
mvn clean package -DskipTests
spark-submit --class com.taxi.etl.dwd.DwdLayerBuilder target/taxi-analytics-assembly.jar
```

## 📋 数据链路

### 实时链路

```
Parquet 文件 → Data Producer → Kafka Topic → Flink Streaming → HDFS ODS → DWD/DWS
```

### 离线链路

```
Parquet 文件 → Spark Batch → Hive ODS → DWD/DWS → Iceberg → MySQL Dashboard
```

## 📊 数仓分层

| 层级 | 说明 | 存储 |
|------|------|------|
| **ODS** | 原始数据层 | Hive ORC |
| **DWD** | 明细数据层 | Iceberg |
| **DWS** | 汇总数据层 | Iceberg |
| **ADS** | 应用数据层 | MySQL |

## 📝 命名规范

### 文件命名
- 目录：小写字母 + 连字符（`data-producer`）
- Python 文件：小写字母 + 下划线（`kafka_producer.py`）
- Scala 文件：大驼峰命名（`RealtimeOdsJob.scala`）

### 数据库表命名
- ODS 层：`ods_taxi_trip_green`
- DWD 层：`dwd_taxi_trip_fact`
- DWS 层：`dws_taxi_order_metrics`

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/xxx`)
3. 提交代码 (`git commit -m 'Add xxx'`)
4. 推送到分支 (`git push origin feature/xxx`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证。

---

**版本**: v1.0  
**更新日期**: 2026-05  
**作者**: ciao924