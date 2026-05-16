# 数据生产模块

将纽约市出租车数据集写入 Kafka，模拟实时数据流。

## 📌 模块定位

本模块是实时数据处理 Pipeline 的**数据源层**，负责将历史 Parquet 格式的出租车数据以可控速率写入 Kafka 主题，为下游流处理模块提供实时数据流。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **多数据源支持** | 支持 Green Taxi（绿色出租车）和 Yellow Taxi（黄色出租车）两种数据类型 |
| **Kafka 主题写入** | 将数据写入独立的 Kafka 主题：`taxi_trip_green` 和 `taxi_trip_yellow` |
| **速率控制** | 自定义消息发送速率，支持 `--rate` 参数控制每秒发送消息数 |
| **断点续传** | 自动保存发送进度到 checkpoint 文件夹，重启后使用 `--resume` 参数继续 |
| **数据校验** | 内置数据校验机制，过滤无效记录，确保数据质量 |
| **优雅退出** | 支持键盘中断（Ctrl+C），确保数据不丢失 |
| **日志监控** | 完善的日志记录，支持控制台和滚动文件输出 |
| **批量发送** | 支持批量发送模式，提高吞吐量 |

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Python | 3.8+ | 开发语言 |
| kafka-python | 2.0.2+ | Kafka 客户端 |
| pandas | 2.0+ | 数据处理 |
| pyarrow | 15.0+ | Parquet 文件读取 |
| python-dotenv | 1.0+ | 环境变量管理 |

## 📁 目录结构

```
data-producer/
├── config/                      # 配置文件目录
│   ├── __init__.py             # 配置管理模块
│   └── kafka_config.py         # Kafka 连接和主题配置
├── data/                       # 数据文件目录
│   ├── green_tripdata_2025-04.parquet   # 绿色出租车数据
│   └── yellow_tripdata_2025-04.parquet  # 黄色出租车数据
├── logs/                       # 日志文件目录
│   └── kafka_producer.log      # 滚动日志文件
├── checkpoint/                 # 断点续传目录
│   └── {topic}_checkpoint.json # 各主题的断点文件
├── scripts/                    # 脚本目录
│   ├── kafka_producer.py              # 主生产者脚本
│   ├── test_kafka_connection.py      # Kafka 连接测试
│   ├── check_parquet_file.py         # Parquet 文件检查
│   ├── test_single_message.py        # 单条消息测试
│   ├── test_batch_send.py            # 批量发送测试
│   ├── kafka_producer.service        # systemd 服务文件
│   ├── run_background.bat           # Windows 后台运行脚本
│   └── test_kafka_connection.py     # Kafka 连接测试
├── .env.example                # 环境变量示例文件
├── requirements.txt            # Python 依赖清单
└── docs/                       # 模块文档
```

## 🔧 Kafka 配置

### 主题配置

| 主题名 | 数据类型 | 说明 |
|--------|----------|------|
| `taxi_trip_green` | Green Taxi | 绿色出租车行程数据 |
| `taxi_trip_yellow` | Yellow Taxi | 黄色出租车行程数据 |

### 生产者配置

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `bootstrap_servers` | hadoop102:9092, hadoop103:9092, hadoop104:9092 | Kafka 集群地址 |
| `acks` | all | 确认机制 |
| `retries` | 5 | 重试次数 |
| `batch_size` | 16384 | 批量大小 |
| `linger_ms` | 100 | 批量等待时间 |

## 🚀 运行方式

### 安装依赖

```bash
cd data-producer
pip install -r requirements.txt
```

### 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，配置 Kafka 连接信息
```

### 启动生产者

```bash
# 启动主生产者（默认速率）
python scripts/kafka_producer.py --topic taxi_trip_green

# 指定发送速率（每秒发送消息数）
python scripts/kafka_producer.py --topic taxi_trip_green --rate 100

# 从断点继续发送
python scripts/kafka_producer.py --topic taxi_trip_green --resume

# 指定数据文件路径
python scripts/kafka_producer.py --topic taxi_trip_yellow --data data/yellow_tripdata_2025-04.parquet
```

### 测试命令

```bash
# 测试 Kafka 连接
python scripts/test_kafka_connection.py

# 检查 Parquet 文件
python scripts/check_parquet_file.py --file data/green_tripdata_2025-04.parquet

# 发送单条测试消息
python scripts/test_single_message.py

# 测试批量发送
python scripts/test_batch_send.py
```

### 后台运行（Linux/systemd）

```bash
# 使用 systemd 服务
sudo cp scripts/kafka-producer.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable kafka-producer
sudo systemctl start kafka-producer
```

### 后台运行（Windows）

```bash
# 使用批处理脚本
scripts\run_background.bat
```

## 📊 数据格式

### Green Taxi 数据字段

| 字段 | 类型 | 说明 |
|------|------|------|
| VendorID | Integer | 供应商ID |
| lpep_pickup_datetime | Timestamp | 上车时间 |
| lpep_dropoff_datetime | Timestamp | 下车时间 |
| passenger_count | Integer | 乘客数量 |
| trip_distance | Double | 行程距离 |
| fare_amount | Double | 车费 |
| ... | ... | 其他字段 |

### Yellow Taxi 数据字段

| 字段 | 类型 | 说明 |
|------|------|------|
| VendorID | Integer | 供应商ID |
| tpep_pickup_datetime | Timestamp | 上车时间 |
| tpep_dropoff_datetime | Timestamp | 下车时间 |
| passenger_count | Integer | 乘客数量 |
| trip_distance | Double | 行程距离 |
| fare_amount | Double | 车费 |
| ... | ... | 其他字段 |

## 📝 文档链接

- [模块分析报告](数据生产模块-模块分析报告.md)
- [修复日志](数据生产模块-修复日志-20260507.md)
- [变更日志索引](数据生产模块-变更日志索引.md)
