# 出租车大数据分析系统 - DolphinScheduler 工作流配置

**文档版本**：v1.0
**创建日期**：2026-04-26
**适用版本**：v4.0+

---

## 一、环境配置

### 1.1 DolphinScheduler 集群信息

| 配置项 | 值 |
|--------|-----|
| 版本 | 3.1.8 |
| 部署节点 | hadoop102 |
| Web UI | http://hadoop102:12345 |
| Zookeeper | hadoop102:2181,hadoop103:2181,hadoop104:2181 |
| 数据库 | MySQL 8.0 (192.168.127.102:3306) |

### 1.2 Worker 分组配置

| 分组名称 | 节点 | 用途 |
|----------|------|------|
| `default` | hadoop102, hadoop103, hadoop104 | 默认任务 |
| `realtime` | hadoop103 | 实时任务调度 |
| `offline` | hadoop104 | 离线任务调度 |

### 1.3 租户配置

| 配置项 | 值 |
|--------|-----|
| 租户编码 | dolphin |
| 队列 | default |
| 系统用户 | dolphin |

### 1.4 环境变量配置

```bash
export HADOOP_HOME=/opt/module/hadoop-3.3.1
export HADOOP_CONF_DIR=/opt/module/hadoop-3.3.1/etc/hadoop
export SPARK_HOME=/opt/module/spark-3.1.3
export FLINK_HOME=/opt/module/flink-1.17.2
export HIVE_HOME=/opt/module/hive-3.1.3
export JAVA_HOME=/opt/module/jdk1.8.0_431
export PATH=$PATH:$HADOOP_HOME/bin:$SPARK_HOME/bin:$FLINK_HOME/bin:$HIVE_HOME/bin
```

---

## 二、通用任务定义

### 2.1 Spark 任务节点模板

**任务类型**：Spark
**程序类型**：Java/Scala

**基础配置参数**：

| 参数 | 值 | 说明 |
|------|-----|------|
| `--master` | yarn | 资源管理器 |
| `--deploy-mode` | cluster | 部署模式 |
| `--driver-memory` | 4g | Driver 内存 |
| `--executor-memory` | 8g | Executor 内存 |
| `--executor-cores` | 4 | 每 Executor 核心数 |
| `--num-executors` | 4 | Executor 数量 |
| `spark.sql.adaptive.enabled` | true | 启用 AQE |
| `spark.sql.adaptive.coalescePartitions.enabled` | true | 动态分区合并 |

**JAR 路径**：`/opt/module/taxi-etl/taxi-etl-4.0-SNAPSHOT-jar-with-dependencies.jar`

### 2.2 Shell 任务节点模板

**任务类型**：Shell

**基础配置**：

```bash
#!/bin/bash
# 通用环境变量
source /etc/profile
export JAVA_HOME=/opt/module/jdk1.8.0_431
```

---

## 三、工作流定义

### 3.1 工作流列表

| 工作流名称 | 工作流编码 | 依赖关系 | 执行策略 |
|------------|------------|----------|----------|
| taxi_ods_workflow | ods_daily | 无 | 每日 01:00 |
| taxi_dwd_workflow | dwd_daily | ODS 完成后 | 每日 02:00 |
| taxi_dws_workflow | dws_daily | DWD 完成后 | 每日 03:00 |
| taxi_ads_workflow | ads_daily | DWS 完成后 | 每日 04:00 |
| taxi_quality_workflow | quality_daily | ADS 完成后 | 每日 05:00 |
| taxi_cleanup_workflow | cleanup_daily | 独立 | 每日 03:00 |

---

## 四、ODS 层工作流

### 4.1 工作流配置

| 配置项 | 值 |
|--------|-----|
| 工作流名称 | taxi_ods_workflow |
| 工作流编码 | ods_daily |
| 描述 | ODS 层数据加载工作流 |
| 定时调度 | 每天 01:00 |
| Worker 分组 | offline |
| 失败策略 | 结束 |
| 租户 | dolphin |

### 4.2 任务节点

```
┌─────────────────────────────────────────────────────────────────┐
│                    taxi_ods_workflow                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐                                          │
│  │  GreenOdsLoader   │                                          │
│  │  (黄表ODS加载)     │                                          │
│  │  参数: {dt}       │                                          │
│  └────────┬─────────┘                                          │
│           │                                                    │
│  ┌────────┴─────────┐                                          │
│  │  YellowOdsLoader │                                          │
│  │  (绿表ODS加载)    │                                          │
│  │  参数: {dt}       │                                          │
│  └────────┬─────────┘                                          │
│           │                                                    │
│  ┌────────┴─────────┐                                          │
│  │  OdsValidator    │                                          │
│  │  (ODS数据验证)    │                                          │
│  │  参数: {dt}       │                                          │
│  └──────────────────┘                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 任务节点详细配置

#### GreenOdsLoader

| 配置项 | 值 |
|--------|-----|
| 节点名称 | GreenOdsLoader |
| 任务类型 | Spark |
| 主类 | com.taxi.etl.ods.GreenOdsLoader |
| 启动脚本 | spark-submit |
| 参数 | ${EXECUTION_ID} ${DT} ${DT} |

#### YellowOdsLoader

| 配置项 | 值 |
|--------|-----|
| 节点名称 | YellowOdsLoader |
| 任务类型 | Spark |
| 主类 | com.taxi.etl.ods.YellowOdsLoader |
| 启动脚本 | spark-submit |
| 参数 | ${EXECUTION_ID} ${DT} ${DT} |

#### OdsValidator

| 配置项 | 值 |
|--------|-----|
| 节点名称 | OdsValidator |
| 任务类型 | Spark |
| 主类 | com.taxi.etl.ods.OdsValidator |
| 启动脚本 | spark-submit |
| 参数 | ${EXECUTION_ID} ${DT} ${DT} |

### 4.4 全局参数

| 参数名 | 参数值 | 说明 |
|--------|--------|------|
| DT | ${system.biz.date} | 业务日期 (yyyy-MM-dd) |
| EXECUTION_ID | ${uuid} | 执行唯一ID |

---

## 五、DWD 层工作流

### 5.1 工作流配置

| 配置项 | 值 |
|--------|-----|
| 工作流名称 | taxi_dwd_workflow |
| 工作流编码 | dwd_daily |
| 描述 | DWD 层数据构建工作流 |
| 定时调度 | 每天 02:00 |
| Worker 分组 | offline |
| 失败策略 | 结束 |
| 租户 | dolphin |
| 依赖 | taxi_ods_workflow |

### 5.2 任务节点

```
┌─────────────────────────────────────────────────────────────────┐
│                    taxi_dwd_workflow                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐                                          │
│  │  DwdLayerBuilder  │                                          │
│  │  (DWD层构建)       │                                          │
│  │  参数: {dt}       │                                          │
│  └──────────────────┘                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3 任务节点详细配置

#### DwdLayerBuilder

| 配置项 | 值 |
|--------|-----|
| 节点名称 | DwdLayerBuilder |
| 任务类型 | Spark |
| 主类 | com.taxi.etl.dwd.DwdLayerBuilder |
| 启动脚本 | spark-submit |
| Driver 核心数 | 2 |
| Driver 内存 | 4g |
| Executor 数量 | 4 |
| Executor 核心数 | 4 |
| Executor 内存 | 8g |
| 参数 | ${EXECUTION_ID} ${DT} ${DT} |

---

## 六、DWS 层工作流

### 6.1 工作流配置

| 配置项 | 值 |
|--------|-----|
| 工作流名称 | taxi_dws_workflow |
| 工作流编码 | dws_daily |
| 描述 | DWS 层数据构建工作流 |
| 定时调度 | 每天 03:00 |
| Worker 分组 | offline |
| 失败策略 | 结束 |
| 租户 | dolphin |
| 依赖 | taxi_dwd_workflow |

### 6.2 任务节点

```
┌─────────────────────────────────────────────────────────────────┐
│                    taxi_dws_workflow                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐                                          │
│  │  DwsLayerBuilder  │                                          │
│  │  (DWS层构建)       │                                          │
│  │  参数: {dt}       │                                          │
│  └──────────────────┘                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.3 任务节点详细配置

#### DwsLayerBuilder

| 配置项 | 值 |
|--------|-----|
| 节点名称 | DwsLayerBuilder |
| 任务类型 | Spark |
| 主类 | com.taxi.etl.dws.DwsLayerBuilder |
| 启动脚本 | spark-submit |
| Driver 核心数 | 2 |
| Driver 内存 | 4g |
| Executor 数量 | 4 |
| Executor 核心数 | 4 |
| Executor 内存 | 8g |
| 参数 | ${EXECUTION_ID} ${DT} ${DT} |

---

## 七、ADS 层工作流

### 7.1 工作流配置

| 配置项 | 值 |
|--------|-----|
| 工作流名称 | taxi_ads_workflow |
| 工作流编码 | ads_daily |
| 描述 | ADS 层数据构建工作流 |
| 定时调度 | 每天 04:00 |
| Worker 分组 | offline |
| 失败策略 | 结束 |
| 租户 | dolphin |
| 依赖 | taxi_dws_workflow |

### 7.2 任务节点

```
┌─────────────────────────────────────────────────────────────────┐
│                    taxi_ads_workflow                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐                                          │
│  │  AdsLayerBuilder │                                          │
│  │  (ADS层构建)      │                                          │
│  │  参数: {dt}       │                                          │
│  └──────────────────┘                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 7.3 任务节点详细配置

#### AdsLayerBuilder

| 配置项 | 值 |
|--------|-----|
| 节点名称 | AdsLayerBuilder |
| 任务类型 | Spark |
| 主类 | com.taxi.etl.ads.AdsLayerBuilder |
| 启动脚本 | spark-submit |
| Driver 核心数 | 2 |
| Driver 内存 | 4g |
| Executor 数量 | 4 |
| Executor 核心数 | 4 |
| Executor 内存 | 8g |
| 参数 | ${EXECUTION_ID} ${DT} ${DT} |

---

## 八、质量检测工作流

### 8.1 工作流配置

| 配置项 | 值 |
|--------|-----|
| 工作流名称 | taxi_quality_workflow |
| 工作流编码 | quality_daily |
| 描述 | 数据质量检测工作流 |
| 定时调度 | 每天 05:00 |
| Worker 分组 | offline |
| 失败策略 | 警告 |
| 租户 | dolphin |
| 依赖 | taxi_ads_workflow |

### 8.2 任务节点

```
┌─────────────────────────────────────────────────────────────────┐
│                 taxi_quality_workflow                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────┐                                  │
│  │  DwdQualityReporter      │                                  │
│  │  (DWD层质量报告)          │                                  │
│  └──────────┬───────────────┘                                  │
│             │                                                  │
│  ┌──────────┴───────────────┐                                  │
│  │  SourceRecordCountCheck │                                  │
│  │  (源文件记录数对账)       │                                  │
│  └──────────┬───────────────┘                                  │
│             │                                                  │
│  ┌──────────┴───────────────┐                                  │
│  │  QualityScoreCalculator  │                                  │
│  │  (质量评分计算)           │                                  │
│  └──────────────────────────┘                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 九、清理工作流

### 9.1 工作流配置

| 配置项 | 值 |
|--------|-----|
| 工作流名称 | taxi_cleanup_workflow |
| 工作流编码 | cleanup_daily |
| 描述 | 数据生命周期清理工作流 |
| 定时调度 | 每天 03:00 |
| Worker 分组 | default |
| 失败策略 | 警告 |
| 租户 | dolphin |

### 9.2 任务节点

```
┌─────────────────────────────────────────────────────────────────┐
│                 taxi_cleanup_workflow                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────┐                                          │
│  │  SmallFileMerge  │                                          │
│  │  (小文件合并)     │                                          │
│  └────────┬─────────┘                                          │
│           │                                                    │
│  ┌────────┴─────────┐                                          │
│  │  OdsDataCleanup │                                          │
│  │  (ODS数据清理)    │                                          │
│  └──────────────────┘                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 十、任务依赖关系

### 10.1 依赖关系图

```
┌─────────────────────────────────────────────────────────────────┐
│                      任务依赖关系图                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  taxi_ods_workflow                                             │
│       │                                                         │
│       ▼                                                         │
│  taxi_dwd_workflow                                             │
│       │                                                         │
│       ▼                                                         │
│  taxi_dws_workflow                                             │
│       │                                                         │
│       ▼                                                         │
│  taxi_ads_workflow                                             │
│       │                                                         │
│       ▼                                                         │
│  taxi_quality_workflow                                         │
│                                                                 │
│  ─────────────────────────────────────────────────────────────  │
│                                                                 │
│  taxi_cleanup_workflow (独立执行)                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 10.2 工作流依赖配置

| 上游工作流 | 下游工作流 | 依赖条件 |
|------------|------------|----------|
| taxi_ods_workflow | taxi_dwd_workflow | 上游工作流执行成功 |
| taxi_dwd_workflow | taxi_dws_workflow | 上游工作流执行成功 |
| taxi_dws_workflow | taxi_ads_workflow | 上游工作流执行成功 |
| taxi_ads_workflow | taxi_quality_workflow | 上游工作流执行成功 |

---

## 十一、失败重试与告警配置

### 11.1 全局重试策略

| 配置项 | 值 |
|--------|-----|
| 最大重试次数 | 3 |
| 重试间隔 | 5 分钟 |
| 失败告警 | 启用 |

### 11.2 任务级重试配置

| 任务类型 | 最大重试次数 | 重试间隔 |
|----------|-------------|----------|
| Spark 任务 | 3 | 5 分钟 |
| Shell 任务 | 2 | 3 分钟 |
| 数据质量任务 | 1 | 立即 |

### 11.3 告警配置

**告警方式**：钉钉

**告警规则**：

| 告警类型 | 触发条件 | 告警内容 |
|----------|----------|----------|
| 工作流失败 | 工作流执行失败 | [告警] ${工作流名称} 执行失败 |
| 任务超时 | 任务执行超过 2 小时 | [告警] ${任务名称} 执行超时 |
| 质量告警 | 质量评分低于 80 | [告警] 数据质量评分低于阈值 |

**钉钉配置**：

```yaml
# alert.properties
alert.type=DINGTALK
dingtalk.webhook.url=https://oapi.dingtalk.com/robot/send?access_token=XXX
dingtalk.msg.type=MARKDOWN
```

---

## 十二、调度监控看板

### 12.1 监控指标

| 指标 | 说明 | 刷新频率 |
|------|------|----------|
| 工作流成功率 | 今日工作流成功率 | 每分钟 |
| 任务执行数 | 今日执行任务数 | 每分钟 |
| 平均耗时 | 工作流平均执行时间 | 每小时 |
| 失败任务数 | 今日失败任务数 | 每分钟 |

### 12.2 看板配置

DolphinScheduler 自带监控页面：
- 访问地址：`http://hadoop102:12345/dolphinscheduler`
- 监控路径：安全中心 → 监控中心 → 工作流统计

### 12.3 自定义告警规则

| 规则名称 | SQL 条件 | 告警级别 |
|----------|----------|----------|
| 连续失败告警 | COUNT(1) WHERE STATUS='FAILURE' AND RUNS>3 | 严重 |
| 超时告警 | DATEDIFF(END_TIME, START_TIME) > 2 HOURS | 警告 |
| 质量告警 | QUALITY_SCORE < 80 | 警告 |

---

## 十三、执行参数说明

### 13.1 日期参数

| 参数 | 格式 | 示例 | 说明 |
|------|------|------|------|
| system.biz.date | yyyy-MM-dd | 2025-01-15 | 业务日期 |
| system.cur_date | yyyy-MM-dd | 2025-01-15 | 当前日期 |
| system.datetime | yyyy-MM-dd HH:mm:ss | 2025-01-15 01:00:00 | 当前时间 |

### 13.2 自定义参数

| 参数名 | 定义方式 | 示例 | 说明 |
|--------|----------|------|------|
| DT | \${system.biz.date} | 2025-01-15 | 业务日期 |
| MONTH | \${system.biz.date.substring(0,7)} | 2025-01 | 月份 |
| YEAR | \${system.biz.date.substring(0,4)} | 2025 | 年份 |

---

**文档结束**
