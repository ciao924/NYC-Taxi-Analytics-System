# 纽约市出租车数据分析系统 - 完整项目分析报告

---

## 📋 文档信息

| 项目 | 说明 |
|------|------|
| **项目名称** | NYC Taxi Analytics System |
| **文档版本** | v2.0 |
| **生成日期** | 2026-05-14 |
| **分析范围** | 全模块覆盖 |

---

## 一、项目概述

本项目是一个完整的**纽约市出租车数据分析系统**，采用**Lambda架构**设计，包含**实时流处理**和**离线批处理**两条数据链路，实现了数据采集、清洗、质量检测、指标计算和可视化分析的完整闭环。

### 1.1 系统定位

- **业务领域**: 出租车运营数据分析
- **数据来源**: 纽约市 Taxi & Limousine Commission (TLC) 公开数据集
- **核心价值**: 提供实时监控、业务分析、决策支持能力

### 1.2 架构特点

| 特性 | 描述 |
|------|------|
| **实时+离线一体化** | Flink 实时流 + Spark 离线批处理 |
| **Lambda架构** | 实时层与离线层分离，统一服务层 |
| **数据仓库方案** | 基于 Hive 构建企业级数据仓库 |
| **质量保障** | 全链路数据质量检测与告警机制 |
| **AI赋能** | 支持自然语言查询，智能生成SQL和图表 |

---

## 二、系统架构

### 2.1 整体架构图

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
│  │   DWD/DWS Layer │────▶│   Hive       │────▶│   MySQL Dashboard   │     │
│  │  (Hive Tables)  │     │  (Data Lake) │     │   (BI Reporting)    │     │
│  └────────┬────────┘     └──────────────┘     └──────────────────────┘     │
│           │                                                                │
│           ▼                                                                │
│  ┌─────────────────┐     ┌─────────────────┐                               │
│  │  Spring Boot    │────▶│   Vue 3 UI      │                               │
│  │   Backend API   │     │   Dashboard     │                               │
│  │  + AI Assistant │     │                 │                               │
│  └─────────────────┘     └─────────────────┘                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

| 模块名称 | 技术栈 | 核心职责 | 代码量估算 | 状态 |
|----------|--------|----------|------------|------|
| **data-producer** | Python 3.8+ | 数据生产，模拟实时流 | ~500 LOC | ✅ |
| **streaming-processor** | Flink 1.17 + Scala | 实时数据处理 | ~3000 LOC | ✅ |
| **batch-processor** | Spark 3.1 + Scala | 离线批处理 | ~5000 LOC | ✅ |
| **taxi-analytics-backend** | Spring Boot 3.1 | 后端 API 服务 | ~8000 LOC | ✅ |
| **taxi-analytics-frontend** | Vue.js | 前端可视化 | ~3000 LOC | ✅ |
| **shared-types** | TypeScript | 类型定义 | ~500 LOC | ✅ |

---

## 三、模块详细分析

### 3.1 Data Producer（数据生产模块）

#### 3.1.1 模块定位
将历史 Parquet 数据以可控速率写入 Kafka，模拟实时数据流。

#### 3.1.2 目录结构
```
data-producer/
├── config/kafka_config.py        # Kafka 配置管理
├── scripts/kafka_producer.py     # 主生产者脚本
├── scripts/test_kafka_connection.py
├── scripts/test_batch_send.py
└── .env.example                  # 环境变量模板
```

#### 3.1.3 核心功能
| 功能 | 说明 |
|------|------|
| **速率控制** | 可配置消息发送速率 |
| **断点续传** | 自动保存发送进度 |
| **数据校验** | 过滤无效记录 |
| **双数据类型支持** | Green/Yellow 出租车数据 |

---

### 3.2 Streaming Processor（实时流处理模块）

#### 3.2.1 模块定位
从 Kafka 消费实时数据，进行清洗、质量检测和指标计算。

#### 3.2.2 核心作业

**RealtimeOdsJob** - 实时 ODS 层写入作业：
- 从 Kafka 消费数据
- JSON 解析和数据清洗
- 时间戳校验和过滤
- 按 taxi_type 分流（green/yellow）
- 写入 HDFS ODS（ORC + SNAPPY 压缩）
- 分区策略：`year=YYYY/month=MM`

**核心配置**：
- Checkpoint 间隔：120秒
- 状态后端：HashMapStateBackend
- 重启策略：固定延迟重启（3次，间隔10秒）

#### 3.2.3 最新变更（v1.0.0）

| 类别 | 内容 |
|------|------|
| **新增功能** | 数据质量检测模块、告警配置动态加载、费用构成分析 |
| **修复问题** | Flink Lambda 泛型类型推断、Processing Time 窗口无输出、MySQL `rank` 关键字冲突 |
| **配置优化** | Checkpoint 存储改为 HDFS、Consumer Group ID 可配置 |

---

### 3.3 Batch Processor（离线批处理模块）

#### 3.3.1 模块定位
处理历史数据，构建数仓分层（ODS → DWD → DWS → ADS）。

#### 3.3.2 DWD 层构建流程

**DwdLayerBuilder** 核心处理流程：

1. **阶段1**: 读取 ODS 数据并合并（Green + Yellow）
2. **阶段2**: 分层空值处理（必填字段过滤 + 可选字段填充）
3. **阶段3**: 值域过滤（距离、金额、乘客数、时长限制）
4. **阶段4**: 维度关联（支付类型、供应商、区域、费率等）
5. **阶段5**: 逻辑修复（现金支付小费归零、机场费逻辑修正）
6. **阶段6**: 派生字段（trip_id、日期维度、行程类型）
7. **阶段7**: 质量检测（QualityManager.fullCheck）
8. **阶段8**: 原子写入（IcebergTableManager.atomicOverwrite，支持 Hive/Iceberg 双模式）

#### 3.3.3 ADS 层分析维度

| 类别 | 分析主题 | 实现类 |
|------|----------|--------|
| **日报指标** | KPI日报、时段分布、支付分析、运营商分析 | KpiDailyBuilder 等 |
| **分布分析** | 距离分布、时长分布、乘客分布、收入贡献 | DistanceDistributionBuilder 等 |
| **费用分析** | 区域费用、车型费用、费用构成、费用趋势 | FeeCompositionBuilder 等 |
| **流量分析** | 区域流量、下车热点、上车热点 | BoroughFlowBuilder 等 |

---

### 3.4 Taxi Analytics Backend（后端服务模块）

#### 3.4.1 模块定位
提供 RESTful API 和 WebSocket 实时推送服务，支撑前端可视化展示。

#### 3.4.2 数据库配置

后端配置了**三个数据源**：

| 数据源 | 数据库名 | 用途 |
|--------|----------|------|
| ads | nyc_taxi_ads | 指标数据存储 |
| quality | nyc_taxi_quality | 质量检测数据 |
| ai | nyc_taxi_ai | AI 模块数据 |

#### 3.4.3 模块功能矩阵

| 模块 | 功能 | API 数量 |
|------|------|----------|
| **ai** | AI 查询、SQL 生成、智能分析 | ~15 |
| **analysis** | 深度分析、对比分析 | ~10 |
| **dashboard** | KPI 仪表盘、指标汇总 | ~12 |
| **export** | 数据导出、异步任务 | ~8 |
| **map** | 热力图、地理分析 | ~6 |
| **quality** | 质量监控、告警管理 | ~10 |
| **realtime** | 实时推送、WebSocket | ~8 |

#### 3.4.4 AI 智能助手模块修复记录

| 日期 | 修复内容 |
|------|---------|
| 2026-05-14 | 修复 Dsl 类反序列化问题，添加 `@JsonIgnoreProperties` |
| 2026-05-14 | 修复智能体响应解析逻辑，正确区分 SQL/DSL |
| 2026-05-14 | 修复 Coze API SSE 流式响应解析 |
| 2026-05-14 | AI 智能助手模块架构重构（智能路由升级） |
| 2026-05-14 | 修复 API 路径配置重复问题 |
| 2026-05-13 | 修复 SQL 生成不准确和可视化图表问题 |

---

## 四、技术栈汇总

### 4.1 核心技术栈

| 分类 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **语言** | Java | 1.8/17 | 后端服务、批处理 |
| | Scala | 2.12 | Flink/Spark 开发 |
| | Python | 3.8+ | 数据生产 |
| | TypeScript | - | 前端类型定义 |
| **实时处理** | Apache Flink | 1.17.2 | 流处理引擎 |
| **离线处理** | Apache Spark | 3.1.3 | 批处理引擎 |
| **消息队列** | Apache Kafka | 3.4.0+ | 实时消息传递 |
| **数据存储** | Apache Hive | 3.1.2+ | 元数据管理 |
| | Apache Hive | 3.1.2 | 数据仓库 |
| | MySQL | 8.0+ | 指标存储 |
| **后端框架** | Spring Boot | 3.1.12 | RESTful 服务 |
| **ORM** | MyBatis Plus | 3.5.5 | 数据库操作 |

---

## 五、数据链路分析

### 5.1 实时链路

```
Parquet 文件 → Data Producer → Kafka Topic → Flink Streaming → HDFS ODS → DWD/DWS
```

**数据流向**:
1. **数据生产**: Python 读取 Parquet，按速率写入 Kafka
2. **实时消费**: Flink 从 Kafka 消费消息
3. **数据清洗**: JsonParseFunction + DataCleanFunction
4. **质量检测**: AlertDetector 实时检测异常
5. **指标计算**: RealtimeMetricsJob 计算实时指标
6. **数据输出**: 写入 HDFS ODS 和 MySQL

### 5.2 离线链路

```
Parquet 文件 → Spark Batch → Hive ODS → DWD/DWS → MySQL Dashboard
```

**数据流向**:
1. **数据读取**: Spark 读取 Parquet 文件
2. **ODS 加载**: GreenOdsLoader / YellowOdsLoader
3. **DWD 构建**: DwdLayerBuilder 清洗转换
4. **DWS 汇总**: DwsLayerBuilder 聚合计算
5. **ADS 生成**: AdsLayerBuilder 多维度分析
6. **指标输出**: 写入 Hive 和 MySQL

---

## 六、数仓分层设计

### 6.1 层级架构

| 层级 | 英文名称 | 存储 | 特点 |
|------|----------|------|------|
| **ODS** | Operational Data Store | Hive ORC | 原始数据，保留完整历史 |
| **DWD** | Data Warehouse Detail | Hive | 清洗后明细，支持变更追踪 |
| **DWS** | Data Warehouse Summary | Hive | 主题域汇总，按维度聚合 |
| **ADS** | Application Data Service | MySQL | 面向报表，低延迟查询 |

### 6.2 表命名规范

| 层级 | 命名模式 | 示例 |
|------|----------|------|
| ODS | `ods_taxi_trip_{类型}` | `ods_taxi_trip_green` |
| DWD | `dwd_taxi_trip_fact` | `dwd_taxi_trip_fact` |
| DWS | `dws_taxi_{主题}_{聚合}` | `dws_taxi_order_metrics` |
| ADS | `ads_{分析主题}_{维度}` | `ads_daily_kpi` |

---

## 七、质量保障体系

### 7.1 质量检测模块

| 模块 | 职责 | 关键组件 |
|------|------|----------|
| **streaming-processor/quality** | 实时质量检测 | AlertDetector, QualityAggregateFunctions |
| **batch-processor/quality** | 离线质量检测 | DwdQualityReporter, QualityScoreCalculator |

### 7.2 质量检测维度

| 检测类型 | 检测内容 | 告警阈值 |
|----------|----------|----------|
| **数据完整性** | 空值率、缺失字段 | > 5% |
| **数据准确性** | 字段格式、业务规则 | 异常记录 > 1% |
| **数据一致性** | 上下游数据核对 | 差异 > 0.1% |
| **数据时效性** | 延迟检测 | > 5 分钟 |

---

## 八、部署与运维

### 8.1 启动命令汇总

| 模块 | 启动命令 |
|------|----------|
| **数据生产** | `python scripts/kafka_producer.py --taxi-type green --rate 10` |
| **实时流处理** | `flink run -c com.taxi.realtime.RealtimeOdsJob target/flink-realtime-consumer-1.0-SNAPSHOT.jar --config application-ods.properties` |
| **离线批处理** | `spark-submit --class com.taxi.etl.dwd.DwdLayerBuilder target/taxi-analytics-assembly.jar` |
| **后端服务** | `mvn spring-boot:run` |

### 8.2 监控指标

| 指标类型 | 监控内容 |
|----------|----------|
| **Kafka** | 消息 Lag、分区偏移量 |
| **Flink** | 任务状态、吞吐量、延迟 |
| **Spark** | 作业执行时间、资源使用 |
| **数据质量** | 错误率、告警数量 |

---

## 九、项目文档体系

```
docs/
├── DolphinScheduler工作流配置.md   # 工作流调度配置
├── ETL开发规范.md                   # ETL 开发规范
├── PROJECT_STRUCTURE.md             # 项目结构说明
├── 告警配置.md                       # 告警配置说明
├── 数仓分层设计.md                   # 数仓设计文档
├── 数仓命名规范.md                   # 命名规范
├── 数仓高级特性配置.md               # 高级特性配置
├── 数据字典.md                       # 数据字典
└── 数据生命周期.md                   # 数据生命周期管理
```

---

## 十、功能验证结果

### 10.1 已完成验证

| 验证项 | 状态 | 说明 |
|--------|------|------|
| 后端编译 | ✅ | `mvn clean compile` 成功 |
| 前端编译 | ✅ | `npm run build` 成功 |
| 核心流程 | ✅ | 自然语言 → DSL → SQL → 数据 → 图表 |
| UI设计 | ✅ | 现代化卡片式布局，响应式设计 |

### 10.2 功能特性

| 特性 | 状态 | 说明 |
|------|------|------|
| 自然语言转DSL | ✅ | 支持指标、维度、时间范围解析 |
| SQL生成 | ✅ | 支持18张ADS层表查询 |
| 图表可视化 | ✅ | 支持折线图、柱状图、饼图、散点图 |
| 响应式图表尺寸 | ✅ | 根据容器宽度动态计算高度 |
| 会话管理 | ✅ | 支持新建、切换、重命名会话 |
| 查询历史 | ✅ | 保存最近20条查询记录 |
| 收藏功能 | ✅ | 支持收藏常用查询 |
| 快捷查询 | ✅ | 内置常用查询示例 |

---

## 十一、总结与建议

### 11.1 项目亮点

1. **Lambda 架构设计**: 实时与离线一体化，支持实时监控和历史分析
2. **完整的数据链路**: 从采集到可视化完整闭环
3. **质量保障体系**: 全链路数据质量检测与告警
4. **AI智能助手**: 支持自然语言查询，自动生成SQL和图表
5. **模块化设计**: 职责清晰，易于维护和扩展
6. **完善的文档**: 规范齐全，便于团队协作

### 11.2 改进建议

| 建议 | 说明 | 优先级 |
|------|------|--------|
| **测试覆盖** | 增加单元测试和集成测试 | 高 |
| **CI/CD 流程** | 建立自动化构建部署流程 | 高 |
| **监控体系** | 完善监控告警可视化 | 中 |
| **代码规范** | 统一代码风格检查 | 中 |
| **文档更新** | 保持文档与代码同步 | 低 |

---

## 📌 附录

### 文件统计

| 类型 | 数量 |
|------|------|
| Java 文件 | 约 100 个 |
| Scala 文件 | 约 80 个 |
| Python 文件 | 约 10 个 |
| 配置文件 | 约 30 个 |
| 文档文件 | 约 25 个 |

### 代码量估算

| 模块 | 代码量 (LOC) |
|------|-------------|
| streaming-processor | ~3000 |
| batch-processor | ~5000 |
| taxi-analytics-backend | ~8000 |
| taxi-analytics-frontend | ~3000 |
| **总计** | **~19000** |

---

**文档版本**: v2.0  
**生成日期**: 2026-05-14  
**分析范围**: 全模块覆盖