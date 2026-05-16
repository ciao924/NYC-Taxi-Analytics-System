# 数据生产模块

将纽约市出租车数据集写入 Kafka，模拟实时数据流。

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
└── docs/                    # 模块文档
```

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
# 启动主生产者
python scripts/kafka_producer.py

# 指定发送速率（每秒发送消息数）
python scripts/kafka_producer.py --rate 100

# 从断点继续
python scripts/kafka_producer.py --resume
```

### 测试命令

```bash
# 测试 Kafka 连接
python scripts/test_kafka_connection.py

# 发送单条测试消息
python scripts/test_single_message.py

# 测试批量发送
python scripts/test_batch_send.py
```

## 📝 文档链接

- [模块分析报告](数据生产模块-模块分析报告.md)
- [修复日志索引](数据生产模块-变更日志索引.md)