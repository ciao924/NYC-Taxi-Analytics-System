# 实时流处理模块

基于 Apache Flink 的实时数据处理引擎。

## 📌 模块定位

本模块是实时数据处理 Pipeline 的**核心处理层**，承接上游数据生产模块的 Kafka 数据流，进行实时清洗、质量检测、指标计算和 ODS 落地。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **Kafka 消费** | 从 `taxi_trip_green` 和 `taxi_trip_yellow` 主题消费实时出租车数据 |
| **JSON 解析** | 将 JSON 消息转换为结构化数据对象，支持错误侧输出 |
| **数据清洗** | 过滤无效数据、校验时间戳、处理异常记录 |
| **Watermark 管理** | 支持乱序数据的 Watermark 处理，设置时间窗口 |
| **ODS 落地** | 将原始数据写入 HDFS，使用 ORC 格式和 SNAPPY 压缩 |
| **实时指标计算** | 实时计算订单量、热点区域、费用分布等核心指标 |
| **质量检测** | 实时监控数据质量指标，支持告警触发 |
| **多 Sink 输出** | 支持 HDFS、MySQL、告警、死信队列等多种输出方式 |
| **状态管理** | 支持 Flink 检查点和状态恢复 |
| **死信队列** | 处理解析和清洗失败的数据 |

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Apache Flink | 1.17.2 | 实时流处理引擎 |
| Apache Kafka | 3.4.0 | 消息队列 |
| Scala | 2.12.17 | 开发语言 |
| Maven | 3.6+ | 构建工具 |
| MySQL | 8.0+ | 数据存储 |
| Hadoop HDFS | 3.3.4 | 文件存储 |
| ORC | - | 列式存储格式 |

## 📁 目录结构

```
streaming-processor/
├── src/main/scala/com/taxi/realtime/
│   ├── config/
│   │   └── ConfigManager.scala      # 配置管理
│   ├── model/
│   │   ├── GreenTripRecord.scala   # 行程记录模型
│   │   ├── Trip.scala              # 行程通用模型
│   │   ├── FeeComposition.scala    # 费用组成模型
│   │   ├── HotspotResult.scala     # 热点区域结果
│   │   ├── OrderMetric.scala       # 订单指标模型
│   │   └── ErrorRecordJava.scala   # 错误记录模型
│   ├── quality/
│   │   ├── AlertDetector.scala     # 告警检测器
│   │   ├── QualityAggregateFunctions.scala  # 质量聚合函数
│   │   └── QualityModels.scala     # 质量数据模型
│   ├── serializer/
│   │   └── GreenTripRecordVectorizer.scala  # 序列化器
│   ├── sink/
│   │   ├── AlertSinkBuilder.scala       # 告警 Sink 构建器
│   │   ├── DeadLetterSinkBuilder.scala   # 死信队列 Sink
│   │   ├── GreenTripRecordBucketAssigner.scala  # 分桶策略
│   │   ├── MysqlSinkFactory.scala        # MySQL Sink 工厂
│   │   └── QualitySinkBuilder.scala      # 质量数据 Sink
│   ├── source/
│   │   └── KafkaSourceBuilder.scala      # Kafka 数据源构建
│   ├── utils/
│   │   ├── Constants.scala           # 常量定义
│   │   ├── DataCleaner.scala         # 数据清洗工具
│   │   ├── DataUtil.scala            # 数据处理工具
│   │   ├── JsonDeserializer.scala   # JSON 反序列化
│   │   ├── LoggerUtil.scala          # 日志工具
│   │   └── MetricsCollector.scala    # 指标收集器
│   ├── DataCleanFunction.scala       # 数据清洗函数
│   ├── JsonParseFunction.scala       # JSON 解析函数
│   ├── RealtimeMetricsJob.scala      # 实时指标计算作业
│   └── RealtimeOdsJob.scala          # 实时 ODS 写入作业
├── src/main/resources/
│   ├── application.properties        # 主配置
│   ├── application-ods.properties    # ODS 作业配置
│   ├── application-metrics.properties # 指标作业配置
│   ├── flink-conf.yaml              # Flink 集群配置
│   ├── log4j2.properties            # 日志配置
│   ├── log4j2.xml                   # 日志 XML 配置
│   ├── hive-site.xml                # Hive 元数据配置
│   └── create_mysql_tables.sql      # MySQL 建表脚本
├── scripts/
│   ├── deploy.sh                    # 部署脚本
│   ├── restart.sh                   # 重启脚本
│   ├── stop.sh                      # 停止脚本
│   ├── status.sh                    # 状态查看脚本
│   ├── savepoint.sh                 # 保存点脚本
│   └── kafka_lag_monitor.sh         # Kafka 消费延迟监控
├── docs/                            # 模块文档
├── pom.xml                          # Maven 配置
└── dependency-reduced-pom.xml      # 依赖优化配置
```

## 🔧 核心作业

### RealtimeOdsJob - ODS 层写入作业

负责将 Kafka 中的原始数据落地到 HDFS ODS 层：

- **输入**: Kafka `taxi_trip_green` / `taxi_trip_yellow` 主题
- **输出**: HDFS ORC 文件，按 `year=YYYY/month=MM` 分区
- **特性**:
  - 支持 Watermark 和乱序处理
  - 错误数据侧输出到死信队列
  - 定期执行 `MSCK REPAIR TABLE` 同步 Hive 分区

### RealtimeMetricsJob - 实时指标计算作业

负责实时计算核心业务指标：

- **输入**: Kafka `taxi_trip_green` / `taxi_trip_yellow` 主题
- **输出**: MySQL 指标表 + 告警
- **指标类型**:
  - 实时订单量统计
  - 热点区域 Top10（上车/下车）
  - 费用组成分析
  - 支付方式分布
  - 数据质量监控

## 🚀 运行方式

### 编译打包

```bash
cd streaming-processor
mvn clean package -DskipTests
```

### 启动 ODS 作业

```bash
# 本地运行
flink run \
  -c com.taxi.realtime.RealtimeOdsJob \
  target/flink-realtime-1.0-SNAPSHOT.jar \
  --config application-ods.properties

# 集群运行（YARN）
flink run \
  -m yarn-cluster \
  -c com.taxi.realtime.RealtimeOdsJob \
  target/flink-realtime-1.0-SNAPSHOT.jar \
  --config application-ods.properties

# 指定 JobManager
flink run \
  -m hadoop102:8081 \
  -c com.taxi.realtime.RealtimeOdsJob \
  target/flink-realtime-1.0-SNAPSHOT.jar
```

### 启动指标作业

```bash
# 本地运行
flink run \
  -c com.taxi.realtime.RealtimeMetricsJob \
  target/flink-realtime-1.0-SNAPSHOT.jar \
  --config application-metrics.properties

# 集群运行
flink run \
  -m yarn-cluster \
  -c com.taxi.realtime.RealtimeMetricsJob \
  target/flink-realtime-1.0-SNAPSHOT.jar
```

### 脚本操作

```bash
# 部署
bash scripts/deploy.sh

# 重启
bash scripts/restart.sh

# 停止
bash scripts/stop.sh

# 查看状态
bash scripts/status.sh

# 创建保存点
bash scripts/savepoint.sh <job_id>

# 监控 Kafka 消费延迟
bash scripts/kafka_lag_monitor.sh
```

## 📊 输出指标

| 指标类型 | 输出位置 | 说明 |
|----------|----------|------|
| **实时订单量** | MySQL | 每分钟订单数统计 |
| **热点区域** | MySQL | 实时上车/下车热点 Top10 |
| **费用分析** | MySQL | 实时费用分布统计 |
| **支付方式** | MySQL | 支付方式实时分布 |
| **质量指标** | MySQL | 数据质量实时监控 |
| **告警信息** | MySQL | 异常数据告警记录 |
| **ODS 数据** | HDFS | 原始数据归档 |

## 🔌 数据模型

### GreenTripRecord

| 字段 | 类型 | 说明 |
|------|------|------|
| vendorId | Integer | 供应商ID |
| pickupDatetime | Timestamp | 上车时间 |
| dropoffDatetime | Timestamp | 下车时间 |
| passengerCount | Integer | 乘客数量 |
| tripDistance | Double | 行程距离 |
| rateCodeId | Integer | 费率代码 |
| storeAndFwdFlag | String | 存储转发标志 |
| pickupLocationId | Integer | 上车地点ID |
| dropoffLocationId | Integer | 下车地点ID |
| paymentType | Integer | 支付类型 |
| fareAmount | Double | 车费 |
| extra | Double | 附加费 |
| mtaTax | Double |  MTA 税 |
| tipAmount | Double | 小费 |
| tollsAmount | Double | 通行费 |
| improvementSurcharge | Double | 改进费 |
| totalAmount | Double | 总费用 |

## 📝 文档链接

- [模块分析报告](实时流处理模块-模块分析报告.md)
- [修复日志](实时流处理模块-修复日志-20260507.md)
- [变更日志索引](实时流处理模块-变更日志索引.md)
- [实时数仓方案：双Job架构设计](实时流处理模块-实时数仓方案：双Job架构设计.md)
- [实时数据质量检测：纽约出租车数据](实时流处理模块-实时数据质量检测：纽约出租车数据.md)
