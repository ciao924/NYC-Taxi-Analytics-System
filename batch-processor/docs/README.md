# Batch Processor Module

离线批处理模块 - 基于 Apache Spark 的离线数据处理引擎。

## 📌 模块定位

本模块是数据处理 Pipeline 的**离线处理层**，负责处理历史数据，构建数仓分层结构（ODS → DWD → DWS）。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **ODS 加载** | 从 Parquet 文件加载原始数据到 Hive ODS 层 |
| **DWD 构建** | 构建明细数据层，进行数据清洗和标准化 |
| **DWS 构建** | 构建汇总数据层，计算聚合指标 |
| **ADS 输出** | 将聚合指标写入 MySQL，供 BI 报表使用 |
| **维度表同步** | 管理维度表数据，支持缓慢变化维度 |
| **数据质量检查** | 对处理后的数据进行质量校验 |
| **元数据管理** | 管理数据表结构和字段信息 |

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Apache Spark | 3.5+ | 离线批处理引擎 |
| Apache Hive | 3.1.2 | 元数据管理 |
| Apache Iceberg | 1.5+ | 数据湖存储 |
| MySQL | 8.0+ | 应用层存储 |
| Scala | 2.12.17 | 开发语言 |
| Maven | 3.6+ | 构建工具 |

## 📁 目录结构

```
batch-processor/
├── src/main/scala/com/taxi/etl/
│   ├── ods/                  # ODS 层处理
│   │   ├── GreenOdsLoader.scala
│   │   ├── YellowOdsLoader.scala
│   │   └── OdsValidator.scala
│   ├── dwd/                  # DWD 层处理
│   │   └── DwdLayerBuilder.scala
│   ├── dws/                  # DWS 层处理
│   │   └── DwsLayerBuilder.scala
│   ├── ads/                  # ADS 层处理
│   │   ├── base/             # 基础组件
│   │   ├── daily/            # 日级指标
│   │   ├── fee/              # 费用分析
│   │   └── traffic/          # 流量分析
│   ├── common/               # 公共组件
│   ├── models/               # 数据模型
│   ├── quality/              # 质量检测
│   ├── exception/            # 异常处理
│   └── utils/                # 工具类
├── src/main/resources/       # 配置文件
│   ├── application.conf
│   ├── application-dev.conf
│   ├── application-prod.conf
│   └── hive-site.xml
├── pom.xml                   # Maven 配置
└── README.md                 # 本说明文档
```

## 🚀 快速开始

### 1. 编译打包

```bash
cd batch-processor
mvn clean package -DskipTests
```

### 2. 运行作业

```bash
# 运行 ODS 加载作业
spark-submit --class com.taxi.etl.ods.GreenOdsLoader \
  target/taxi-analytics-assembly.jar

# 运行 DWD 构建作业
spark-submit --class com.taxi.etl.dwd.DwdLayerBuilder \
  target/taxi-analytics-assembly.jar

# 运行 ADS 构建作业
spark-submit --class com.taxi.etl.ads.AdsLayerBuilder \
  target/taxi-analytics-assembly.jar
```

## 📊 数仓分层结构

```
ODS (原始数据层)
    │
    ▼
DWD (明细数据层)
    │
    ▼
DWS (汇总数据层)
    │
    ▼
ADS (应用数据层) → MySQL → BI Dashboard
```

## 📋 数仓层级说明

| 层级 | 说明 | 存储格式 |
|------|------|----------|
| **ODS** | 原始数据层，直接从数据源加载 | Hive ORC |
| **DWD** | 明细数据层，清洗后的业务明细 | Iceberg |
| **DWS** | 汇总数据层，按维度聚合 | Iceberg |
| **ADS** | 应用数据层，最终报表指标 | MySQL |

## 📝 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2025-04-01 | 初始版本 |
| v1.1 | 2025-04-15 | 添加 DWD/DWS 层构建 |
| v1.2 | 2025-04-30 | 支持 Iceberg 数据湖 |