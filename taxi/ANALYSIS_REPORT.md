# 出租车数据分析系统 - 前后端分析报告

## 一、项目概述

本项目是一个出租车数据分析系统，包含**离线批处理**和**实时流处理**两大模块，旨在为业务决策提供数据支撑。系统采用前后端分离架构，前端使用 Vue 3 + TypeScript + Element Plus，后端使用 Spring Boot + MyBatis Plus。

---

## 三、架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端层 (Vue 3)                            │
│  ┌─────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ ┌─────────┐ │
│  │Dashboard│ │ Realtime │ │  Quality │ │    AI   │ │   Map   │ │
│  └────┬────┘ └────┬─────┘ └────┬─────┘ └────┬────┘ └────┬────┘ │
│       │           │            │            │            │      │
└───────┼───────────┼────────────┼────────────┼────────────┼──────┘
        │           │            │            │            │
        ▼           ▼            ▼            ▼            ▼
┌─────────────────────────────────────────────────────────────────┐
│                        API层 (REST/WebSocket)                   │
│   /dashboard/*   /realtime/*   /quality/*   /ai/*   /map/*    │
└───────┬───────────────────────┬────────────────────────────────┘
        │                       │
        ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                     后端服务层 (Spring Boot)                     │
│  Controller → Service → Mapper → MySQL/Redis/Caffeine          │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 前端框架 | Vue | 3.4.x | 组合式API |
| 类型系统 | TypeScript | 5.4.x | 类型安全 |
| UI组件 | Element Plus | 2.6.x | 组件库 |
| 图表库 | ECharts | 5.5.x | 数据可视化 |
| 状态管理 | Pinia | 2.1.x | 状态管理 |
| 路由 | Vue Router | 4.3.x | 路由管理 |
| 后端框架 | Spring Boot | 3.1.x | Java后端 |
| ORM | MyBatis Plus | 3.5.x | 数据访问 |
| 缓存 | Caffeine | 3.1.x | 本地缓存 |
| 限流 | Guava | 32.1.x | 接口限流 |
| 文档 | SpringDoc | 2.3.x | API文档 |

---

## 四、前端模块分析

### 4.1 模块结构

```
src/
├── api/           # API接口定义
├── components/    # 可复用组件
│   ├── ai/        # AI相关组件
│   ├── business/  # 业务组件
│   ├── charts/    # 图表组件
│   ├── common/    # 通用组件
│   ├── dashboard/ # 仪表盘组件
│   └── quality/   # 质量检测组件
├── composables/   # 组合式函数
├── router/        # 路由配置
├── stores/        # 状态管理
├── utils/         # 工具函数
└── views/         # 页面视图
    ├── ai/        # AI智能体模块
    ├── dashboard/ # 数据看板模块
    ├── map/       # 地图热力图模块
    ├── quality/   # 数据质量检测模块
    └── realtime/  # 实时监控模块
```

### 4.2 核心模块功能

| 模块 | 功能描述 | 核心组件 |
|------|----------|----------|
| **Dashboard** | 数据看板，展示KPI指标、趋势图表 | KpiCard、TrendChart、PieChart |
| **Realtime** | 实时监控，WebSocket推送 | StatCard、AlertList |
| **Quality** | 数据质量检测 | QualityScore、AnomalyTable |
| **AI Agent** | 自然语言查询助手 | ChatWindow、AiQuery |
| **Map** | 热力地图展示 | HeatmapView |

### 4.3 API调用层设计

采用 Axios 封装，支持：
- 请求重试机制（网络超时自动重试）
- 统一响应拦截（处理业务错误码）
- 请求参数自动时间戳（防缓存）

**响应数据结构**：
```typescript
interface ResponseData<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}
```

---

## 五、后端模块分析

### 5.1 模块结构

```
src/main/java/com/taxi/analytics/
├── common/         # 公共模块
│   ├── config/     # 配置类
│   ├── controller/ # 公共控制器
│   ├── exception/  # 异常处理
│   ├── interceptor/# 拦截器
│   ├── result/     # 统一响应封装
│   └── validation/ # 自定义校验
└── modules/        # 业务模块
    ├── ai/         # AI智能体模块
    ├── analysis/   # 数据分析模块
    ├── dashboard/  # 仪表盘模块
    ├── export/     # 数据导出模块
    ├── map/        # 地图模块
    ├── quality/    # 质量检测模块
    └── realtime/   # 实时模块
```

### 5.2 核心服务

| 服务 | 功能 | 关键特性 |
|------|------|----------|
| **DashboardKpiService** | KPI数据聚合 | Caffeine缓存（5分钟） |
| **RealtimeService** | 实时数据推送 | WebSocket实时推送 |
| **AiService** | AI查询处理 | LLM调用、SQL生成 |
| **QualityService** | 数据质量检测 | 定时任务检测 |
| **AnalysisService** | 深度数据分析 | 模拟数据支持 |

### 5.3 数据层设计

- **数据库**: MySQL（业务数据）
- **缓存策略**: Caffeine（本地缓存，5分钟过期）
- **数据结构**: DTO → Entity → DB

### 5.4 Dashboard API 接口列表

| API路径 | HTTP方法 | 功能描述 | Controller文件 |
|---------|----------|----------|----------------|
| `/dashboard/kpi/summary` | GET | 获取KPI汇总数据 | DashboardController |
| `/dashboard/kpi/trend` | GET | 获取KPI趋势数据 | DashboardController |
| `/dashboard/hourly/distribution` | GET | 获取小时分布数据 | DashboardController |
| `/dashboard/weekday/analysis` | GET | 获取星期分析数据 | DashboardController |
| `/dashboard/payment/analysis` | GET | 获取支付方式分析 | DashboardController |
| `/dashboard/fee/composition` | GET | 获取费用构成 | DashboardController |
| `/dashboard/fee/percentage` | GET | 获取费用占比 | DashboardController |
| `/dashboard/zone/hotspots` | GET | 获取上下客热点 | DashboardController |
| `/dashboard/borough/flow` | GET | 获取行政区流量 | DashboardController |
| `/dashboard/date/range` | GET | 获取可用日期范围 | DashboardController |
| `/dashboard/refresh` | POST | 刷新缓存 | DashboardController |

---

## 六、数据流分析

### 6.1 离线数据流程

```
Spark批处理 → MySQL(analysis_kpi_daily等) → Dashboard API → 前端展示
```

### 6.2 实时数据流程

```
Flink流处理 → Redis → WebSocket → 前端实时更新
```

### 6.3 AI查询流程

```
用户查询 → AI Controller → Intent分类 → SQL生成 → 执行查询 → 图表生成 → 返回结果
```

### 6.4 深度分析数据流程

```
前端请求 → AnalysisController → AnalysisService → AnalysisMapper → MySQL → Mock数据兜底 → 返回结果
```

---

## 七、关键代码示例

### 7.1 后端统一响应封装

```java
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
}
```

### 7.2 前端状态管理

```typescript
export const useDashboardStore = defineStore('dashboard', () => {
  const kpiSummary = ref<KpiSummary | null>(null)
  const kpiTrend = ref<TrendData[]>([])
  
  const fetchKpiSummary = async (startDate?: string, endDate?: string) => {
    try {
      const response = await dashboardApi.getKpiSummary(startDate, endDate)
      kpiSummary.value = response
    } catch (error) {
      console.error('Failed to fetch KPI summary:', error)
    }
  }
  
  return { kpiSummary, kpiTrend, fetchKpiSummary }
})
```

### 7.3 后端缓存策略

```java
private final Cache<String, Object> cache = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(100)
        .build();

@Override
public KpiSummaryDTO getKpiSummary(LocalDate startDate, LocalDate endDate) {
    String cacheKey = "kpi_summary:" + startDate + ":" + endDate;
    return (KpiSummaryDTO) cache.get(cacheKey, k -> {
        // 从数据库查询并转换为DTO
        Map<String, Object> summary = baseMapper.getKpiSummary(startDate, endDate);
        return KpiSummaryDTO.builder()
                .tripCount(getLongValue(summary, "trip_count"))
                .totalRevenue(getDoubleValue(summary, "total_revenue"))
                .avgFare(getDoubleValue(summary, "avg_fare"))
                .avgDistance(getDoubleValue(summary, "avg_distance"))
                .build();
    });
}
```

---

## 八、技术亮点

1. **类型安全**: 前后端均使用强类型（TypeScript/Java），减少运行时错误
2. **缓存策略**: Caffeine本地缓存，减少数据库查询压力
3. **限流保护**: Guava限流拦截器，保护API接口
4. **统一响应**: 全局异常处理与响应封装，前端统一处理
5. **Mock数据兜底**: AnalysisService提供Mock数据，便于开发测试
6. **响应式设计**: Element Plus组件库，移动端适配

---

## 九、待优化项

| 优先级 | 优化项 | 说明 |
|--------|--------|------|
| 高 | 后端服务启动 | 需解决Maven仓库权限问题 |
| 高 | WebSocket集成 | 需后端支持实时推送 |
| 中 | 代码分割 | 前端chunk过大，需动态导入优化 |
| 中 | 热力图地图 | 需高德地图API密钥配置 |
| 低 | 单元测试 | 需补充测试用例 |

---

## 十、总结

本项目已完成核心功能的重构与修复：

1. ✅ **DashboardController返回类型修复** - 确保返回类型与Service匹配
2. ✅ **数据映射修复** - 修正字段名映射错误
3. ✅ **前端默认日期设置** - 解决初始加载数据为空问题
4. ✅ **前端构建验证** - 构建成功，无TypeScript错误

系统采用前后端分离架构，代码结构清晰，具备良好的可扩展性和可维护性。后续可继续完善WebSocket实时推送、热力地图展示等功能。