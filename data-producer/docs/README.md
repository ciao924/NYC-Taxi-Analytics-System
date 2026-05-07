# Data Producer Module

数据生产模块 - 将纽约市出租车数据集写入 Kafka，模拟实时数据流。

## 📌 模块定位

本模块是实时数据处理 Pipeline 的**数据源层**，负责将历史 Parquet 格式的出租车数据以可控速率写入 Kafka 主题。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **多数据源支持** | 支持 Green/Yellow 两种出租车数据 |
| **速率控制** | 自定义消息发送速率，模拟真实业务场景 |
| **断点续传** | 自动保存发送进度，重启后从断点继续 |
| **数据校验** | 内置数据校验机制，过滤无效记录 |
| **优雅退出** | 支持键盘中断，确保数据不丢失 |
| **日志监控** | 完善的日志记录，支持控制台和文件输出 |

## 🛠️ 技术栈

- Python 3.8+
- kafka-python 2.0.2+
- pandas 2.0+
- pyarrow 15.0+
- python-dotenv 1.0+

## 📁 目录结构

```
data-producer/
├── config/                  # 配置文件目录
│   ├── __init__.py          # 配置管理模块
│   └── kafka_config.py      # Kafka 配置定义
├── data/                    # 数据文件目录
│   ├── green_tripdata_2025-04.parquet   # 绿色出租车数据
│   └── yellow_tripdata_2025-04.parquet  # 黄色出租车数据
├── logs/                    # 日志文件目录
├── checkpoint/              # 断点续传目录
├── scripts/                 # 脚本目录
│   ├── kafka_producer.py            # 主生产者脚本
│   ├── test_kafka_connection.py     # Kafka 连接测试
│   ├── check_parquet_file.py        # Parquet 文件检查
│   ├── test_single_message.py       # 单条消息测试
│   └── test_batch_send.py           # 批量发送测试
├── .env.example             # 环境变量示例文件
├── requirements.txt         # Python 依赖清单
└── README.md                # 本说明文档
```

## 🚀 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 文件：

```env
# Kafka 配置
KAFKA_BOOTSTRAP_SERVERS=hadoop102:9092,hadoop103:9092,hadoop104:9092
KAFKA_TOPIC_GREEN=taxi_trip_green
KAFKA_TOPIC_YELLOW=taxi_trip_yellow

# 数据文件路径
GREEN_TRIPDATA_PATH=data/green_tripdata_2025-04.parquet
YELLOW_TRIPDATA_PATH=data/yellow_tripdata_2025-04.parquet
```

### 3. 启动生产者

```bash
# 启动绿色出租车数据生产者（默认）
python scripts/kafka_producer.py

# 启动黄色出租车数据生产者
python scripts/kafka_producer.py --taxi-type yellow

# 指定发送速率（50条/秒）
python scripts/kafka_producer.py --rate 50
```

## 📋 命令行参数

| 参数 | 类型 | 描述 | 默认值 |
|------|------|------|--------|
| `--taxi-type` | str | 出租车类型：`green` 或 `yellow` | `green` |
| `--file-path` | str | Parquet 数据文件路径 | 根据 taxi-type 自动选择 |
| `--topic` | str | Kafka 主题名称 | 根据 taxi-type 自动选择 |
| `--rate` | int | 发送速率（条/秒） | `10` |
| `--start-offset` | int | 数据文件起始位置 | `0` |

## 🔄 断点续传机制

- **断点文件位置**：`checkpoint/{topic}_checkpoint.json`
- **保存内容**：`{"offset": 1000, "timestamp": 1712000000}`
- **触发时机**：每发送 100 条记录自动保存

## 📊 日志管理

- **日志存储**：`logs/kafka_producer.log`
- **日志级别**：INFO
- **输出目标**：控制台 + 文件

## ✅ 数据校验规则

| 校验项 | 描述 |
|--------|------|
| `VendorID` | 不能为空 |
| `pickup_datetime` | 不能为空 |
| `dropoff_datetime` | 不能为空 |
| `passenger_count` | 不能为空 |
| `trip_distance` | 不能为空 |
| `PULocationID` | 不能为空 |
| `DOLocationID` | 不能为空 |

## 📝 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2025-04-01 | 初始版本 |
| v1.1 | 2025-04-15 | 添加黄色出租车支持 |
| v1.2 | 2025-04-30 | 优化速率控制和断点续传 |