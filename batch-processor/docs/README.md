# 离线批处理模块

基于 Apache Spark 的离线数据处理引擎。

## 📌 模块定位

本模块是数据处理 Pipeline 的**离线处理层**，负责处理历史数据，构建数仓分层结构（ODS → DWD → DWS → ADS）。

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
│   │   ├── SparkSessionFactory.scala
│   │   ├── ConfigManager.scala
│   │   └── QualityManager.scala
│   ├── quality/              # 质量检测
│   │   ├── QualityReporter.scala
│   │   └── QualityScoreCalculator.scala
│   └── utils/                # 工具类
├── src/main/resources/       # 配置文件
│   ├── application.conf
│   ├── application-dev.conf
│   ├── application-prod.conf
│   └── hive-site.xml
├── docs/                     # 模块文档
└── pom.xml                   # Maven 配置
```

## 🚀 运行方式

### 编译打包

```bash
cd batch-processor
mvn clean package -DskipTests
```

### 提交作业

```bash
# 全量执行
spark-submit \
  --class com.taxi.etl.xxx \
  --master yarn \
  --deploy-mode cluster \
  target/spark-test-1.0-SNAPSHOT.jar
```

## 📊 数仓分层

| 层级 | 说明 | 存储位置 |
|------|------|----------|
| **ODS** | 原始数据层 | Hive ODS 库 |
| **DWD** | 明细数据层 | Hive DWD 库 |
| **DWS** | 汇总数据层 | Hive DWS 库 |
| **ADS** | 应用数据层 | MySQL |

## 📝 文档链接

- [模块分析报告](离线批处理模块-模块分析报告.md)
- [修复日志索引](离线批处理模块-变更日志索引.md)
- [离线数仓规范](离线批处理模块-离线数仓规范与边界手册.md)