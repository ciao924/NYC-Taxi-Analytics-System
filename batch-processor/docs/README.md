# 离线批处理模块

基于 Apache Spark 的离线数据处理引擎。

## 📌 模块定位

本模块是数据处理 Pipeline 的**离线处理层**，负责处理历史数据，构建完整的数仓分层结构（ODS → DWD → DWS → ADS）。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **ODS 加载** | 从 Parquet 文件加载原始数据到 Hive ODS 层，支持 Green/Yellow Taxi |
| **DWD 构建** | 构建明细数据层，进行数据清洗、标准化、维度统一 |
| **DWS 构建** | 构建汇总数据层，计算聚合指标（按天/小时/区域等维度） |
| **ADS 输出** | 将聚合指标写入 MySQL，供 BI 报表使用 |
| **维度表同步** | 管理维度表数据，支持缓慢变化维度（SCD） |
| **数据质量检查** | 对处理后的数据进行质量校验，生成质量报告 |
| **元数据管理** | 管理数据表结构和字段信息 |
| **一致性校验** | 跨层数据一致性验证 |
| **智能缓存** | 小表广播优化、AQE 倾斜处理 |
| **Iceberg 支持** | 支持 Iceberg 数据湖格式 |

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Apache Spark | 3.1.3 | 离线批处理引擎 |
| Apache Hive | 3.1.2 | 元数据管理 |
| Apache Iceberg | 1.1.0 | 数据湖存储 |
| Scala | 2.12.10 | 开发语言 |
| Maven | 3.6+ | 构建工具 |
| MySQL | 8.0+ | 应用层存储 |
| Hadoop | 3.3.1 | 分布式存储 |

## 📁 目录结构

```
batch-processor/
├── src/main/scala/com/taxi/etl/
│   ├── ods/                          # ODS 层处理
│   │   ├── GreenOdsLoader.scala      # Green Taxi ODS 加载
│   │   ├── YellowOdsLoader.scala     # Yellow Taxi ODS 加载
│   │   └── OdsValidator.scala        # ODS 数据验证
│   ├── dwd/                          # DWD 层处理
│   │   ├── DwdLayerBuilder.scala     # DWD 层构建器
│   │   └── ReadDimensionTables.scala # 维度表读取
│   ├── dws/                          # DWS 层处理
│   │   └── DwsLayerBuilder.scala     # DWS 层构建器
│   ├── ads/                          # ADS 层处理
│   │   ├── AdsLayerBuilder.scala     # ADS 层构建器
│   │   ├── base/                     # 基础组件
│   │   │   ├── AdsConstants.scala     # ADS 常量定义
│   │   │   ├── AdsTableOrchestrator.scala  # 表编排器
│   │   │   ├── BaseAdsWriter.scala   # 基础写入器
│   │   │   ├── ConsistencyValidator.scala  # 一致性校验
│   │   │   └── DwsDataLoader.scala   # DWS 数据加载器
│   │   ├── daily/                    # 日级指标
│   │   │   ├── AirportAnalysisBuilder.scala    # 机场分析
│   │   │   ├── HourlyDistributionBuilder.scala # 小时分布
│   │   │   ├── KpiDailyBuilder.scala           # 日级 KPI
│   │   │   ├── PaymentAnalysisBuilder.scala     # 支付分析
│   │   │   ├── VendorAnalysisBuilder.scala      # 供应商分析
│   │   │   └── WeekdayAnalysisBuilder.scala      # 工作日分析
│   │   ├── distribution/             # 分布分析
│   │   │   ├── DistanceDistributionBuilder.scala  # 距离分布
│   │   │   ├── DurationDistributionBuilder.scala # 时长分布
│   │   │   ├── PassengerDistributionBuilder.scala # 乘客分布
│   │   │   └── RevenueContributionBuilder.scala   # 营收贡献
│   │   ├── fee/                     # 费用分析
│   │   │   ├── FeeByBoroughBuilder.scala    # 各区费用
│   │   │   ├── FeeByTaxiTypeBuilder.scala   # 各类型费用
│   │   │   ├── FeeCompositionBuilder.scala  # 费用组成
│   │   │   ├── FeePercentageBuilder.scala   # 费用占比
│   │   │   └── FeeTrendBuilder.scala       # 费用趋势
│   │   └── traffic/                 # 流量分析
│   │       ├── BoroughFlowBuilder.scala     # 区域流量
│   │       ├── DropoffHotspotsBuilder.scala # 下车热点
│   │       └── PickupHotspotsBuilder.scala  # 上车热点
│   ├── common/                       # 公共组件
│   │   ├── SparkSessionFactory.scala  # SparkSession 工厂
│   │   ├── ConfigManager.scala        # 配置管理
│   │   ├── QualityManager.scala       # 质量管理
│   │   ├── MetricsCollector.scala     # 指标收集
│   │   ├── DataFrameMetrics.scala     # DataFrame 指标
│   │   ├── CacheManager.scala         # 缓存管理
│   │   ├── SmartCacheLite.scala       # 轻量级缓存
│   │   ├── IcebergTableManager.scala  # Iceberg 表管理
│   │   ├── JoinStrategyLite.scala      # Join 策略
│   │   └── SparkListenerMetrics.scala # Spark 监听器
│   ├── quality/                      # 质量检测
│   │   ├── QualityReporter.scala       # 质量报告
│   │   ├── QualityScoreCalculator.scala # 质量评分
│   │   ├── OdsQualityReporter.scala    # ODS 质量报告
│   │   ├── DwdQualityReporter.scala    # DWD 质量报告
│   │   ├── DwsQualityReporter.scala    # DWS 质量报告
│   │   ├── AdsQualityReporter.scala    # ADS 质量报告
│   │   └── SourceRecordCountCheck.scala # 源数据记录数校验
│   ├── models/                       # 数据模型
│   │   ├── BuildResult.scala          # 构建结果
│   │   ├── JobContext.scala           # 作业上下文
│   │   └── QualityReport.scala        # 质量报告模型
│   ├── exception/                     # 异常处理
│   │   └── QualityCheckException.scala # 质量检查异常
│   └── utils/                         # 工具类
│       ├── DeadLetterWriter.scala     # 死信写入器
│       ├── MonitorUtils.scala         # 监控工具
│       └── TaxiDataUtils.scala        # 出租车数据工具
├── src/main/resources/
│   ├── application.conf              # 主配置
│   ├── application-dev.conf          # 开发环境配置
│   ├── application-prod.conf         # 生产环境配置
│   ├── hive-site.xml                  # Hive 配置
│   └── log4j.properties               # 日志配置
├── src/main/java/taxi-analysis/
│   ├── CheckAnalysisSchema.scala      # Schema 检查
│   ├── CsvToHive.scala               # CSV 转 Hive
│   ├── DataQualityCheck.scala         # 数据质量检查
│   ├── DataUtil.scala                # 数据工具
│   ├── DimTableSync.scala            # 维度表同步
│   ├── DwdDataQualityCheck.scala     # DWD 质量检查
│   ├── DwdLayerBuilder.scala         # DWD 构建
│   ├── ReadDimensionTables.scala     # 读取维度表
│   └── TaxiAnalysisToMySQL.scala      # 数据写入 MySQL
├── docs/                              # 模块文档
└── pom.xml                            # Maven 配置
```

## 🔧 数仓分层

### ODS 层（操作数据层）

存储原始数据，保持数据原貌：

- **Green Taxi ODS**: `nyc_taxi_ods.taxi_trip_green_ods`
- **Yellow Taxi ODS**: `nyc_taxi_ods.taxi_trip_yellow_ods`

### DWD 层（明细数据层）

进行数据清洗和标准化：

- **事实表**: `nyc_taxi_dwd.fact_taxi_trips`
- 统一 Green/Yellow Taxi 数据格式
- 补充时间维度信息（年/月/日/小时）

### DWS 层（汇总数据层）

按业务需求进行轻度汇总：

- **汇总表**: `nyc_taxi_dws.dws_taxi_trips_*`
- 支持按天/小时/区域/供应商等维度聚合

### ADS 层（应用数据层）

面向业务场景的应用数据，写入 MySQL：

| 表类型 | 说明 |
|--------|------|
| **日级指标** | 日订单量、营收、KPI 汇总 |
| **区域分析** | 各区订单/营收/热点 |
| **时间分析** | 小时分布、工作日分析 |
| **费用分析** | 费用组成、趋势、各区对比 |
| **流量分析** | 上下车热点、区域流量 |
| **供应商分析** | 各供应商表现对比 |

## 🚀 运行方式

### 编译打包

```bash
cd batch-processor
mvn clean package -DskipTests
```

### 启动各层作业

```bash
# ODS 层加载
spark-submit \
  --class com.taxi.analysis.GreenOdsLoader \
  --master yarn \
  --deploy-mode cluster \
  target/spark-test-1.0-SNAPSHOT.jar

# DWD 层构建
spark-submit \
  --class com.taxi.etl.dwd.DwdLayerBuilder \
  --master yarn \
  --deploy-mode cluster \
  target/spark-test-1.0-SNAPSHOT.jar \
  --start-date 2024-01-01 --end-date 2024-01-31

# DWS 层构建
spark-submit \
  --class com.taxi.etl.dws.DwsLayerBuilder \
  --master yarn \
  --deploy-mode cluster \
  target/spark-test-1.0-SNAPSHOT.jar \
  --start-date 2024-01-01 --end-date 2024-01-31

# ADS 层构建
spark-submit \
  --class com.taxi.etl.ads.AdsLayerBuilder \
  --master yarn \
  --deploy-mode cluster \
  target/spark-test-1.0-SNAPSHOT.jar \
  --start-date 2024-01-01 --end-date 2024-01-31
```

### 参数说明

| 参数 | 说明 | 示例 |
|------|------|------|
| `--start-date` | 开始日期 | 2024-01-01 |
| `--end-date` | 结束日期 | 2024-01-31 |
| `--env` | 运行环境 | dev/prod |

## 📊 ADS 产出指标

### 日级指标

| 指标 | 说明 |
|------|------|
| `ads_kpi_daily` | 日级 KPI 汇总 |
| `ads_vendor_daily` | 供应商日表现 |
| `ads_payment_daily` | 支付方式日分布 |
| `ads_weekday_analysis` | 工作日分析 |

### 区域分析

| 指标 | 说明 |
|------|------|
| `ads_borough_flow` | 各区流量统计 |
| `ads_pickup_hotspots` | 上车热点 Top10 |
| `ads_dropoff_hotspots` | 下车热点 Top10 |

### 费用分析

| 指标 | 说明 |
|------|------|
| `ads_fee_composition` | 费用组成结构 |
| `ads_fee_by_borough` | 各区费用统计 |
| `ads_fee_by_taxi_type` | 各类型费用统计 |
| `ads_fee_trend` | 费用趋势分析 |

### 分布分析

| 指标 | 说明 |
|------|------|
| `ads_distance_distribution` | 行程距离分布 |
| `ads_duration_distribution` | 行程时长分布 |
| `ads_passenger_distribution` | 乘客数分布 |

## 📝 文档链接

- [模块分析报告](离线批处理模块-模块分析报告.md)
- [修复日志](离线批处理模块-修复日志-20260507.md)
- [变更日志索引](离线批处理模块-变更日志索引.md)
- [离线数仓规范与边界手册](离线批处理模块-离线数仓规范与边界手册.md)
