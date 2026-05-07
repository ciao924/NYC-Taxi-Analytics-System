# NYC Taxi Analytics System - 项目结构规范

## 📁 项目结构

```
spark_test/
├── data-producer/                    # 数据生产模块
│   ├── config/                       # 配置文件
│   │   ├── __init__.py
│   │   └── kafka_config.py
│   ├── scripts/                      # Python 脚本
│   │   ├── kafka_producer.py         # 主生产者脚本
│   │   ├── test_kafka_connection.py  # Kafka 连接测试
│   │   └── ...
│   ├── data/                         # 数据文件
│   ├── checkpoint/                   # 断点续传
│   ├── logs/                         # 日志文件
│   ├── docs/                         # 模块文档
│   │   └── CHANGELOG.md              # 变更日志
│   ├── reports/                      # 运行报告
│   │   └── README.md
│   ├── .env.example                  # 环境变量模板
│   ├── requirements.txt              # Python 依赖
│   └── README.md                     # 模块说明
├── streaming-processor/              # 实时流处理模块
│   ├── src/main/scala/com/taxi/realtime/
│   │   ├── config/                   # 配置管理
│   │   ├── model/                    # 数据模型
│   │   ├── quality/                  # 质量检测
│   │   ├── sink/                     # 输出模块
│   │   ├── source/                   # 输入模块
│   │   ├── utils/                    # 工具类
│   │   ├── RealtimeOdsJob.scala      # ODS 作业
│   │   └── RealtimeMetricsJob.scala  # 指标作业
│   ├── src/main/resources/           # 配置资源
│   ├── scripts/                      # 部署脚本
│   ├── logs/                         # 日志文件
│   ├── docs/                         # 模块文档
│   │   └── CHANGELOG.md
│   ├── reports/                      # 运行报告
│   ├── target/                       # 编译产物
│   ├── pom.xml                       # Maven 配置
│   └── README.md
├── batch-processor/                  # 离线批处理模块
│   ├── src/main/scala/com/taxi/etl/
│   │   ├── ods/                      # ODS 层
│   │   ├── dwd/                      # DWD 层
│   │   ├── dws/                      # DWS 层
│   │   ├── ads/                      # ADS 层
│   │   ├── common/                   # 公共组件
│   │   ├── models/                   # 数据模型
│   │   ├── quality/                  # 质量检测
│   │   └── utils/                    # 工具类
│   ├── src/main/resources/           # 配置资源
│   ├── logs/                         # 日志文件
│   ├── docs/                         # 模块文档
│   │   └── CHANGELOG.md
│   ├── reports/                      # 运行报告
│   ├── target/                       # 编译产物
│   ├── pom.xml                       # Maven 配置
│   └── README.md
├── docs/                             # 项目级文档
│   ├── ETL开发规范.md
│   ├── 数仓分层设计.md
│   ├── 数仓命名规范.md
│   ├── 数据字典.md
│   └── ...
├── .gitignore                        # Git 忽略配置
├── README.md                         # 项目说明
├── INTEGRATION_GUIDE.md              # 整合指南
└── REALTIME_OFFLINE_INTEGRATION.md   # 实时离线衔接
```

## 📋 目录规范

### 1. 模块级目录

每个模块必须包含以下目录：

| 目录 | 用途 | 必需 |
|------|------|------|
| `config/` | 配置文件 | ✅ |
| `scripts/` | 脚本文件 | ✅ |
| `logs/` | 日志文件 | ✅ |
| `docs/` | 模块文档 | ✅ |
| `reports/` | 运行报告 | ✅ |

### 2. 文档规范

每个模块的 `docs/` 目录必须包含：

| 文件 | 用途 |
|------|------|
| `CHANGELOG.md` | 变更日志 |
| `README.md` | 模块说明（在模块根目录） |

### 3. 报告规范

每个模块的 `reports/` 目录用于存放：

| 类型 | 文件命名 |
|------|----------|
| 运行报告 | `report_YYYYMMDD.md` |
| 性能报告 | `performance_YYYYMMDD.md` |
| 问题报告 | `issue_YYYYMMDD.md` |
| 数据质量报告 | `quality_YYYYMMDD.md` |

## 🔧 编译规范

### data-producer (Python)

```bash
cd data-producer
pip install -r requirements.txt
python scripts/kafka_producer.py --help
```

### streaming-processor (Maven)

```bash
cd streaming-processor
mvn clean package -DskipTests
```

### batch-processor (Maven)

```bash
cd batch-processor
mvn clean package -DskipTests
```

## 📝 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 目录 | 小写字母 + 连字符 | `data-producer` |
| Python 文件 | 小写字母 + 下划线 | `kafka_producer.py` |
| Scala 文件 | 大驼峰命名 | `RealtimeOdsJob.scala` |
| 配置文件 | 小写字母 + 连字符 | `application-ods.properties` |
| 数据库表 | 前缀_业务_类型 | `ods_taxi_trip_green` |

## 🚀 版本管理

### 变更日志格式

```markdown
## v1.x.x (YYYY-MM-DD)

### 新增
- 功能描述

### 修复
- 问题描述

### 优化
- 优化描述

### 移除
- 移除描述
```

---

**版本**: v1.0  
**更新日期**: 2026-05-07