# NYC Taxi Analytics System - 完整项目分析报告

**文档版本**：v1.0
**生成日期**：2026-06-22
**数据规模**：~1100 万条纽约市出租车行程记录

---

## 目录

1. [项目概述](#一项目概述)
2. [系统架构](#二系统架构)
3. [技术栈详解](#三技术栈详解)
4. [模块结构分析](#四模块结构分析)
5. [数仓分层设计](#五数仓分层设计)
6. [核心业务功能](#六核心业务功能)
7. [数据流转链路](#七数据流转链路)
8. [配置与部署](#八配置与部署)
9. [质量保障体系](#九质量保障体系)
10. [AI 智能查询模块](#十ai-智能查询模块)
11. [项目文档清单](#十一项目文档清单)
12. [总结与展望](#十二总结与展望)

---

## 一、项目概述

### 1.1 项目定位

本项目是基于 **纽约市交通局（NYC TLC）公开发布的真实出租车出行数据** 构建的完整大数据分析平台。项目采用经典 **Lambda 架构**，实现了实时流处理与离线批处理的统一数据服务。

### 1.2 核心目标

| 目标 | 描述 | 状态 |
|------|------|------|
| **实时数据处理** | 消费 Kafka 实时数据，完成 ODS 落地、指标计算、质量检测 | ✅ 已完成 |
| **离线数据仓库** | 构建 ODS → DWD → DWS → ADS 完整分层数仓 | ✅ 已完成 |
| **数据质量保障** | 全链路数据质量监控、异常检测、告警推送 | ✅ 已完成 |
| **智能查询分析** | 支持自然语言查询、SQL 自动生成、图表可视化 | ✅ 已完成 |
| **数据可视化** | 多维度图表、热力地图、实时大屏指标展示 | ✅ 已完成 |

### 1.3 数据集规模

| 数据集 | 规模 | 说明 |
|--------|------|------|
| Yellow Taxi | ~900 万条 | 纽约黄色出租车行程记录 |
| Green Taxi | ~200 万条 | 纽约绿色出租车行程记录 |
| **合计** | **~1100 万条** | 覆盖完整行程、费用、区域、时间等维度 |

### 1.4 业务价值

- **运营分析**：洞察出租车运营规律，优化调度策略
- **收入分析**：费用结构、小费趋势、区域营收贡献
- **流量分析**：热点区域识别、高峰时段预测
- **质量监控**：全链路数据质量保障，异常及时发现

---

## 二、系统架构

### 2.1 整体架构图

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
│   │   · 5分钟滑动窗口聚合  │    │     · Hive 数据仓库管理      │         │
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
                    │  Kafka · HDFS · Hive · MySQL │
                    └───────────────────────────────────┘
```

### 2.2 架构特点

| 特点 | 说明 |
|------|------|
| **Lambda 架构** | 实时通道与离线通道并行，兼顾低延迟与数据准确性 |
| **分层数仓** | ODS → DWD → DWS → ADS 四层架构，职责清晰 |
| **Hive 数据仓库** | 基于 Hive 的企业级数据仓库，支持分区与压缩 |
| **实时推送** | WebSocket 实现秒级数据更新 |
| **AI 赋能** | NL2SQL 自然语言查询，智能图表推荐 |

---

## 三、技术栈详解

### 3.1 大数据处理层

| 组件 | 版本 | 用途 | 关键配置 |
|------|------|------|----------|
| Apache Spark | 3.1.3 | 离线批处理引擎 | executor-memory 8g, num-executors 4-6 |
| Apache Flink | 1.17.2 | 实时流处理引擎 | parallelism 4-8, checkpointInterval 120s |
| Apache Kafka | 3.4.0 | 消息队列 | acks=all, retries=3 |
| Apache Hive | 3.1.2 | 数据仓库元数据管理 | ORC + SNAPPY |
| HDFS | 3.3+ | 分布式文件存储 | replication=3 |
| Scala | 2.12.10 | Spark/Flink 开发语言 | target:jvm-1.8 |

### 3.2 应用服务层

| 组件 | 版本 | 用途 | 关键配置 |
|------|------|------|----------|
| Spring Boot | 3.1.12 | 后端服务框架 | Java 17, context-path: /api |
| MyBatis Plus | 3.5.5 | ORM 框架 | 多数据源支持 |
| Java | 17 | 后端开发语言 | LTS 版本 |
| MySQL | 8.0+ | ADS 层及业务数据存储 | 4个数据库实例 |
| WebSocket | — | 实时数据推送 | 广播模式 |
| Caffeine | 3.1.8 | 本地缓存 | TTL 配置 |
| Redis | — | 分布式缓存 | 可选配置 |
| Guava | 32.1.2 | 限流工具 | RateLimiter |

### 3.3 前端展示层

| 组件 | 版本 | 用途 | 关键配置 |
|------|------|------|----------|
| Vue.js | 3.4+ | 前端框架 | Composition API |
| TypeScript | 5.4+ | 类型安全 | 严格模式 |
| ECharts | 5.5+ | 数据图表库 | 主题定制 |
| Element Plus | 2.6+ | UI 组件库 | 暗黑模式支持 |
| Pinia | 2.1+ | 状态管理 | 模块化 store |
| Vite | 5.x | 构建工具 | 热更新 |
| Leaflet | 1.9.4 | 地图组件 | 热力图插件 |
| Axios | 1.6.8 | HTTP 客户端 | 拦截器配置 |

### 3.4 数据生产层

| 组件 | 版本 | 用途 | 关键配置 |
|------|------|------|----------|
| Python | 3.8+ | 数据生产脚本 | 类型注解 |
| kafka-python | — | Kafka 生产者客户端 | 速率控制 |
| pandas / pyarrow | — | Parquet 文件读取 | 内存优化 |

---

## 四、模块结构分析

### 4.1 项目目录结构

```
spark_test/
├── data-producer/                    # 数据生产模块 (Python)
│   ├── config/                        # Kafka 连接配置
│   ├── scripts/                      # 生产脚本
│   │   ├── kafka_producer.py         # 主生产者脚本（速率控制 + 断点续传）
│   │   ├── test_kafka_connection.py  # 连接测试
│   │   └── check_parquet_file.py     # 源数据检查
│   └── docs/                         # 模块文档

├── streaming-processor/              # 实时流处理模块 (Flink)
│   ├── src/main/scala/com/taxi/realtime/
│   │   ├── RealtimeOdsJob.scala      # 实时 ODS 落地作业
│   │   ├── RealtimeMetricsJob.scala  # 实时指标计算作业
│   │   ├── config/                   # 配置管理
│   │   ├── model/                    # 数据模型
│   │   ├── quality/                  # 数据质量检测
│   │   ├── sink/                     # 数据输出
│   │   ├── source/                   # 数据源
│   │   └── utils/                    # 工具类
│   ├── src/main/resources/           # 配置文件
│   ├── scripts/                      # 部署脚本
│   └── docs/                         # 模块文档

├── batch-processor/                  # 离线批处理模块 (Spark)
│   ├── src/main/scala/com/taxi/etl/
│   │   ├── ods/                      # ODS 层：数据加载与校验
│   │   ├── dwd/                      # DWD 层：数据清洗与维度关联
│   │   ├── dws/                      # DWS 层：多维聚合宽表
│   │   ├── ads/                      # ADS 层：业务指标输出
│   │   │   ├── base/                 # 基础组件
│   │   │   ├── daily/                # 日级指标
│   │   │   ├── distribution/         # 分布分析
│   │   │   ├── fee/                  # 费用分析
│   │   │   └── traffic/              # 流量分析
│   │   ├── common/                   # 公共组件
│   │   ├── quality/                  # 质量报告
│   │   ├── utils/                    # 工具类
│   │   └── exception/                # 异常处理
│   ├── src/main/resources/           # 配置文件
│   └── docs/                         # 模块文档

├── taxi/
│   ├── taxi-analytics-backend/       # 后端服务模块 (Spring Boot)
│   │   ├── src/main/java/com/taxi/analytics/
│   │   │   ├── common/               # 公共模块
│   │   │   └── modules/              # 业务模块
│   │   │       ├── ai/               # AI 智能查询
│   │   │       ├── analysis/         # 多维数据分析
│   │   │       ├── dashboard/        # 仪表盘 KPI
│   │   │       ├── export/           # 数据导出
│   │   │       ├── map/              # 热力地图
│   │   │       ├── quality/          # 数据质量监控
│   │   │       └── realtime/         # 实时指标 + WebSocket
│   │   ├── src/main/resources/       # 配置文件
│   │   └── docs/                     # 模块文档
│   │
│   └── taxi-analytics-frontend/      # 前端应用模块 (Vue 3)
│       ├── src/
│       │   ├── api/                  # API 接口封装
│       │   ├── components/           # 公共组件
│       │   ├── views/                # 页面视图
│       │   ├── stores/               # Pinia 状态管理
│       │   └── router/               # 路由配置
│       └── docs/                     # 模块文档

├── packages/
│   └── shared-types/                 # 前后端共享类型 (TypeScript)
│       └── src/api/                  # 类型定义

└── docs/                             # 项目级文档
```

### 4.2 模块职责汇总

| 模块 | 技术栈 | 核心职责 | 输出产物 |
|------|--------|----------|----------|
| **data-producer** | Python | 数据采集与生产 | Kafka 消息 |
| **streaming-processor** | Flink/Scala | 实时流处理 | 实时 ODS + 实时指标 |
| **batch-processor** | Spark/Scala | 离线数仓构建 | ODS/DWD/DWS/ADS |
| **taxi-analytics-backend** | Spring Boot | 业务 API 服务 | REST API + WebSocket |
| **taxi-analytics-frontend** | Vue 3 | 数据可视化 | UI 界面 |
| **shared-types** | TypeScript | 类型定义共享 | 类型包 |

---

## 五、数仓分层设计

### 5.1 分层架构

```
┌─────────────────────────────────────────────────────────┐
│                    ADS 层 (MySQL)                        │
│         18 张业务指标表，直接驱动前端 BI 展示             │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    DWS 层 (Hive)                         │
│         6 张主题宽表，按日/小时/区域等维度预聚合          │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    DWD 层 (Hive)                         │
│         1 张事实大宽表，清洗标准化 + 维度关联 + 派生字段  │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    ODS 层 (Hive)                         │
│         2 张原始数据表（yellow / green），保留原始格式    │
└─────────────────────────────────────────────────────────┘
```

### 5.2 各层详细说明

| 层级 | 全称 | 存储格式 | 刷新策略 | 数据质量要求 |
|------|------|----------|----------|-------------|
| **ODS** | Operational Data Store | Hive / ORC+SNAPPY | 批量/实时写入 | 完整性 ≥ 99.9% |
| **DWD** | Data Warehouse Detail | Hive / ORC+SNAPPY | 每日全量覆盖 | 完整性 100%，准确性 ≥ 99.5% |
| **DWS** | Data Warehouse Service | Hive / ORC+SNAPPY | 每日全量覆盖 | 一致性 100% |
| **ADS** | Application Data Store | MySQL | T+1 批量写入 | 完整性 100% |

### 5.3 ODS 层表清单

| 表名 | 数据来源 | 说明 |
|------|----------|------|
| `taxi_trip_green_ods` | Parquet / Kafka | 绿表原始行程数据 |
| `taxi_trip_yellow_ods` | Parquet / Kafka | 黄表原始行程数据 |

### 5.4 DWD 层表清单

| 表名 | 说明 |
|------|------|
| `fact_taxi_trips` | 行程事实大宽表（清洗、标准化、维度关联） |
| `dim_date` | 日期维度表 |
| `dim_location_zone` | 位置维度表 |
| `dim_payment_type` | 支付类型维度表 |
| `dim_vendor` | 供应商维度表 |
| `dim_airport` | 机场维度表 |

### 5.5 DWS 主题宽表（6 张）

| 表名 | 说明 |
|------|------|
| `dws_trip_daily` | 日行程汇总表 |
| `dws_trip_hourly` | 小时行程汇总表 |
| `dws_trip_zone_pickup_daily` | 上车区域热力日汇总表 |
| `dws_trip_zone_dropoff_daily` | 下车区域热力日汇总表 |
| `dws_trip_fee_daily` | 费用结构日汇总宽表 |
| `dws_trip_vendor_daily` | 供应商日汇总表 |

**存储格式**：Hive / ORC+SNAPPY
**分区字段**：dt（日期分区）

### 5.6 ADS 业务指标表（18 张）

**日级指标（6 张）**：
- `analysis_kpi_daily` - 每日 KPI 指标
- `analysis_hourly_distribution` - 小时分布统计
- `analysis_payment_analysis` - 支付方式分析
- `analysis_vendor_analysis` - 供应商分析
- `analysis_weekday_analysis` - 星期分析
- `analysis_airport_analysis` - 机场分析

**分布分析（4 张）**：
- `analysis_distance_distribution` - 距离分布
- `analysis_duration_distribution` - 时长分布
- `analysis_passenger_distribution` - 乘客数分布
- `analysis_revenue_contribution` - 营收贡献

**费用分析（5 张）**：
- `analysis_fee_by_borough` - 行政区费用分析
- `analysis_fee_by_taxi_type` - 车型费用分析
- `analysis_fee_composition` - 费用构成分析
- `analysis_fee_percentage` - 费用占比分析
- `analysis_fee_trend` - 费用趋势分析

**流量分析（3 张）**：
- `analysis_borough_flow` - 行政区流量
- `analysis_pickup_hotspots` - 上车热点
- `analysis_dropoff_hotspots` - 下车热点

---

## 六、核心业务功能

### 6.1 仪表盘（Dashboard）

| 功能 | 说明 | 数据源 |
|------|------|--------|
| **KPI 总览** | 总订单量、总营收、平均车费、平均行程距离 | `analysis_kpi_daily` |
| **趋势分析** | 按天/周/月展示订单量与营收趋势折线图 | `analysis_kpi_daily` |
| **支付结构** | 各支付方式订单占比饼图 | `analysis_payment_analysis` |
| **供应商对比** | 各出租车供应商订单量与营收柱状图对比 | `analysis_vendor_analysis` |

### 6.2 实时监控（Realtime）

| 功能 | 说明 | 数据源 |
|------|------|--------|
| **实时 KPI** | 当前小时订单量、实时营收滚动更新 | WebSocket 推送 |
| **热点区域 TOP N** | 实时统计上车/下车最热门的前 10 个区域 | Flink 窗口聚合 |
| **费用结构实时分布** | 车费、小费、附加费等费用组成实时占比 | Flink 窗口聚合 |
| **数据质量告警** | 异常数据实时推送告警 | MySQL 告警表 |

### 6.3 深度分析（Analysis）

覆盖 10+ 个分析维度：

| 分析维度 | 说明 |
|----------|------|
| **机场分析** | 机场行程占比、高峰时段、平均费用 |
| **区域营收分析** | 各行政区营收贡献排行与区域流向 |
| **费用组成分析** | 车费、MTA税、拥堵费、机场费等费用结构 |
| **小时分布分析** | 24小时出行规律热力分布 |
| **支付方式分析** | 现金/无现金支付趋势与区域差异 |
| **车型分析** | Yellow vs Green 出租车各维度对比 |
| **行程特征分析** | 行程距离、时长、乘客数的分布规律 |
| **供应商分析** | 各供应商服务质量与收入对比 |
| **工作日分析** | 工作日 vs 周末出行模式差异 |

### 6.4 热力地图（Map）

| 功能 | 说明 |
|------|------|
| **区域散点热力图** | 基于高德地图的区域热力渲染 |
| **上下车热点切换** | 支持上车热点 / 下车热点自由切换 |
| **行政区筛选** | 按行政区（Borough）筛选 |
| **时间过滤** | 支持时间段过滤 |

### 6.5 质量检测（Quality）

| 功能 | 说明 |
|------|------|
| **全链路质量报告** | ODS / DWD / DWS / ADS 各层质量评分汇总 |
| **质量指标体系** | 完整性检查、唯一性检查、值域合理性检查 |
| **实时告警记录** | Flink 实时检测异常记录，推送告警至 MySQL |
| **表健康状态** | 各业务指标表数据量、更新时间、质量得分一览 |

### 6.6 AI 智能查询（AI）

| 功能 | 说明 |
|------|------|
| **自然语言输入** | 用中文直接提问，如"2024年1月机场行程的平均小费是多少" |
| **SQL 自动生成** | 大模型理解业务意图，结合 Schema 自动生成可执行 SQL |
| **图表智能推荐** | 根据数据结构自动推荐最适合的可视化方式 |
| **多轮对话** | 支持上下文关联的多轮追问，历史会话持久化 |
| **ETL SQL 生成** | 辅助生成数仓各层的 ETL 处理 SQL |
| **数据倾斜诊断** | 辅助分析 Spark/Flink 作业的数据倾斜问题 |
| **Flink 反压分析** | 智能分析实时作业背压原因 |

---

## 七、数据流转链路

### 7.1 实时通道（秒级/分钟级）

```
Parquet 源文件
    ↓
[data-producer] → Kafka Topic (nyc-yellow-trips / nyc-green-trips)
    ↓
[streaming-processor - Flink]
    ├── RealtimeOdsJob  → 数据清洗 → HDFS/ORC (实时 ODS)
    └── RealtimeMetricsJob → 窗口聚合 → MySQL (实时指标) + 质量告警
```

### 7.2 离线通道（T+1 批处理）

```
Parquet 源文件 / HDFS ODS
    ↓
[batch-processor - Spark]
    ├── ODS 加载   → Hive ODS (原始层)
    ├── DWD 构建   → Hive (明细层：清洗 + 标准化 + 维度关联)
    ├── DWS 构建   → Hive (汇总层：6张主题宽表)
    └── ADS 构建   → MySQL  (应用层：18张业务指标表)
    ↓
[taxi-analytics-backend] → REST API / WebSocket
    ↓
[taxi-analytics-frontend] → 可视化展示
```

### 7.3 批处理执行顺序

```
Step 1: ODS 层加载
  → GreenOdsLoader + YellowOdsLoader
    ↓
Step 2: DWD 层构建
  → DwdLayerBuilder (数据清洗 + 维度关联)
    ↓
Step 3: DWS 层构建
  → DwsLayerBuilder (多维聚合)
    ↓
Step 4: ADS 层构建
  → AdsLayerBuilder (18张业务指标表)
```

---

## 八、配置与部署

### 8.1 后端数据库配置

系统使用 4 个 MySQL 数据库实例：

| 数据库 | 用途 |
|--------|------|
| `nyc_taxi_ads` | ADS 层业务指标数据 |
| `nyc_taxi_realtime` | 实时指标数据 |
| `nyc_taxi_quality` | 数据质量监控数据 |
| `nyc_taxi_ai` | AI 会话与查询历史 |

### 8.2 环境变量配置

```yaml
# 数据库配置
MYSQL_HOST: 192.168.127.102
MYSQL_PORT: 3306
MYSQL_USER: root
MYSQL_PASSWORD: xxx

# AI 配置 (Coze)
COZE_API_URL: https://k376w6hmfv.coze.site/stream_run
COZE_TOKEN: xxx
COZE_SESSION_ID: xxx
COZE_PROJECT_ID: xxx
```

### 8.3 Spark 作业资源配置参考

| 作业 | Executor 数量 | Executor 内存 | Executor CPU |
|------|--------------|--------------|-------------|
| ODS 加载 | 4 | 8 GB | 2 核 |
| DWD 构建 | 6 | 12 GB | 3 核 |
| DWS 构建 | 4 | 10 GB | 2 核 |
| ADS 构建 | 2 | 8 GB | 2 核 |

### 8.4 启动顺序

```bash
# 1. 启动数据生产模块
cd data-producer
python scripts/kafka_producer.py --topic nyc-yellow-trips --rate 100

# 2. 启动实时流处理模块（可选）
cd streaming-processor
flink run -c com.taxi.realtime.RealtimeOdsJob target/flink-realtime-1.0-SNAPSHOT.jar

# 3. 启动离线批处理模块
cd batch-processor
spark-submit --class com.taxi.etl.ods.YellowOdsLoader target/spark-test-1.0-SNAPSHOT.jar
spark-submit --class com.taxi.etl.dwd.DwdLayerBuilder target/spark-test-1.0-SNAPSHOT.jar
spark-submit --class com.taxi.etl.dws.DwsLayerBuilder target/spark-test-1.0-SNAPSHOT.jar
spark-submit --class com.taxi.etl.ads.AdsLayerBuilder target/spark-test-1.0-SNAPSHOT.jar

# 4. 启动后端服务
cd taxi/taxi-analytics-backend
mvn spring-boot:run

# 5. 启动前端应用
cd taxi/taxi-analytics-frontend
npm install
npm run dev
```

### 8.5 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端应用 | http://localhost:5173 | 主应用入口 |
| 后端 API | http://localhost:8080/api | REST API |
| Swagger 文档 | http://localhost:8080/swagger-ui.html | 接口文档 |
| Flink Web UI | http://hadoop102:8081 | 实时作业监控 |

---

## 九、质量保障体系

### 9.1 数据质量检测规则

| 层级 | 检测项 | 阈值 | 处理方式 |
|------|--------|------|----------|
| **ODS** | 源文件对账 | 记录数一致 | 差异告警 |
| **ODS** | 格式校验 | JSON/Parquet 格式正确 | 错误数据入死信表 |
| **DWD** | 空值检查 | 必填字段非空 | 过滤或填充默认值 |
| **DWD** | 范围检查 | 数值在合理范围 | 过滤 |
| **DWD** | 时间校验 | 上车时间 < 下车时间 | 过滤 |
| **DWD** | 金额校验 | fare_amount >= 0 | 过滤 |
| **DWS** | 汇总校验 | 细粒度合计 = 粗粒度 | 差异告警 |
| **DWS** | 趋势检查 | 与历史同期对比 | 异常告警 |

### 9.2 关键监控指标

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| Spark 作业执行时间 | 从启动到完成的总耗时 | > 2 小时 |
| 数据保留率 | （最终记录数 / 原始记录数）× 100% | < 95% |
| 质量得分 | 各层数据质量综合评分 | < 90 分 |
| ADS 构建失败表数 | 18张 ADS 表中构建失败的数量 | > 0 |
| Flink Checkpoint 延迟 | 实时作业检查点完成延迟 | > 30 秒 |

### 9.3 异常处理机制

```
异常数据 → Dead Letter Queue → 人工审查 → 数据修复/丢弃
           ↓
        告警记录 → 质量监控页面展示 → 通知相关人员
```

---

## 十、AI 智能查询模块

### 10.1 技术实现

| 组件 | 说明 |
|------|------|
| **大模型接入** | 支持 Coze 和 DeepSeek 双模型 |
| **DSL 中间层** | 提高 SQL 生成的准确率与稳定性 |
| **SQL 安全检查** | 防止注入攻击，仅允许只读查询 |
| **Schema 自动检索** | 用户无需了解表结构即可提问 |
| **多轮对话** | 支持上下文关联的追问 |

### 10.2 AI 查询流程图

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

### 10.3 AI 功能列表

| 功能 | API 路径 | 说明 |
|------|----------|------|
| 统一查询入口 | `POST /ai/query` | 输入自然语言，返回 SQL 结果和图表配置 |
| ETL SQL 生成 | `POST /ai/gen-etl` | 根据描述生成 Spark SQL ETL 语句 |
| 字段映射推荐 | `POST /ai/map-fields` | 源表到目标表的字段映射 |
| 数据倾斜诊断 | `POST /ai/skew-diagnose` | 分析 Spark 执行计划 |
| Flink 反压诊断 | `POST /ai/flink/backpressure` | 分析 Flink 作业反压 |
| 并行度推荐 | `POST /ai/flink/parallelism` | 推荐 Flink 作业并行度 |
| 会话历史查询 | `GET /ai/sessions/{sessionId}/messages` | 查询历史对话 |

---

## 十一、项目文档清单

### 11.1 项目级文档

| 文档 | 路径 | 说明 |
|------|------|------|
| 项目概述 | `README.md` | 项目总览 |
| 完整分析报告 | `docs/COMPLETE_PROJECT_ANALYSIS.md` | 系统整体架构分析 |
| 数仓分层设计 | `docs/项目-数仓分层设计.md` | 四层架构设计规范 |
| 数据字典 | `docs/项目-数据字典.md` | 所有数据表字段说明 |
| ETL 开发规范 | `docs/项目-ETL开发规范.md` | ETL 开发流程与命名规范 |
| 数仓命名规范 | `docs/项目-数仓命名规范.md` | 表、字段命名规范 |
| 集成指南 | `docs/INTEGRATION_GUIDE.md` | 组件集成与环境配置 |

### 11.2 模块文档

| 模块 | 文档路径 | 核心内容 |
|------|----------|---------|
| data-producer | `data-producer/docs/` | Kafka 生产者实现、速率控制、断点续传 |
| streaming-processor | `streaming-processor/docs/` | Flink Job 设计、窗口策略、质量检测 |
| batch-processor | `batch-processor/docs/` | Spark ETL 全链路、性能优化、数仓建设 |
| taxi-analytics-backend | `taxi/taxi-analytics-backend/docs/` | REST API 设计、AI 查询、WebSocket |
| taxi-analytics-frontend | `taxi/taxi-analytics-frontend/docs/` | Vue 组件设计、图表集成、路由配置 |
| shared-types | `packages/shared-types/docs/` | TypeScript 类型定义规范 |

---

## 十二、总结与展望

### 12.1 项目亮点

1. **Lambda 架构完整实现**：实时流处理与离线批处理并行，兼顾延迟与准确性
2. **Hive 企业级数据仓库**：基于 Hive 的四层数仓架构，支持分区与压缩优化
3. **全链路质量保障**：从 ODS 到 ADS 的多层质量检测体系
4. **AI NL2SQL 能力**：自然语言查询，降低数据分析门槛
5. **前后端类型统一**：通过 shared-types 包消除接口联调中的类型不一致
6. **可扩展性设计**：模块化架构，便于新增分析维度和数据源

### 12.2 技术亮点

| 技术点 | 说明 |
|--------|------|
| **Hive 分区优化** | 按日期分区，减少数据扫描量 |
| **Broadcast Join** | 维度表广播关联，消除 Shuffle |
| **SmartCacheLite** | 自动追踪 DataFrame 缓存状态，防止内存泄漏 |
| **Flink 实时质量检测** | 多维数据质量检测，异常记录路由至 Dead Letter Queue |
| **DSL 中间层** | 提高 SQL 生成准确率，降低大模型幻觉影响 |

### 12.3 未来优化方向

1. **增量更新优化**：实现 DWD/DWS 层增量更新，减少全量扫描
2. **数据治理**：完善数据血缘追踪与元数据管理
3. **弹性伸缩**：基于负载自动调整 Spark/Flink 资源
4. **多租户支持**：支持多部门/多用户的数据隔离
5. **机器学习集成**：引入预测分析与异常检测模型
6. **实时数据湖**：探索 Flink + Hive 实时数据湖方案

---

**文档结束**

> 基于 1100 万条纽约市出租车行程数据 · Lambda 架构 · Spark + Flink + Vue 3 全栈实现