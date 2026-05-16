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
│                  │  - 导出任务  - 质量监控             │                 │
│                  └────────────────┬───────────────────┘                 │
└───────────────────────────────────┼───────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼───────────────────────────────────────┐
│                         数据处理层 (Processing)                           │
│   ┌────────────────────────┐    ┌──────────────────────────────┐         │
│   │   streaming-processor  │    │     batch-processor          │         │
│   │   (Apache Flink)       │    │     (Apache Spark)          │         │
│   │   - 实时流处理         │    │     - 离线批处理             │         │
│   │   - 质量检测           │    │     - 数仓构建               │         │
│   │   - 指标计算           │    │     - 数据质量               │         │
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
├── data-producer/                    # 数据生产模块
│   ├── config/                       # 配置文件
│   ├── scripts/                      # Python 脚本
│   └── docs/                         # 模块文档
├── streaming-processor/              # 实时流处理模块
│   ├── src/main/scala/               # Scala 源码
│   ├── src/main/resources/           # 配置资源
│   ├── scripts/                      # 部署脚本
│   └── docs/                         # 模块文档
├── batch-processor/                  # 离线批处理模块
│   ├── src/main/scala/               # Scala 源码
│   ├── src/main/resources/           # 配置资源
│   └── docs/                         # 模块文档
├── taxi/
│   ├── taxi-analytics-backend/       # 后端服务模块
│   │   ├── src/main/java/            # Java 源码
│   │   ├── src/main/resources/       # 配置资源
│   │   └── docs/                     # 模块文档
│   └── taxi-analytics-frontend/      # 前端应用模块
│       ├── src/                      # Vue 源码
│       └── docs/                     # 模块文档
├── packages/
│   └── shared-types/                 # 共享类型定义
│       ├── src/api/                  # TypeScript 类型
│       └── docs/                     # 模块文档
├── docs/                             # 项目级文档
└── README.md                         # 本说明文档
```

## 🔧 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **前端** | Vue.js | 3.4+ | 前端框架 |
| **前端** | Element Plus | 2.6+ | UI 组件库 |
| **前端** | ECharts | 5.5+ | 图表库 |
| **后端** | Spring Boot | 3.2+ | 后端框架 |
| **后端** | MyBatis Plus | 3.5+ | ORM 框架 |
| **实时处理** | Apache Flink | 1.17.2 | 流处理引擎 |
| **离线处理** | Apache Spark | 3.5+ | 批处理引擎 |
| **消息队列** | Apache Kafka | 3.4.0 | 消息中间件 |
| **元数据** | Apache Hive | 3.1.2 | 数据仓库 |
| **数据湖** | Apache Iceberg | 1.5+ | 数据湖存储 |
| **数据库** | MySQL | 8.0+ | 关系型数据库 |

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- Python 3.8+
- Node.js 20+
- Kafka 3.4+
- Hadoop 3.3+ (可选)

### 启动顺序

1. **启动数据生产模块**
   ```bash
   cd data-producer
   pip install -r requirements.txt
   python scripts/kafka_producer.py
   ```

2. **启动实时流处理模块**
   ```bash
   cd streaming-processor
   mvn clean package -DskipTests
   # 启动 ODS 作业
   flink run -c com.taxi.realtime.RealtimeOdsJob target/*.jar
   # 启动指标作业
   flink run -c com.taxi.realtime.RealtimeMetricsJob target/*.jar
   ```

3. **启动离线批处理模块**
   ```bash
   cd batch-processor
   mvn clean package -DskipTests
   spark-submit --class com.taxi.etl.xxx target/*.jar
   ```

4. **启动后端服务**
   ```bash
   cd taxi/taxi-analytics-backend
   mvn spring-boot:run
   ```

5. **启动前端应用**
   ```bash
   cd taxi/taxi-analytics-frontend
   npm install
   npm run dev
   ```

## 📚 文档结构

| 文档类型 | 路径 | 说明 |
|----------|------|------|
| 项目概述 | README.md | 本文件 |
| 完整分析报告 | docs/项目-完整项目分析报告.md | 项目整体分析 |
| 数仓设计 | docs/项目-数仓分层设计.md | 数据仓库设计 |
| 数据字典 | docs/项目-数据字典.md | 数据表字段说明 |
| 开发规范 | docs/项目-ETL开发规范.md | ETL 开发规范 |

### 模块文档

| 模块 | 文档路径 |
|------|----------|
| 数据生产模块 | data-producer/docs/ |
| 实时流处理模块 | streaming-processor/docs/ |
| 离线批处理模块 | batch-processor/docs/ |
| 后端服务模块 | taxi/taxi-analytics-backend/docs/ |
| 前端应用模块 | taxi/taxi-analytics-frontend/docs/ |
| 共享类型模块 | packages/shared-types/docs/ |

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 提交规范

- **修复日志**: 在各模块 `docs/` 目录创建 `修复日志-日期.md` 文件
- **代码提交**: 遵循 Conventional Commits 规范
- **代码风格**: 遵循项目现有的代码风格

## 📄 许可证

MIT License

---

**项目地址**: https://github.com/ciao924/NYC-Taxi-Analytics-System