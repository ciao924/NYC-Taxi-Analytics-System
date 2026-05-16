# 实时流处理模块

基于 Apache Flink 的实时数据处理引擎。

## 📌 模块定位

本模块是实时数据处理 Pipeline 的**核心处理层**，承接上游数据生产模块的数据输入，进行实时清洗、质量检测和指标计算。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **Kafka 消费** | 从 Kafka 主题消费实时出租车数据 |
| **JSON 解析** | 将 JSON 消息转换为结构化数据对象 |
| **数据清洗** | 过滤无效数据，确保数据质量 |
| **质量检测** | 实时监控数据质量指标，支持告警触发 |
| **指标计算** | 实时计算核心业务指标（订单量、热点区域等） |
| **多 Sink 输出** | 支持 HDFS、MySQL、告警等多种输出方式 |
| **死信队列** | 处理解析和清洗失败的数据 |
| **状态管理** | 支持 Flink 检查点和状态恢复 |

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Apache Flink | 1.17.2 | 实时流处理引擎 |
| Apache Kafka | 3.4.0 | 消息队列 |
| MySQL | 8.0+ | 数据存储 |
| Scala | 2.12.17 | 开发语言 |
| Maven | 3.6+ | 构建工具 |

## 📁 目录结构

```
streaming-processor/
├── src/main/scala/com/taxi/realtime/
│   ├── config/              # 配置管理
│   ├── model/               # 数据模型
│   ├── quality/             # 质量检测模块
│   ├── sink/                # 数据输出模块
│   ├── source/              # 数据输入模块
│   ├── utils/               # 工具类
│   ├── RealtimeMetricsJob.scala   # 实时指标计算作业
│   └── RealtimeOdsJob.scala       # 实时 ODS 写入作业
├── src/main/resources/       # 配置文件
│   ├── application.properties
│   ├── application-ods.properties
│   ├── application-metrics.properties
│   └── flink-conf.yaml
├── scripts/                  # 脚本文件
│   ├── deploy.sh
│   ├── restart.sh
│   ├── stop.sh
│   └── status.sh
├── docs/                     # 模块文档
└── pom.xml                   # Maven 配置
```

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
  target/flink-realtime-1.0-SNAPSHOT.jar

# 集群运行
flink run \
  -m yarn-cluster \
  -c com.taxi.realtime.RealtimeOdsJob \
  target/flink-realtime-1.0-SNAPSHOT.jar \
  --config application-ods.properties
```

### 启动指标作业

```bash
flink run \
  -c com.taxi.realtime.RealtimeMetricsJob \
  target/flink-realtime-1.0-SNAPSHOT.jar \
  --config application-metrics.properties
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
```

## 📊 输出指标

| 指标类型 | 说明 |
|----------|------|
| **实时订单量** | 每分钟订单数统计 |
| **热点区域** | 实时上车/下车热点 Top10 |
| **费用分析** | 实时费用分布统计 |
| **支付方式** | 支付方式实时分布 |
| **质量指标** | 数据质量实时监控 |

## 📝 文档链接

- [模块分析报告](实时流处理模块-模块分析报告.md)
- [修复日志索引](实时流处理模块-变更日志索引.md)
- [实时数仓方案](实时流处理模块-实时数仓方案：双Job架构设计.md)
- [质量检测文档](实时流处理模块-实时数据质量检测：纽约出租车数据.md)