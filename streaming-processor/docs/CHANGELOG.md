# 实时流处理模块 - 变更日志

## 目录结构

本文档是变更日志索引，详细的修复记录请参阅以下文件：

| 日期 | 文件 | 主要内容 |
|------|------|----------|
| 2026-04-29 | [CHANGELOG-20260429.md](./CHANGELOG-20260429.md) | 热点TopN窗口语义修复、ODS空分区NPE、死信处理、Hive配置统一 |
| 2026-05-06 | [CHANGELOG-20260506.md](./CHANGELOG-20260506.md) | Flink类型推断修复、处理时间窗口、MySQL rank关键字、多Topic消费 |
| 2026-05-07 | [CHANGELOG-20260507.md](./CHANGELOG-20260507.md) | ODS作业重构、质量检测模块、序列化问题修复、数据库架构分离 |

---

## 版本历史

### v1.0.0 (2026-05-07)

#### 新增功能
- 数据质量检测模块（实时统计错误率并写入 MySQL）
- 告警配置动态加载（根据 `quality_alert_config` 表调整阈值）
- 费用构成分析（按支付类型统计）

#### 修复问题
- ✅ Flink Lambda 泛型类型推断失败
- ✅ Processing Time 窗口无输出问题
- ✅ MySQL `rank` 关键字冲突
- ✅ ObjectMapper 序列化问题
- ✅ 配置参数静态变量跨 JVM 传递问题

#### 配置优化
- Checkpoint 存储改为 HDFS
- Consumer Group ID 改为可配置
- ODS 分区同步间隔可配置

---

## 贡献指南

### 添加新的变更记录

1. 在 `docs/` 目录创建新文件：`CHANGELOG-YYYYMMDD.md`
2. 文件格式参考已有文件
3. 更新本文档的目录表格

### 文件命名规范

```
CHANGELOG-YYYYMMDD.md
```

示例：`CHANGELOG-20260507.md`

---

## 报告问题

如发现问题，请记录在 `reports/` 目录下的 Issue 报告中。