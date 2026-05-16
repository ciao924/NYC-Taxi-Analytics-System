# NYC Taxi Analytics System

> 纽约市出租车数据分析系统 —— 基于 1100 万条真实出行数据构建的实时 + 离线一体化大数据分析平台

[![Java](https://img.shields.io/badge/Java-17-blue)](https://www.oracle.com/java/)
[![Spark](https://img.shields.io/badge/Apache%20Spark-3.1.3-orange)](https://spark.apache.org/)
[![Flink](https://img.shields.io/badge/Apache%20Flink-1.17.2-red)](https://flink.apache.org/)
[![Vue](https://img.shields.io/badge/Vue.js-3.4+-green)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 📖 项目简介

本系统以纽约市交通局（NYC TLC）公开发布的 **Yellow Taxi** 和 **Green Taxi** 出行数据为基础，基于 **1100 万条原始行程记录**，构建了一套涵盖数据采集、实时流处理、离线数仓建设、业务可视化与 AI 智能查询的完整大数据分析平台。系统涵盖数据采集、实时流处理、离线数仓建设、业务可视化与 AI 智能查询五大核心能力，是 Lambda 架构在城市交通数据领域的完整工程实现。

### 数据集规模

| 数据集 | 规模 | 说明 |
|--------|------|------|
| Yellow Taxi | ~900 万条 | 纽约黄色出租车行程记录 |
| Green Taxi | ~200 万条 | 纽约绿色出租车行程记录 |
| **合计** | **~1100 万条** | 覆盖完整行程、费用、区域、时间等维度 |

### 核心目标

| 目标 | 描述 | 状态 |
|------|------|------|
| **实时数据处理** | 消费 Kafka 实时数据，完成 ODS 落地、指标计算、质量检测 | ✅ 已完成 |
| **离线数据仓库** | 构建 ODS → DWD → DWS → ADS 完整分层数仓 | ✅ 已完成 |
| **数据质量保障** | 全链路数据质量监控、异常检测、告警推送 | ✅ 已完成 |
| **智能查询分析** | 支持自然语言查询、SQL 自动生成、图表可视化 | ✅ 已完成 |
| **数据可视化** | 多维度图表、热力地图、实时大屏指标展示 | ✅ 已完成 |

---

## 🏗️ 系统架构

### 整体架构

本系统采用经典 **Lambda 架构**，实时通道与离线通道并行运行，共同服务于上层业务查询。

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           数据可视化层 (UI)                               │
│                  ┌────────────────────────────────────┐                 │
│                  │  taxi-analytics-frontend (Vue 3)   │                 │
│                  │  仪表盘 · 数据分析 · 实时监控       │                 │
│                  │  热力地图 · 质量监控 · AI 智能查询  │                 │
│                  └────────────────┬───────────────────┘                 │
└───────────────────────────────────┼─────────────────────────────────────┘
                                    │ HTTP / WebSocket
┌───────────────────────────────────┼─────────────────────────────────────┐
│                           业务服务层 (Backend)                            │
│                  ┌────────────────────────────────────┐                 │
│                  │ taxi-analytics-backend (Spring Boot)│                 │
│                  │  REST API · WebSocket · AI 查询     │                 │
│                  │  数据分析 · 导出任务 · 质量监控      │                 │
│                  └────────────────┬───────────────────┘                 │
└───────────────────────────────────┼─────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼─────────────────────────────────────┐
│                         数据处理层 (Processing)                           │
│   ┌────────────────────────┐    ┌──────────────────────────────┐         │
│   │   streaming-processor  │    │     batch-processor          │         │
│   │   (Apache Flink 1.17)  │    │     (Apache Spark 3.1)       │         │
│   │   · 实时 ODS 落地      │    │     · ODS → DWD → DWS → ADS  │         │
│   │   · 5分钟滑动窗口聚合  │    │     · Iceberg 数据湖管理      │         │
│   │   · TOP N 区域统计     │    │     · 多层数据质量检测        │         │
│   │   · 质量检测与告警     │    │     · 18张 ADS 业务指标表    │         │
│   └────────────────────────┘    └──────────────────────────────┘         │
└───────────────────────────────────┼─────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼─────────────────────────────────────┐
│                           数据采集层 (Ingestion)                          │
│                  ┌────────────────────────────────────┐                 │
│                  │     data-producer (Python)         │                 │
│                  │     · 读取 Parquet 源数据           │                 │
│                  │     · 发送至 Kafka（速率可控）       │                 │
│                  │     · 支持断点续传与 Checkpoint      │                 │
│                  └────────────────────────────────────┘                 │
└─────────────────────────────────────────────────────────────────────────┘

                    ┌───────────────────────────────────┐
                    │         数据存储层 (Storage)        │
                    │  Kafka · HDFS · Hive · Iceberg · MySQL │
                    └───────────────────────────────────┘
```

### 数据流转链路

**实时通道（秒级/分钟级）**

```
Parquet 源文件
    ↓
[data-producer] → Kafka Topic (nyc-yellow-trips / nyc-green-trips)
    ↓
[streaming-processor - Flink]
    ├── RealtimeOdsJob  → 数据清洗 → HDFS/ORC (实时 ODS)
    └── RealtimeMetricsJob → 窗口聚合 → MySQL (实时指标) + 质量告警
```

**离线通道（T+1 批处理）**

```
Parquet 源文件 / HDFS ODS
    ↓
[batch-processor - Spark]
    ├── ODS 加载   → Hive ODS (原始层)
    ├── DWD 构建   → Iceberg (明细层：清洗 + 标准化 + 维度关联)
    ├── DWS 构建   → Iceberg (汇总层：6张主题宽表)
    └── ADS 构建   → MySQL  (应用层：18张业务指标表)
    ↓
[taxi-analytics-backend] → REST API / WebSocket
    ↓
[taxi-analytics-frontend] → 可视化展示
```

---

## 🗄️ 数仓分层设计

本系统严格遵循企业级数仓四层分层规范，各层职责独立、数据可追溯。

```
┌─────────────────────────────────────────────────────────┐
│                    ADS 层 (MySQL)                        │
│         18 张业务指标表，直接驱动前端 BI 展示             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    DWS 层 (Iceberg)                      │
│         6 张主题宽表，按日/小时/区域等维度预聚合          │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    DWD 层 (Iceberg)                      │
│         1 张事实大宽表，清洗标准化 + 维度关联 + 派生字段  │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    ODS 层 (Hive)                         │
│         2 张原始数据表（yellow / green），保留原始格式    │
└─────────────────────────────────────────────────────────┘
```

| 层级 | 全称 | 存储格式 | 刷新策略 | 数据质量要求 |
|------|------|----------|----------|-------------|
| **ODS** | Operational Data Store | Hive / ORC+SNAPPY | 批量/实时写入 | 完整性 ≥ 99.9% |
| **DWD** | Data Warehouse Detail | Iceberg / Parquet | 每日全量覆盖 | 完整性 100%，准确性 ≥ 99.5% |
| **DWS** | Data Warehouse Service | Iceberg / Parquet | 每日全量覆盖 | 一致性 100% |
| **ADS** | Application Data Store | MySQL | T+1 批量写入 | 完整性 100% |

### DWS 主题宽表（6 张）

| 表名 | 说明 |
|------|------|
| `dws_trip_daily` | 日行程汇总表 |
| `dws_trip_hourly` | 小时行程汇总表 |
| `dws_trip_zone_pickup_daily` | 上车区域热力日汇总表 |
| `dws_trip_zone_dropoff_daily` | 下车区域热力日汇总表 |
| `dws_trip_fee_daily` | 费用结构日汇总宽表 |
| `dws_trip_vendor_daily` | 供应商日汇总表 |

### ADS 业务指标表（18 张）

**日级指标（6 张）**：`analysis_kpi_daily`、`analysis_hourly_distribution`、`analysis_payment_analysis`、`analysis_vendor_analysis`、`analysis_weekday_analysis`、`analysis_airport_analysis`

**分布分析（4 张）**：`analysis_distance_distribution`、`analysis_duration_distribution`、`analysis_passenger_distribution`、`analysis_revenue_contribution`

**费用分析（5 张）**：`analysis_fee_by_borough`、`analysis_fee_by_taxi_type`、`analysis_fee_composition`、`analysis_fee_percentage`、`analysis_fee_trend`

**流量分析（3 张）**：`analysis_borough_flow`、`analysis_pickup_hotspots`、`analysis_dropoff_hotspots`

---

## 🔧 技术栈

### 大数据处理层

| 组件 | 版本 | 用途 |
|------|------|------|
| Apache Spark | 3.1.3 | 离线批处理引擎 |
| Apache Flink | 1.17.2 | 实时流处理引擎 |
| Apache Kafka | 3.4.0 | 消息队列 |
| Apache Hive | 3.1.2 | 数据仓库元数据管理 |
| Apache Iceberg | 1.1.0 | 数据湖存储（ACID + Schema 演进） |
| HDFS | 3.3+ | 分布式文件存储 |
| Scala | 2.12.10 | Spark/Flink 开发语言 |

### 应用服务层

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.1.12 | 后端服务框架 |
| MyBatis Plus | 3.5.5 | ORM 框架 |
| Java | 17 | 后端开发语言 |
| MySQL | 8.0+ | ADS 层及业务数据存储 |
| WebSocket | — | 实时数据推送 |

### 前端展示层

| 组件 | 版本 | 用途 |
|------|------|------|
| Vue.js | 3.4+ | 前端框架 |
| TypeScript | 5.4+ | 类型安全 |
| ECharts | 5.5+ | 数据图表库 |
| Element Plus | 2.6+ | UI 组件库 |
| Pinia | 2.1+ | 状态管理 |
| Vite | 5.x | 构建工具 |

### 数据生产层

| 组件 | 版本 | 用途 |
|------|------|------|
| Python | 3.8+ | 数据生产脚本 |
| kafka-python | — | Kafka 生产者客户端 |
| pandas / pyarrow | — | Parquet 文件读取 |

---

## 📁 项目结构

```
spark_test/
├── data-producer/                         # 数据生产模块 (Python)
│   ├── config/                            # Kafka 连接配置
│   ├── scripts/
│   │   ├── kafka_producer.py             # 主生产者脚本（速率控制 + 断点续传）
│   │   ├── test_kafka_connection.py      # 连接测试
│   │   └── check_parquet_file.py         # 源数据检查
│   └── docs/MODULE_ANALYSIS.md           # 模块分析文档
│
├── streaming-processor/                   # 实时流处理模块 (Flink)
│   ├── src/main/scala/com/taxi/realtime/
│   │   ├── RealtimeOdsJob.scala          # 实时 ODS 落地作业
│   │   ├── RealtimeMetricsJob.scala      # 实时指标计算作业
│   │   ├── config/                        # 配置管理
│   │   ├── model/                         # 数据模型（Yellow/Green/Metrics）
│   │   ├── quality/                       # 数据质量检测
│   │   ├── sink/                          # 数据输出（HDFS / MySQL）
│   │   ├── source/                        # 数据源（Kafka Consumer）
│   │   └── utils/                         # 工具类
│   ├── src/main/resources/               # 配置文件
│   ├── scripts/                           # 部署脚本
│   └── docs/MODULE_ANALYSIS.md           # 模块分析文档
│
├── batch-processor/                       # 离线批处理模块 (Spark)
│   ├── src/main/scala/com/taxi/etl/
│   │   ├── ods/                           # ODS 层：数据加载与校验
│   │   │   ├── GreenOdsLoader.scala
│   │   │   ├── YellowOdsLoader.scala
│   │   │   └── OdsValidator.scala
│   │   ├── dwd/                           # DWD 层：数据清洗与维度关联
│   │   │   └── DwdLayerBuilder.scala
│   │   ├── dws/                           # DWS 层：多维聚合宽表
│   │   │   └── DwsLayerBuilder.scala
│   │   ├── ads/                           # ADS 层：业务指标输出
│   │   │   ├── AdsLayerBuilder.scala
│   │   │   ├── base/                      # 基础组件（写入器/校验器/加载器）
│   │   │   ├── daily/                     # 日级指标
│   │   │   ├── distribution/              # 分布分析
│   │   │   ├── fee/                       # 费用分析
│   │   │   └── traffic/                   # 流量分析
│   │   ├── common/                        # 公共组件
│   │   │   ├── CacheManager.scala        # 缓存管理
│   │   │   ├── ConfigManager.scala       # 配置管理
│   │   │   ├── IcebergTableManager.scala # Iceberg 表管理（原子写入）
│   │   │   ├── QualityManager.scala      # 质量管理
│   │   │   ├── SmartCacheLite.scala      # 智能缓存（自动追踪释放）
│   │   │   └── SparkSessionFactory.scala # Spark 会话工厂
│   │   ├── quality/                       # 质量报告（DWD/DWS/ADS 层）
│   │   ├── utils/                         # 工具类（DeadLetter / Monitor）
│   │   └── exception/                     # 异常处理
│   ├── src/main/resources/
│   │   ├── application.conf              # 主配置
│   │   ├── application-dev.conf          # 开发环境配置
│   │   ├── application-prod.conf         # 生产环境配置
│   │   └── hive-site.xml
│   └── docs/MODULE_ANALYSIS.md           # 模块分析文档
│
├── taxi/
│   ├── taxi-analytics-backend/            # 后端服务模块 (Spring Boot)
│   │   ├── src/main/java/com/taxi/analytics/
│   │   │   ├── common/                    # 公共模块（配置/异常/统一返回）
│   │   │   └── modules/                   # 业务模块
│   │   │       ├── ai/                   # AI 智能查询（NL2SQL + 图表推荐）
│   │   │       ├── analysis/             # 多维数据分析
│   │   │       ├── dashboard/            # 仪表盘 KPI
│   │   │       ├── export/               # 数据导出
│   │   │       ├── map/                  # 热力地图
│   │   │       ├── quality/              # 数据质量监控
│   │   │       └── realtime/             # 实时指标 + WebSocket 推送
│   │   ├── src/main/resources/
│   │   └── docs/MODULE_ANALYSIS.md       # 模块分析文档
│   │
│   └── taxi-analytics-frontend/           # 前端应用模块 (Vue 3)
│       ├── src/
│       │   ├── api/                       # API 接口封装
│       │   ├── components/               # 公共组件（AI / Analysis）
│       │   ├── views/                    # 页面视图
│       │   │   ├── dashboard/            # 仪表盘
│       │   │   ├── analysis/             # 数据分析
│       │   │   ├── realtime/             # 实时监控
│       │   │   ├── map/                  # 热力地图
│       │   │   ├── quality/              # 质量监控
│       │   │   └── ai/                   # AI 智能查询
│       │   ├── stores/                   # Pinia 状态管理
│       │   └── router/                   # 路由配置
│       └── docs/MODULE_ANALYSIS.md       # 模块分析文档
│
├── packages/
│   └── shared-types/                      # 前后端共享类型 (TypeScript)
│       ├── src/api/
│       │   ├── common.ts                 # 通用请求/响应类型
│       │   ├── dashboard.ts              # 仪表盘类型
│       │   ├── ai.ts                     # AI 查询类型
│       │   ├── analysis.ts               # 分析数据类型
│       │   ├── quality.ts                # 质量类型
│       │   └── map.ts                    # 地图类型
│       └── docs/MODULE_ANALYSIS.md       # 模块分析文档
│
├── docs/                                  # 项目级文档
│   ├── COMPLETE_PROJECT_ANALYSIS_REPORT.md
│   ├── 项目-数仓分层设计.md
│   ├── 项目-数据字典.md
│   ├── 项目-ETL开发规范.md
│   ├── 项目-数仓命名规范.md
│   └── INTEGRATION_GUIDE.md
└── README.md
```

---

## ✨ 核心功能详解

### 📊 仪表盘（Dashboard）

- **KPI 总览**：总订单量、总营收、平均车费、平均行程距离等核心指标卡片
- **趋势分析**：按天/周/月展示订单量与营收趋势折线图
- **支付结构**：各支付方式（现金/信用卡/移动支付等）订单占比饼图
- **供应商对比**：各出租车供应商订单量与营收柱状图对比

### ⚡ 实时监控（Realtime）

- **实时 KPI**：当前小时订单量、实时营收滚动更新（WebSocket 驱动）
- **热点区域 TOP N**：实时统计上车/下车最热门的前 10 个区域
- **费用结构实时分布**：车费、小费、附加费等费用组成实时占比
- **数据质量告警**：异常数据实时推送告警

### 📈 数据分析（Analysis）

覆盖 10+ 个分析维度，支持时间范围、车型、区域多维筛选：

- **机场分析**：机场行程占比、高峰时段、平均费用
- **区域营收分析**：各行政区营收贡献排行与区域流向
- **费用组成分析**：车费、MTA税、拥堵费、机场费等费用结构
- **小时分布分析**：24小时出行规律热力分布
- **支付方式分析**：现金/无现金支付趋势与区域差异
- **车型分析**：Yellow vs Green 出租车各维度对比
- **行程特征分析**：行程距离、时长、乘客数的分布规律
- **供应商分析**：各供应商服务质量与收入对比
- **工作日分析**：工作日 vs 周末出行模式差异

### 🗺️ 热力地图（Map）

- 基于高德地图的区域散点热力图渲染
- 支持上车热点 / 下车热点自由切换
- 按行政区（Borough）筛选，支持时间段过滤
- 热点数据来源于 ADS 层 `analysis_pickup_hotspots` / `analysis_dropoff_hotspots` 表

### 🔍 数据质量监控（Quality）

- **全链路质量报告**：ODS / DWD / DWS / ADS 各层质量评分汇总
- **质量指标体系**：完整性检查、唯一性检查、值域合理性检查
- **实时告警记录**：Flink 实时检测异常记录，推送告警至 MySQL
- **表健康状态**：各业务指标表数据量、更新时间、质量得分一览

### 🤖 AI 智能查询（AI）

本系统集成自然语言到 SQL 的全链路 AI 查询能力，是系统的核心创新模块：

- **自然语言输入**：用中文直接提问，如"2024年1月机场行程的平均小费是多少"
- **SQL 自动生成**：大模型理解业务意图，结合 Schema 自动生成可执行 SQL
- **图表智能推荐**：根据数据结构自动推荐最适合的可视化方式（柱状图/折线图/饼图等）
- **多轮对话**：支持上下文关联的多轮追问，历史会话持久化
- **ETL SQL 生成**：辅助生成数仓各层的 ETL 处理 SQL
- **数据倾斜诊断**：辅助分析 Spark/Flink 作业的数据倾斜问题
- **Flink 反压分析**：智能分析实时作业背压原因

**技术实现**：
- 支持 **Coze** 和 **DeepSeek** 双大模型接入
- DSL 中间层设计，提高 SQL 生成的准确率与稳定性
- SQL 安全检查机制，防止注入攻击，仅允许只读查询
- Schema 自动检索，用户无需了解表结构即可提问

---

## 🚀 快速开始

### 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 后端 + Spark/Flink 运行环境 |
| Maven | 3.9+ | Java/Scala 项目构建 |
| Scala | 2.12.10 | Spark/Flink 开发语言 |
| Python | 3.8+ | 数据生产脚本 |
| Node.js | 20+ | 前端构建 |
| Apache Kafka | 3.4+ | 消息队列 |
| Apache Flink | 1.17.2 | 实时处理集群 |
| Apache Spark | 3.1.3 | 离线处理集群 |
| Apache Hive | 3.1.2 | 元数据管理 |
| Hadoop HDFS | 3.3+ | 分布式存储 |
| MySQL | 8.0+ | 业务数据库 |

### 数据准备

将 NYC TLC 原始 Parquet 文件放置于指定目录：

```
/path/to/data/
├── yellow_tripdata_*.parquet    # Yellow Taxi 数据
└── green_tripdata_*.parquet     # Green Taxi 数据
```

在 `data-producer/config/` 中配置数据路径和 Kafka 连接信息。

### 启动顺序

#### 1. 启动数据生产模块

```bash
cd data-producer
pip install -r requirements.txt

# 启动 Yellow Taxi 数据生产（--rate 控制每秒发送记录数）
python scripts/kafka_producer.py --topic nyc-yellow-trips --rate 100

# 启动 Green Taxi 数据生产
python scripts/kafka_producer.py --topic nyc-green-trips --rate 100
```

#### 2. 启动实时流处理模块（可选，用于实时监控）

```bash
cd streaming-processor
mvn clean package -DskipTests

# 启动 ODS 落地作业（Kafka → HDFS/ORC）
flink run -c com.taxi.realtime.RealtimeOdsJob \
  target/flink-realtime-1.0-SNAPSHOT.jar

# 启动实时指标计算作业（5分钟滑动窗口 → MySQL）
flink run -c com.taxi.realtime.RealtimeMetricsJob \
  target/flink-realtime-1.0-SNAPSHOT.jar
```

#### 3. 启动离线批处理模块（构建完整数仓）

```bash
cd batch-processor
mvn clean package -DskipTests

# Step 1: ODS 层加载（Yellow + Green 双路）
spark-submit --class com.taxi.etl.ods.YellowOdsLoader \
  --executor-instances 4 --executor-memory 8g \
  target/spark-test-1.0-SNAPSHOT.jar

spark-submit --class com.taxi.etl.ods.GreenOdsLoader \
  --executor-instances 4 --executor-memory 8g \
  target/spark-test-1.0-SNAPSHOT.jar

# Step 2: DWD 层构建（数据清洗 + 维度关联 + 派生字段）
spark-submit --class com.taxi.etl.dwd.DwdLayerBuilder \
  --executor-instances 6 --executor-memory 12g \
  target/spark-test-1.0-SNAPSHOT.jar

# Step 3: DWS 层构建（多维聚合宽表）
spark-submit --class com.taxi.etl.dws.DwsLayerBuilder \
  --executor-instances 4 --executor-memory 10g \
  target/spark-test-1.0-SNAPSHOT.jar

# Step 4: ADS 层构建（18张业务指标表 → MySQL）
spark-submit --class com.taxi.etl.ads.AdsLayerBuilder \
  --executor-instances 2 --executor-memory 8g \
  target/spark-test-1.0-SNAPSHOT.jar
```

#### 4. 启动后端服务

```bash
cd taxi/taxi-analytics-backend
# 修改 src/main/resources/application.yml 配置数据库连接
mvn spring-boot:run
```

#### 5. 启动前端应用

```bash
cd taxi/taxi-analytics-frontend
npm install
npm run dev
```

### 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端应用 | http://localhost:5173 | 主应用入口 |
| 后端 API | http://localhost:8080/api | REST API |
| Swagger 文档 | http://localhost:8080/swagger-ui.html | 接口文档 |
| Flink Web UI | http://hadoop102:8081 | 实时作业监控 |

### Spark 作业资源配置参考

| 作业 | Executor 数量 | Executor 内存 | Executor CPU |
|------|--------------|--------------|-------------|
| ODS 加载 | 4 | 8 GB | 2 核 |
| DWD 构建 | 6 | 12 GB | 3 核 |
| DWS 构建 | 4 | 10 GB | 2 核 |
| ADS 构建 | 2 | 8 GB | 2 核 |

---

## 📊 监控与运维

### 关键监控指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| Spark 作业执行时间 | 从启动到完成的总耗时 | > 2 小时 |
| 数据保留率 | （最终记录数 / 原始记录数）× 100% | < 95% |
| 质量得分 | 各层数据质量综合评分 | < 90 分 |
| ADS 构建失败表数 | 18张 ADS 表中构建失败的数量 | > 0 |
| Flink Checkpoint 延迟 | 实时作业检查点完成延迟 | > 30 秒 |

### 关键日志示例

```
✅ ODS 加载完成：yellow_taxi 10,235,841 条 / green_taxi 1,024,396 条
✅ DWD 构建成功：fact_taxi_trips 处理完成，数据保留率 98.7%
✅ DWS 构建成功：6 张宽表写入完成
✅ ADS 层构建完成：成功 18 张，失败 0 张
❌ 质量检测告警：完整性检查未通过，当前完整性 94.2% < 阈值 95%
```

---

## 🔑 核心技术亮点

### 1. Iceberg 数据湖原子写入

批处理各层均采用 Iceberg 事务性覆盖写入，保障写入过程中查询不受影响：

```scala
IcebergTableManager.atomicOverwrite(
  spark, dataFrame, "nyc_taxi_dwd", "fact_taxi_trips",
  Seq("year", "month", "taxi_type")
)
```

### 2. Spark 性能优化体系

| 优化项 | 说明 |
|--------|------|
| Broadcast Join | 维度表（Zone / Borough / Payment 等）广播关联，消除 Shuffle |
| 分区裁剪 | 按 `year` / `month` 分区过滤，减少数据扫描量 |
| 列裁剪 | DWS 层只读取必要字段，降低内存压力 |
| 单次扫描多指标聚合 | 一次 groupBy 计算全部指标，避免多次 I/O |
| SmartCacheLite | 自动追踪 DataFrame 缓存状态，处理完成后统一释放，防止内存泄漏 |

### 3. Flink 实时质量检测

实时流中内置多维数据质量检测，异常记录路由至 Dead Letter Queue，并推送告警：

- 空值检测（关键字段非空校验）
- 值域合理性检测（金额、距离、人数等范围校验）
- 时间逻辑检测（上车时间须早于下车时间）
- 异常告警写入 MySQL，供质量监控页面展示

### 4. 前后端 TypeScript 类型统一

通过 `@taxi-analytics/shared-types` 包在前后端间共享完整的类型定义，从根源上消除接口联调中的类型不一致问题。

### 5. AI NL2SQL 全链路

```
用户输入自然语言
    ↓
意图识别 + Schema 检索
    ↓
大模型（DeepSeek / Coze）生成 SQL
    ↓
SQL 安全检查（防注入 + 只读校验）
    ↓
执行查询 → 返回数据
    ↓
图表类型智能推荐 → 前端渲染
```

---

## 📚 文档索引

### 项目级文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目概述 | `README.md` | 本文件 |
| 完整分析报告 | `docs/COMPLETE_PROJECT_ANALYSIS_REPORT.md` | 系统整体架构与功能分析 |
| 数仓分层设计 | `docs/项目-数仓分层设计.md` | 数仓四层架构设计规范 |
| 数据字典 | `docs/项目-数据字典.md` | 所有数据表字段说明 |
| ETL 开发规范 | `docs/项目-ETL开发规范.md` | ETL 开发流程与命名规范 |
| 数仓命名规范 | `docs/项目-数仓命名规范.md` | 表、字段命名规范 |
| 集成指南 | `docs/INTEGRATION_GUIDE.md` | 组件集成与环境配置指南 |

### 模块文档

| 模块 | 文档路径 | 核心内容 |
|------|----------|---------|
| data-producer | `data-producer/docs/MODULE_ANALYSIS.md` | Kafka 生产者实现、速率控制、断点续传 |
| streaming-processor | `streaming-processor/docs/MODULE_ANALYSIS.md` | Flink Job 设计、窗口策略、质量检测 |
| batch-processor | `batch-processor/docs/MODULE_ANALYSIS.md` | Spark ETL 全链路、性能优化、数仓建设 |
| taxi-analytics-backend | `taxi/taxi-analytics-backend/docs/MODULE_ANALYSIS.md` | REST API 设计、AI 查询、WebSocket |
| taxi-analytics-frontend | `taxi/taxi-analytics-frontend/docs/MODULE_ANALYSIS.md` | Vue 组件设计、图表集成、路由配置 |
| shared-types | `packages/shared-types/docs/MODULE_ANALYSIS.md` | TypeScript 类型定义规范 |

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 代码提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

| 类型 | 说明 |
|------|------|
| `feat` | 新增功能 |
| `fix` | 修复 Bug |
| `docs` | 文档更新 |
| `refactor` | 代码重构（不影响功能） |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具链变更 |

### 修复日志规范

在各模块 `docs/` 目录创建 `修复日志-YYYY-MM-DD.md` 记录变更内容。

---


**项目地址**：[https://github.com/ciao924/NYC-Taxi-Analytics-System](https://github.com/ciao924/NYC-Taxi-Analytics-System)

> 基于 1100 万条纽约市出租车行程数据 · Lambda 架构 · Spark + Flink + Vue 3 全栈实现