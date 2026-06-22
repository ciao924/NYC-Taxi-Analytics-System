# 纽约市出租车数据分析系统 - 项目分析报告

---

## 📋 文档信息

| 项目 | 说明 |
|------|------|
| **项目名称** | NYC Taxi Analytics System |
| **文档版本** | v1.0 |
| **生成日期** | 2026-05-10 |
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
│  └─────────────────┘     └──────────────┘     └──────────────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

| 模块名称 | 技术栈 | 核心职责 | 代码量估算 |
|----------|--------|----------|------------|
| **data-producer** | Python 3.8+ | 数据生产，模拟实时流 | ~500 LOC |
| **streaming-processor** | Flink 1.17 + Scala | 实时数据处理 | ~3000 LOC |
| **batch-processor** | Spark 3.1 + Scala | 离线批处理 | ~5000 LOC |
| **taxi-analytics-backend** | Spring Boot 3.1 | 后端 API 服务 | ~8000 LOC |
| **taxi-analytics-frontend** | Vue.js | 前端可视化 | ~3000 LOC |
| **shared-types** | TypeScript | 类型定义 | ~500 LOC |

---

## 三、模块详细分析

### 3.1 Data Producer（数据生产模块）

#### 3.1.1 模块定位

将历史 Parquet 数据以可控速率写入 Kafka，模拟实时数据流。

#### 3.1.2 目录结构

```
data-producer/
├── config/
│   ├── __init__.py
│   └── kafka_config.py          # Kafka 配置管理
├── scripts/
│   ├── kafka_producer.py        # 主生产者脚本
│   ├── test_kafka_connection.py # 连接测试
│   ├── test_single_message.py   # 单条消息测试
│   ├── test_batch_send.py       # 批量发送测试
│   ├── check_parquet_file.py    # Parquet 文件校验
│   ├── kafka-producer.service   # 系统服务配置
│   └── run_background.bat       # Windows 后台启动
├── data/                        # Parquet 数据文件目录
├── docs/
│   ├── CHANGELOG.md
│   └── README.md
├── .env.example                 # 环境变量模板
```

#### 3.1.3 核心功能

| 功能 | 说明 |
|------|------|
| **速率控制** | 可配置消息发送速率 |
| **断点续传** | 自动保存发送进度，支持故障恢复 |
| **数据校验** | 过滤无效记录 |
| **双数据类型支持** | Green/Yellow 出租车数据 |

#### 3.1.4 启动方式

```bash
cd data-producer
pip install -r requirements.txt
python scripts/kafka_producer.py --taxi-type green --rate 10
```

---

### 3.2 Streaming Processor（实时流处理模块）

#### 3.2.1 模块定位

从 Kafka 消费实时数据，进行清洗、质量检测和指标计算。

#### 3.2.2 目录结构

```
streaming-processor/
├── src/main/scala/com/taxi/realtime/
│   ├── config/
│   │   └── ConfigManager.scala           # 配置管理
│   ├── model/
│   │   ├── Trip.scala                    # 基础行程模型
│   │   ├── GreenTripRecord.scala         # Green 出租车记录
│   │   ├── OrderMetric.scala             # 订单指标
│   │   ├── HotspotResult.scala           # 热点分析结果
│   │   ├── FeeComposition.scala          # 费用构成
│   │   └── ErrorRecordJava.scala         # 错误记录
│   ├── quality/
│   │   ├── AlertDetector.scala           # 告警检测器
│   │   ├── QualityAggregateFunctions.scala # 质量聚合函数
│   │   └── QualityModels.scala           # 质量模型
│   ├── sink/
│   │   ├── AlertSinkBuilder.scala        # 告警输出
│   │   ├── DeadLetterSinkBuilder.scala   # 死信队列
│   │   ├── MysqlSinkFactory.scala        # MySQL 输出工厂
│   │   ├── QualitySinkBuilder.scala      # 质量数据输出
│   │   └── GreenTripRecordBucketAssigner.scala # 分区分配器
│   ├── source/
│   │   └── KafkaSourceBuilder.scala      # Kafka 数据源
│   ├── serializer/
│   │   └── GreenTripRecordVectorizer.scala # 序列化工具
│   ├── utils/
│   │   ├── Constants.scala               # 常量定义
│   │   ├── DataCleaner.scala             # 数据清洗工具
│   │   ├── DataUtil.scala                # 数据工具类
│   │   ├── JsonDeserializer.scala        # JSON 反序列化
│   │   ├── LoggerUtil.scala              # 日志工具
│   │   └── MetricsCollector.scala        # 指标收集
│   ├── DataCleanFunction.scala           # 数据清洗函数
│   ├── JsonParseFunction.scala           # JSON 解析函数
│   ├── RealtimeOdsJob.scala              # ODS 层写入作业
│   └── RealtimeMetricsJob.scala          # 实时指标计算作业
├── src/main/resources/
│   ├── application.properties            # 主配置
│   ├── application-ods.properties        # ODS 作业配置
│   ├── application-metrics.properties    # 指标作业配置
│   ├── flink-conf.yaml                   # Flink 配置
│   ├── hive-site.xml                     # Hive 配置
│   ├── log4j2.xml                        # 日志配置
│   ├── log4j2.properties                 # 日志属性
│   ├── create_mysql_tables.sql           # MySQL 建表脚本
│   ├── verify_data_cleanup.sql           # 数据清理验证
│   ├── visualization_queries.md          # 可视化查询
│   └── fix_record.md                     # 修复记录
├── scripts/
│   ├── create_hive_ods_table.sql         # Hive ODS 建表
│   ├── deploy.bat                        # Windows 部署
│   ├── restart.bat                       # Windows 重启
│   ├── status.bat                        # Windows 状态查看
│   ├── stop.bat                          # Windows 停止
│   ├── kafka_lag_monitor.sh              # Kafka Lag 监控
│   └── savepoint.sh                      # Savepoint 管理
├── docs/
│   ├── CHANGELOG.md
│   ├── CHANGELOG-20260429.md
│   ├── CHANGELOG-20260506.md
│   ├── CHANGELOG-20260507.md
│   ├── README.md
│   ├── 实时数据质量检测：纽约出租车数据.md
│   └── 最终方案：分两个 Job 实现实时数仓（解耦生产级）.md
├── pom.xml
├── deploy.sh
├── restart.sh
├── status.sh
└── stop.sh
```

#### 3.2.3 核心功能

| 功能模块 | 说明 |
|----------|------|
| **实时数据消费** | Kafka Source 消费 |
| **JSON 解析** | JsonParseFunction |
| **数据清洗** | DataCleanFunction |
| **质量检测** | AlertDetector + QualityAggregateFunctions |
| **实时指标计算** | OrderMetric + HotspotResult |
| **多 Sink 输出** | HDFS、MySQL、告警系统 |

#### 3.2.4 关键技术点

- **Flink 版本**: 1.17.2
- **状态后端**: RocksDB
- **分区策略**: GreenTripRecordBucketAssigner
- **质量告警**: 实时异常检测与告警推送

#### 3.2.5 启动方式

```bash
cd streaming-processor
mvn clean package -DskipTests
flink run -c com.taxi.realtime.RealtimeOdsJob target/flink-realtime-consumer-1.0-SNAPSHOT.jar --config application-ods.properties
```

---

### 3.3 Batch Processor（离线批处理模块）

#### 3.3.1 模块定位

处理历史数据，构建数仓分层（ODS → DWD → DWS → ADS）。

#### 3.3.2 目录结构

```
batch-processor/
├── src/main/scala/com/taxi/etl/
│   ├── ads/                              # 应用数据层
│   │   ├── base/
│   │   │   ├── AdsConstants.scala        # ADS 常量
│   │   │   ├── AdsTableOrchestrator.scala # 表编排器
│   │   │   ├── BaseAdsWriter.scala       # ADS 写入基类
│   │   │   ├── ConsistencyValidator.scala # 一致性校验
│   │   │   └── DwsDataLoader.scala       # DWS 数据加载
│   │   ├── daily/                        # 日报指标
│   │   │   ├── AirportAnalysisBuilder.scala
│   │   │   ├── HourlyDistributionBuilder.scala
│   │   │   ├── KpiDailyBuilder.scala
│   │   │   ├── PaymentAnalysisBuilder.scala
│   │   │   ├── VendorAnalysisBuilder.scala
│   │   │   └── WeekdayAnalysisBuilder.scala
│   │   ├── distribution/                  # 分布分析
│   │   │   ├── DistanceDistributionBuilder.scala
│   │   │   ├── DurationDistributionBuilder.scala
│   │   │   ├── PassengerDistributionBuilder.scala
│   │   │   └── RevenueContributionBuilder.scala
│   │   ├── fee/                           # 费用分析
│   │   │   ├── FeeByBoroughBuilder.scala
│   │   │   ├── FeeByTaxiTypeBuilder.scala
│   │   │   ├── FeeCompositionBuilder.scala
│   │   │   ├── FeePercentageBuilder.scala
│   │   │   └── FeeTrendBuilder.scala
│   │   ├── traffic/                       # 流量分析
│   │   │   ├── BoroughFlowBuilder.scala
│   │   │   ├── DropoffHotspotsBuilder.scala
│   │   │   └── PickupHotspotsBuilder.scala
│   │   └── AdsLayerBuilder.scala          # ADS 层构建入口
│   ├── common/                            # 公共组件
│   │   ├── CacheManager.scala             # 缓存管理
│   │   ├── ConfigManager.scala            # 配置管理
│   │   ├── DataFrameMetrics.scala         # DataFrame 指标
│   │   ├── IcebergTableManager.scala      # 表管理（支持 Hive/Iceberg 双模式）
│   │   ├── JoinStrategyLite.scala         # Join 策略
│   │   ├── MetricsCollector.scala         # 指标收集
│   │   ├── QualityManager.scala           # 质量管理器
│   │   ├── SmartCacheLite.scala           # 智能缓存
│   │   ├── SparkListenerMetrics.scala     # Spark 监听器指标
│   │   ├── SparkSessionFactory.scala      # SparkSession 工厂
│   │   └── Version.scala                  # 版本信息
│   ├── dwd/                               # 明细数据层
│   │   └── DwdLayerBuilder.scala          # DWD 层构建
│   ├── dws/                               # 汇总数据层
│   │   └── DwsLayerBuilder.scala          # DWS 层构建
│   ├── exception/
│   │   └── QualityCheckException.scala    # 质量检查异常
│   ├── models/
│   │   ├── BuildResult.scala              # 构建结果
│   │   ├── JobContext.scala               # 作业上下文
│   │   └── QualityReport.scala            # 质量报告
│   ├── ods/                               # 原始数据层
│   │   ├── GreenOdsLoader.scala           # Green 数据加载
│   │   ├── OdsValidator.scala             # ODS 校验
│   │   └── YellowOdsLoader.scala          # Yellow 数据加载
│   ├── quality/                           # 质量检测
│   │   ├── DwdQualityReporter.scala       # DWD 质量报告
│   │   ├── QualityScoreCalculator.scala   # 质量评分计算
│   │   └── SourceRecordCountCheck.scala   # 源记录数检查
│   └── utils/
│       ├── DeadLetterWriter.scala         # 死信写入
│       ├── MonitorUtils.scala             # 监控工具
│       └── TaxiDataUtils.scala            # 出租车数据工具
├── src/main/java/taxi-analysis/           # Java 辅助脚本
│   ├── CheckAnalysisSchema.scala
│   ├── CsvToHive.scala
│   ├── DataQualityCheck.scala
│   ├── DataUtil.scala
│   ├── DimTableSync.scala
│   ├── DwdDataQualityCheck.scala
│   ├── DwdLayerBuilder.scala
│   ├── ReadDimensionTables.scala
│   └── TaxiAnalysisToMySQL.scala
├── src/main/resources/
│   ├── application.conf                   # 主配置
│   ├── application-dev.conf               # 开发环境配置
│   ├── application-prod.conf              # 生产环境配置
│   ├── hive-site.xml                      # Hive 配置
│   └── log4j.properties                   # 日志配置
├── data/                                  # 测试数据
│   ├── green_tripdata_2025-01.parquet
│   ├── green_tripdata_2025-02.parquet
│   ├── green_tripdata_2025-03.parquet
│   ├── yellow_tripdata_2025-01.parquet
│   ├── yellow_tripdata_2025-02.parquet
│   └── yellow_tripdata_2025-03.parquet
├── docs/
│   ├── CHANGELOG.md
│   └── README.md
├── logs/
│   └── ads_layer.log                      # ADS 层日志
├── pom.xml
└── .gitignore
```

#### 3.3.3 数仓分层设计

| 层级 | 名称 | 存储格式 | 说明 |
|------|------|----------|------|
| **ODS** | 原始数据层 | Hive ORC | 原始数据落地 |
| **DWD** | 明细数据层 | Hive | 清洗后的明细数据 |
| **DWS** | 汇总数据层 | Hive | 主题域汇总 |
| **ADS** | 应用数据层 | MySQL | 面向报表的指标 |

#### 3.3.4 ADS 层分析维度

| 类别 | 分析主题 | 实现类 |
|------|----------|--------|
| **日报指标** | 机场分析 | AirportAnalysisBuilder |
| | 时段分布 | HourlyDistributionBuilder |
| | KPI日报 | KpiDailyBuilder |
| | 支付分析 | PaymentAnalysisBuilder |
| | 运营商分析 | VendorAnalysisBuilder |
| | 工作日分析 | WeekdayAnalysisBuilder |
| **分布分析** | 距离分布 | DistanceDistributionBuilder |
| | 时长分布 | DurationDistributionBuilder |
| | 乘客分布 | PassengerDistributionBuilder |
| | 收入贡献 | RevenueContributionBuilder |
| **费用分析** | 区域费用 | FeeByBoroughBuilder |
| | 车型费用 | FeeByTaxiTypeBuilder |
| | 费用构成 | FeeCompositionBuilder |
| | 费用占比 | FeePercentageBuilder |
| | 费用趋势 | FeeTrendBuilder |
| **流量分析** | 区域流量 | BoroughFlowBuilder |
| | 下车热点 | DropoffHotspotsBuilder |
| | 上车热点 | PickupHotspotsBuilder |

#### 3.3.5 启动方式

```bash
cd batch-processor
mvn clean package -DskipTests
spark-submit --class com.taxi.etl.dwd.DwdLayerBuilder target/taxi-analytics-assembly.jar
```

---

### 3.4 Taxi Analytics Backend（后端服务模块）

#### 3.4.1 模块定位

提供 RESTful API 和 WebSocket 实时推送服务，支撑前端可视化展示。

#### 3.4.2 目录结构

```
taxi/taxi-analytics-backend/
├── src/main/java/com/taxi/analytics/
│   ├── common/                           # 公共组件
│   │   ├── config/
│   │   │   ├── AsyncConfig.scala         # 异步配置
│   │   │   ├── CorsConfig.scala          # 跨域配置
│   │   │   ├── JdbcTemplateConfig.scala  # JDBC 配置
│   │   │   ├── MybatisPlusConfig.scala   # MyBatis Plus 配置
│   │   │   ├── SwaggerConfig.scala       # Swagger 配置
│   │   │   ├── ThreadPoolConfig.scala    # 线程池配置
│   │   │   └── WebMvcConfig.scala        # Web MVC 配置
│   │   ├── controller/
│   │   │   └── HealthController.scala    # 健康检查
│   │   ├── exception/
│   │   │   ├── BusinessException.scala   # 业务异常
│   │   │   └── GlobalExceptionHandler.scala # 全局异常处理
│   │   ├── interceptor/
│   │   │   └── RateLimitInterceptor.scala # 限流拦截器
│   │   ├── result/
│   │   │   ├── PageResult.scala          # 分页结果
│   │   │   ├── Result.scala              # 统一结果封装
│   │   │   └── ResultCode.scala          # 结果码
│   │   └── validation/
│   │       ├── DateRange.scala           # 日期范围校验
│   │       └── DateRangeValidatorImpl.scala # 日期校验实现
│   ├── modules/                          # 业务模块
│   │   ├── ai/                           # AI 分析模块
│   │   │   ├── client/                   # LLM 客户端
│   │   │   │   ├── ChatMessage.scala
│   │   │   │   ├── ChatResponse.scala
│   │   │   │   ├── Choice.scala
│   │   │   │   ├── CozeClient.scala
│   │   │   │   ├── DeepSeekClient.scala
│   │   │   │   ├── LLMClient.scala
│   │   │   │   ├── Message.scala
│   │   │   │   └── Usage.scala
│   │   │   ├── controller/
│   │   │   │   └── AiController.scala
│   │   │   ├── dsl/
│   │   │   │   ├── Dsl.scala
│   │   │   │   └── DslValidator.scala
│   │   │   ├── executor/
│   │   │   │   └── QueryExecutor.scala
│   │   │   ├── guard/
│   │   │   │   └── QueryGuard.scala
│   │   │   ├── intent/
│   │   │   │   ├── Intent.scala
│   │   │   │   ├── IntentClassifier.scala
│   │   │   │   ├── IntentType.scala
│   │   │   │   └── QueryContext.scala
│   │   │   ├── mapper/
│   │   │   │   └── AiMapper.scala
│   │   │   ├── metrics/
│   │   │   │   ├── MetricDef.scala
│   │   │   │   └── MetricRegistry.scala
│   │   │   ├── model/
│   │   │   │   ├── ChartBuilder.scala
│   │   │   │   └── ChartData.scala
│   │   │   ├── parser/
│   │   │   │   └── AiResponseParser.scala
│   │   │   ├── service/
│   │   │   │   ├── AiService.scala
│   │   │   │   ├── AiSessionService.scala
│   │   │   │   ├── SchemaRetriever.scala
│   │   │   │   ├── SqlGenerator.scala
│   │   │   │   └── impl/
│   │   │   │       ├── AiServiceImpl.scala
│   │   │   │       └── AiSessionServiceImpl.scala
│   │   │   ├── sql/
│   │   │   │   └── SqlBuilder.scala
│   │   │   └── dto/                      # AI 相关 DTO
│   │   ├── analysis/                      # 分析模块
│   │   │   ├── controller/
│   │   │   │   └── AnalysisController.scala
│   │   │   ├── mapper/
│   │   │   │   └── AnalysisMapper.scala
│   │   │   ├── service/
│   │   │   │   ├── AnalysisService.scala
│   │   │   │   └── impl/
│   │   │   │       └── AnalysisServiceImpl.scala
│   │   │   └── dto/
│   │   │       ├── AirportStatisticsDTO.scala
│   │   │       └── VendorComparisonDTO.scala
│   │   ├── dashboard/                     # 仪表盘模块
│   │   │   ├── controller/
│   │   │   │   └── DashboardController.scala
│   │   │   ├── mapper/
│   │   │   │   └── DashboardKpiMapper.scala
│   │   │   ├── service/
│   │   │   │   ├── DashboardKpiService.scala
│   │   │   │   └── impl/
│   │   │   │       └── DashboardKpiServiceImpl.scala
│   │   │   ├── entity/
│   │   │   │   └── DashboardKpi.scala
│   │   │   └── dto/
│   │   │       ├── BaseQueryRequest.scala
│   │   │       ├── DashboardQueryRequest.scala
│   │   │       ├── HourlyDistributionDTO.scala
│   │   │       ├── KpiSummaryDTO.scala
│   │   │       ├── PaymentDistributionDTO.scala
│   │   │       ├── TrendDataDTO.scala
│   │   │       └── VendorPerformanceDTO.scala
│   │   ├── export/                        # 导出模块
│   │   │   ├── controller/
│   │   │   │   └── ExportController.scala
│   │   │   ├── service/
│   │   │   │   ├── ExportService.scala
│   │   │   │   └── impl/
│   │   │   │       └── ExportServiceImpl.scala
│   │   │   └── task/
│   │   │       ├── ExportRequest.scala
│   │   │       ├── ExportTask.scala
│   │   │       ├── ExportTaskManager.scala
│   │   │       ├── TaskStatus.scala
│   │   │       └── TaskType.scala
│   │   ├── map/                           # 地图模块
│   │   │   ├── controller/
│   │   │   │   └── MapController.scala
│   │   │   ├── mapper/
│   │   │   │   └── MapHeatmapMapper.scala
│   │   │   ├── service/
│   │   │   │   ├── MapHeatmapService.scala
│   │   │   │   └── impl/
│   │   │   │       └── MapHeatmapServiceImpl.scala
│   │   ├── quality/                       # 质量模块
│   │   │   ├── controller/
│   │   │   │   └── QualityController.scala
│   │   │   ├── mapper/
│   │   │   │   └── QualityMapper.scala
│   │   │   ├── service/
│   │   │   │   ├── QualityService.scala
│   │   │   │   └── impl/
│   │   │   │       └── QualityServiceImpl.scala
│   │   │   ├── task/
│   │   │   │   └── QualityCheckTask.scala
│   │   │   └── dto/
│   │   │       ├── AlertDTO.scala
│   │   │       ├── QualitySummaryDTO.scala
│   │   │       └── TableHealthStatusDTO.scala
│   │   └── realtime/                      # 实时模块
│   │       ├── config/
│   │       │   └── WebSocketConfig.scala
│   │       ├── controller/
│   │       │   └── RealtimeController.scala
│   │       ├── handler/
│   │       │   └── RealtimeWebSocketHandler.scala
│   │       ├── service/
│   │       │   ├── RealtimeService.scala
│   │       │   ├── WebSocketService.scala
│   │       │   └── impl/
│   │       │       └── RealtimeServiceImpl.scala
│   │       └── dto/
│   │           ├── FeeCompositionDTO.scala
│   │           ├── HotspotDTO.scala
│   │           └── RealtimeKpiDTO.scala
│   ├── TaxiAnalyticsApplication.scala     # Spring Boot 启动类
├── src/main/resources/
│   ├── db/
│   │   └── schema.sql                    # 数据库初始化
│   ├── mapper/
│   │   └── AnalysisMapper.xml            # MyBatis 映射文件
│   ├── application.yml                   # 应用配置
│   ├── data.sql                          # 初始化数据
│   └── schema.sql                        # 数据库 schema
├── apache-maven-3.9.6/                   # 内嵌 Maven
├── Reference_Documents/
│   └── realtime_module_fix_log.md        # 修复记录
├── dump.sql                              # 数据库备份
└── pom.xml
```

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

#### 3.4.4 技术特性

| 特性 | 实现方式 |
|------|----------|
| **WebSocket 实时推送** | Spring WebSocket |
| **多数据源** | Dynamic Datasource |
| **限流** | Guava RateLimiter |
| **缓存** | Caffeine + Redis |
| **API 文档** | SpringDoc OpenAPI |

---

### 3.5 Taxi Analytics Frontend（前端模块）

#### 3.5.1 模块定位

提供可视化界面，展示实时监控、数据分析和质量报告。

#### 3.5.2 目录结构

```
taxi/taxi-analytics-frontend/
├── src/
│   ├── api/                              # API 接口定义
│   │   ├── ai.ts
│   │   ├── analysis.ts
│   │   ├── dashboard.ts
│   │   ├── export.ts
│   │   ├── index.ts
│   │   ├── map.ts
│   │   ├── quality.ts
│   │   ├── request.ts
│   │   └── types.ts
│   ├── assets/
│   │   └── styles/
│   │       └── global.scss               # 全局样式
│   ├── App.vue                           # 主应用组件
│   └── main.ts                           # 入口文件
├── docs/
│   ├── ANALYSIS_FIX_LOG.md               # 分析模块修复记录
│   └── HEATMAP_FIX_LOG.md                # 热力图修复记录
├── .env.development                      # 开发环境变量
├── .env.production                       # 生产环境变量
├── .eslintrc.json                        # ESLint 配置
├── .lintstagedrc.json                    # lint-staged 配置
├── .prettierrc.json                      # Prettier 配置
├── husky.config.js                       # Husky 配置
├── package-lock.json
└── package.json
```

---

### 3.6 Shared Types（共享类型模块）

#### 3.6.1 模块定位

提供 TypeScript 类型定义，实现前后端类型共享。

#### 3.6.2 目录结构

```
packages/shared-types/
├── src/
│   ├── api/
│   │   ├── ai.ts
│   │   ├── analysis.ts
│   │   ├── common.ts
│   │   ├── dashboard.ts
│   │   ├── index.ts
│   │   ├── map.ts
│   │   └── quality.ts
│   └── index.ts
├── package.json
└── tsconfig.json
```

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

### 4.2 依赖版本矩阵

| 模块 | Java | Scala | 框架版本 |
|------|------|-------|----------|
| streaming-processor | 1.8 | 2.12.17 | Flink 1.17.2 |
| batch-processor | 1.8 | 2.12.10 | Spark 3.1.3 |
| taxi-analytics-backend | 17 | - | Spring Boot 3.1.12 |

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

### 7.3 告警机制

```
质量异常 → AlertDetector → AlertSink → 告警系统/前端推送
```

---

## 八、部署与运维

### 8.1 部署脚本

| 模块 | 部署脚本 | 用途 |
|------|----------|------|
| streaming-processor | `deploy.sh/bat` | 部署启动 |
| | `restart.sh/bat` | 重启服务 |
| | `stop.sh/bat` | 停止服务 |
| | `status.sh/bat` | 状态查看 |
| | `savepoint.sh` | 保存检查点 |

### 8.2 监控指标

| 指标类型 | 监控内容 |
|----------|----------|
| **Kafka** | 消息 Lag、分区偏移量 |
| **Flink** | 任务状态、吞吐量、延迟 |
| **Spark** | 作业执行时间、资源使用 |
| **数据质量** | 错误率、告警数量 |

---

## 九、项目文档体系

### 9.1 文档目录

```
docs/
├── DolphinScheduler工作流配置.md       # 工作流调度配置
├── ETL开发规范.md                       # ETL 开发规范
├── PROJECT_STRUCTURE.md                 # 项目结构说明
├── 告警配置.md                           # 告警配置说明
├── 数仓分层设计.md                       # 数仓设计文档
├── 数仓命名规范.md                       # 命名规范
├── 数仓高级特性配置.md                   # 高级特性配置
├── 数据字典.md                           # 数据字典
└── 数据生命周期.md                       # 数据生命周期管理
```

### 9.2 模块文档

每个模块包含：
- `README.md`: 模块说明
- `CHANGELOG.md`: 变更日志

---

## 十、总结与建议

### 10.1 项目亮点

1. **Lambda 架构设计**: 实时与离线一体化
2. **完整的数据链路**: 从采集到可视化完整闭环
3. **质量保障体系**: 全链路数据质量检测
4. **模块化设计**: 职责清晰，易于维护
5. **完善的文档**: 规范齐全，便于团队协作

### 10.2 改进建议

1. **测试覆盖**: 增加单元测试和集成测试
2. **CI/CD 流程**: 建立自动化构建部署流程
3. **监控体系**: 完善监控告警可视化
4. **代码规范**: 统一代码风格检查
5. **文档更新**: 保持文档与代码同步

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

**文档版本**: v1.0  
**生成日期**: 2026-05-10  
**分析范围**: 全模块覆盖