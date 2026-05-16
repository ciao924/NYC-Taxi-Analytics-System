# NYC Taxi Analytics System

纽约市出租车数据分析系统 - 实时 + 离线一体化数据仓库与分析平台

## 📊 项目概述

本系统是一个综合性的**实时 + 离线**数据仓库与分析平台，通过对纽约市 Yellow Taxi 和 Green Taxi 数据的处理、分析和可视化，为业务决策提供数据支撑。

### 🎯 核心目标

| 目标 | 描述 | 状态 |
|------|------|------|
| **实时数据处理** | 消费 Kafka 实时数据，完成 ODS 落地、指标计算、质量检测 | ✅ |
| **离线数据仓库** | 构建 ODS → DWD → DWS → ADS 完整分层数仓 | ✅ |
| **数据质量保障** | 全链路数据质量监控、异常检测、告警推送 | ✅ |
| **智能查询分析** | 支持自然语言查询、SQL 生成、图表可视化 | ✅ |
| **数据可视化** | 提供多维度图表、热力图、实时指标展示 | ✅ |

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           数据可视化层 (UI)                              │
│                  ┌────────────────────────────────────┐                 │
│                  │  taxi-analytics-frontend (Vue 3)  │                 │
│                  │  - 仪表盘   - 数据分析   - 实时监控  │                 │
│                  │  - 热力图   - 质量监控   - AI 智能   │                 │
│                  └────────────────┬───────────────────┘                 │
└───────────────────────────────────┼───────────────────────────────────────┘
                                    │ HTTP/WebSocket
┌───────────────────────────────────┼───────────────────────────────────────┐
│                           业务服务层 (Backend)                            │
│                  ┌────────────────────────────────────┐                 │
│                  │ taxi-analytics-backend (Spring Boot)│                 │
│                  │  - REST API  - WebSocket  - AI 查询  │                 │
│                  │  - 数据分析  - 导出任务  - 质量监控  │                 │
│                  └────────────────┬───────────────────┘                 │
└───────────────────────────────────┼───────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼───────────────────────────────────────┐
│                         数据处理层 (Processing)                           │
│   ┌────────────────────────┐    ┌──────────────────────────────┐         │
│   │   streaming-processor  │    │     batch-processor          │         │
│   │   (Apache Flink 1.17)  │    │     (Apache Spark 3.1)       │         │
│   │   - 实时流处理         │    │     - 离线批处理             │         │
│   │   - ODS 落地           │    │     - 数仓分层构建           │         │
│   │   - 实时指标计算       │    │     - ADS 指标输出           │         │
│   │   - 质量检测           │    │     - 数据质量检查           │         │
│   └────────────────────────┘    └──────────────────────────────┘         │
└───────────────────────────────────┼───────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼───────────────────────────────────────┐
│                           数据采集层 (Ingestion)                          │
│                  ┌────────────────────────────────────┐                 │
│                  │     data-producer (Python)        │                 │
│                  │     - Kafka 生产者                │                 │
│                  │     - 速率控制                    │                 │
│                  │     - 断点续传                    │                 │
│                  └────────────────────────────────────┘                 │
└─────────────────────────────────────────────────────────────────────────┘
```

## 📁 项目结构

```
spark_test/
├── data-producer/                    # 数据生产模块 (Python)
│   ├── config/                       # Kafka 配置
│   ├── scripts/                      # 生产者脚本
│   │   ├── kafka_producer.py        # 主生产者
│   │   ├── test_kafka_connection.py # 连接测试
│   │   └── check_parquet_file.py    # 数据检查
│   └── docs/                         # 模块文档
├── streaming-processor/              # 实时流处理模块 (Flink)
│   ├── src/main/scala/com/taxi/realtime/
│   │   ├── RealtimeOdsJob.scala     # ODS 落地作业
│   │   ├── RealtimeMetricsJob.scala # 指标计算作业
│   │   ├── config/                  # 配置管理
│   │   ├── model/                   # 数据模型
│   │   ├── quality/                 # 质量检测
│   │   ├── sink/                    # 数据输出
│   │   ├── source/                  # 数据输入
│   │   └── utils/                   # 工具类
│   ├── src/main/resources/           # 配置资源
│   ├── scripts/                      # 部署脚本
│   └── docs/                         # 模块文档
├── batch-processor/                  # 离线批处理模块 (Spark)
│   ├── src/main/scala/com/taxi/etl/
│   │   ├── ods/                     # ODS 层处理
│   │   ├── dwd/                     # DWD 层处理
│   │   ├── dws/                     # DWS 层处理
│   │   ├── ads/                     # ADS 层处理
│   │   │   ├── daily/              # 日级指标
│   │   │   ├── fee/                # 费用分析
│   │   │   ├── traffic/            # 流量分析
│   │   │   └── distribution/       # 分布分析
│   │   ├── common/                  # 公共组件
│   │   ├── quality/                 # 质量检测
│   │   └── utils/                   # 工具类
│   ├── src/main/resources/           # 配置资源
│   └── docs/                         # 模块文档
├── taxi/
│   ├── taxi-analytics-backend/       # 后端服务模块 (Spring Boot)
│   │   ├── src/main/java/com/taxi/analytics/
│   │   │   ├── common/              # 公共模块
│   │   │   │   ├── config/         # 配置类
│   │   │   │   ├── exception/      # 异常处理
│   │   │   │   └── result/         # 统一返回
│   │   │   └── modules/             # 业务模块
│   │   │       ├── ai/             # AI 智能查询
│   │   │       ├── analysis/       # 数据分析
│   │   │       ├── dashboard/      # 仪表盘
│   │   │       ├── export/         # 数据导出
│   │   │       ├── map/            # 热力地图
│   │   │       ├── quality/        # 质量监控
│   │   │       └── realtime/       # 实时监控
│   │   ├── src/main/resources/       # 配置资源
│   │   └── docs/                     # 模块文档
│   └── taxi-analytics-frontend/      # 前端应用模块 (Vue 3)
│       ├── src/
│       │   ├── api/                 # API 接口
│       │   ├── components/          # 组件
│       │   │   ├── ai/             # AI 组件
│       │   │   └── analysis/       # 分析组件
│       │   ├── views/               # 页面视图
│       │   │   ├── ai/             # AI 查询
│       │   │   ├── analysis/       # 数据分析
│       │   │   ├── dashboard/      # 仪表盘
│       │   │   ├── map/            # 热力地图
│       │   │   ├── quality/        # 质量监控
│       │   │   └── realtime/       # 实时监控
│       │   ├── stores/              # 状态管理
│       │   └── router/              # 路由配置
│       └── docs/                     # 模块文档
├── packages/
│   └── shared-types/                 # 共享类型定义 (TypeScript)
│       ├── src/api/                  # API 类型定义
│       │   ├── common.ts            # 通用类型
│       │   ├── dashboard.ts         # 仪表盘类型
│       │   ├── ai.ts                # AI 类型
│       │   ├── analysis.ts          # 分析类型
│       │   ├── quality.ts           # 质量类型
│       │   └── map.ts               # 地图类型
│       └── docs/                     # 模块文档
├── docs/                             # 项目级文档
│   ├── 项目-完整项目分析报告.md
│   ├── 项目-数仓分层设计.md
│   ├── 项目-数据字典.md
│   ├── 项目-ETL开发规范.md
│   └── ...
└── README.md                         # 本说明文档
```

## 🔧 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **前端** | Vue.js | 3.4+ | 前端框架 |
| **前端** | TypeScript | 5.4+ | 类型安全 |
| **前端** | Element Plus | 2.6+ | UI 组件库 |
| **前端** | ECharts | 5.5+ | 图表库 |
| **前端** | Pinia | 2.1+ | 状态管理 |
| **后端** | Spring Boot | 3.1.12 | 后端框架 |
| **后端** | MyBatis Plus | 3.5.5 | ORM 框架 |
| **后端** | Java | 17 | 开发语言 |
| **实时处理** | Apache Flink | 1.17.2 | 流处理引擎 |
| **离线处理** | Apache Spark | 3.1.3 | 批处理引擎 |
| **消息队列** | Apache Kafka | 3.4.0 | 消息中间件 |
| **元数据** | Apache Hive | 3.1.2 | 数据仓库 |
| **数据湖** | Apache Iceberg | 1.1.0 | 数据湖存储 |
| **数据库** | MySQL | 8.0+ | 关系型数据库 |
| **缓存** | Redis | - | 分布式缓存 |
| **数据生产** | Python | 3.8+ | 数据采集 |

## � 数据流说明

### 实时数据流

```
Parquet 文件 → Kafka → Flink → HDFS(ODS) / MySQL(指标)
                      ↓
                   质量检测 → 告警
```

### 离线数据流

```
HDFS(ODS) → Spark(DWD) → Spark(DWS) → Spark(ADS) → MySQL
              ↓            ↓            ↓
           质量检查     质量检查     质量检查
```

### 数仓分层

| 层级 | 说明 | 存储位置 | 数据格式 |
|------|------|----------|----------|
| **ODS** | 原始数据层 | Hive ODS 库 | ORC + SNAPPY |
| **DWD** | 明细数据层 | Hive DWD 库 | ORC + SNAPPY |
| **DWS** | 汇总数据层 | Hive DWS 库 | ORC + SNAPPY |
| **ADS** | 应用数据层 | MySQL | 表格 |

## �🚀 快速开始

### 环境要求

| 组件 | 版本要求 |
|------|----------|
| JDK | 17+ |
| Maven | 3.9+ |
| Python | 3.8+ |
| Node.js | 20+ |
| Kafka | 3.4+ |
| Hadoop | 3.3+ (可选) |
| MySQL | 8.0+ |
| Redis | 6.0+ (可选) |

### 启动顺序

#### 1. 启动数据生产模块

```bash
cd data-producer
pip install -r requirements.txt
python scripts/kafka_producer.py --topic taxi_trip_green --rate 100
```

#### 2. 启动实时流处理模块

```bash
cd streaming-processor
mvn clean package -DskipTests

# 启动 ODS 作业（数据落地到 HDFS）
flink run -c com.taxi.realtime.RealtimeOdsJob target/flink-realtime-1.0-SNAPSHOT.jar

# 启动指标作业（实时计算指标）
flink run -c com.taxi.realtime.RealtimeMetricsJob target/flink-realtime-1.0-SNAPSHOT.jar
```

#### 3. 启动离线批处理模块

```bash
cd batch-processor
mvn clean package -DskipTests

# DWD 层构建
spark-submit --class com.taxi.etl.dwd.DwdLayerBuilder target/spark-test-1.0-SNAPSHOT.jar

# DWS 层构建
spark-submit --class com.taxi.etl.dws.DwsLayerBuilder target/spark-test-1.0-SNAPSHOT.jar

# ADS 层构建
spark-submit --class com.taxi.etl.ads.AdsLayerBuilder target/spark-test-1.0-SNAPSHOT.jar
```

#### 4. 启动后端服务

```bash
cd taxi/taxi-analytics-backend
mvn spring-boot:run
```

#### 5. 启动前端应用

```bash
cd taxi/taxi-analytics-frontend
npm install
npm run dev
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端应用 | http://localhost:5173 |
| 后端 API | http://localhost:8080/api |
| Swagger 文档 | http://localhost:8080/swagger-ui.html |
| Flink Web UI | http://hadoop102:8081 |

## 🌐 功能模块

### 📊 仪表盘（Dashboard）

- KPI 指标卡片：总订单量、总营收、平均车费、平均距离
- 趋势图表：按天/周/月展示订单和营收趋势
- 支付分布：饼图展示各支付方式占比
- 供应商表现：柱状图对比各供应商数据

### ⚡ 实时监控（Realtime）

- 实时 KPI：当前小时订单量、实时营收
- 热点区域：Top10 上车/下车热点
- 费用组成：实时费用结构分布

### 📈 数据分析（Analysis）

- **基础分析**：机场分析、区域营收、费用组成、热点分析、小时分布、支付分析、车型分析、行程特征、供应商分析、工作日分析
- **多维分析**：支持多维度交叉分析（机场×区域、时间×支付方式等）

### 🗺️ 热力地图（Map）

- 基于高德地图的散点热力图
- 支持上车/下车热点切换
- 区域筛选功能

### 🔍 质量监控（Quality）

- 数据质量概览
- 表健康状态
- 告警记录

### 🤖 AI 智能查询（AI）

- 自然语言转 SQL
- 意图识别
- 图表自动生成
- 多轮对话支持
- ETL SQL 生成
- 数据倾斜诊断
- Flink 反压分析

## 📚 文档结构

### 项目级文档

| 文档类型 | 路径 | 说明 |
|----------|------|------|
| 项目概述 | README.md | 本文件 |
| 完整分析报告 | docs/项目-完整项目分析报告.md | 项目整体分析 |
| 数仓设计 | docs/项目-数仓分层设计.md | 数据仓库设计 |
| 数据字典 | docs/项目-数据字典.md | 数据表字段说明 |
| 开发规范 | docs/项目-ETL开发规范.md | ETL 开发规范 |
| 命名规范 | docs/项目-数仓命名规范.md | 数仓命名规范 |
| 集成指南 | docs/INTEGRATION_GUIDE.md | 集成指南 |

### 模块文档

| 模块 | 文档路径 | 说明 |
|------|----------|------|
| 数据生产模块 | data-producer/docs/README.md | Kafka 生产者 |
| 实时流处理模块 | streaming-processor/docs/README.md | Flink 实时处理 |
| 离线批处理模块 | batch-processor/docs/README.md | Spark 离线处理 |
| 后端服务模块 | taxi/taxi-analytics-backend/docs/README.md | Spring Boot 服务 |
| 前端应用模块 | taxi/taxi-analytics-frontend/docs/README.md | Vue 前端应用 |
| 共享类型模块 | packages/shared-types/docs/README.md | TypeScript 类型 |

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 提交规范

- **修复日志**: 在各模块 `docs/` 目录创建 `修复日志-日期.md` 文件
- **代码提交**: 遵循 Conventional Commits 规范
  - `feat`: 新功能
  - `fix`: 修复 Bug
  - `docs`: 文档更新
  - `refactor`: 重构代码
  - `test`: 测试相关
- **代码风格**: 遵循项目现有的代码风格

### 分支管理

- `main`: 主分支，稳定版本
- `develop`: 开发分支
- `feature/*`: 功能分支
- `hotfix/*`: 热修复分支

## 📄 License

MIT License

---

**项目地址**: https://github.com/ciao924/NYC-Taxi-Analytics-System
