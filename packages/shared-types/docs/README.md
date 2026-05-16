# 共享类型模块

前后端共享的 TypeScript 类型定义。

## 📌 模块定位

本模块提供**前后端统一的类型定义**，确保前后端数据结构的一致性，减少类型错误，提供更好的开发体验和类型安全。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **通用类型** | 通用的分页、日期范围、API 响应等基础类型 |
| **仪表盘类型** | Dashboard 模块的请求/响应数据结构 |
| **实时监控类型** | Realtime 模块的请求/响应数据结构 |
| **数据分析类型** | Analysis 模块的请求/响应数据结构 |
| **质量监控类型** | Quality 模块的请求/响应数据结构 |
| **AI 模块类型** | AI 智能查询的请求/响应数据结构 |
| **地图类型** | 热力地图相关的类型定义 |
| **图表类型** | 统一的图表配置和数据格式定义 |

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| TypeScript | 5.4+ | 类型定义语言 |
| npm/yarn | - | 包管理工具 |

## 📁 目录结构

```
shared-types/
├── src/
│   ├── api/                    # API 相关类型
│   │   ├── index.ts          # 类型导出入口
│   │   ├── common.ts         # 通用类型定义
│   │   ├── dashboard.ts      # 仪表盘类型
│   │   ├── realtime.ts       # 实时监控类型
│   │   ├── analysis.ts       # 数据分析类型
│   │   ├── map.ts            # 地图相关类型
│   │   ├── quality.ts        # 质量监控类型
│   │   └── ai.ts             # AI 模块类型
│   └── index.ts              # 全局导出
├── docs/                      # 模块文档
├── package.json               # 依赖配置
├── tsconfig.json              # TypeScript 配置
└── README.md                  # 本文档
```

## 📦 类型详情

### 通用类型（common.ts）

```typescript
// 分页参数
interface PageParams {
  pageNum?: number;
  pageSize?: number;
}

// 日期范围参数
interface DateRangeParams {
  startDate: string;
  endDate: string;
}

// API 统一响应
interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
  success: boolean;
}

// 分页响应
interface PageResponse<T = unknown> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

// 图表配置
interface ChartConfig {
  chartType: ChartType;
  xAxisField: string;
  yAxisField: string;
  title: string;
  data: Record<string, unknown>[];
}

// 图表类型
type ChartType = 'bar' | 'line' | 'pie' | 'scatter' | 'heatmap' | 'table';
```

### 仪表盘类型（dashboard.ts）

```typescript
// KPI 汇总
interface KpiSummary {
  tripCount: number;
  totalRevenue: number;
  avgFare: number;
  avgDistance: number;
}

// 趋势数据
interface TrendData {
  statDate: string;
  totalTrips: number;
  totalRevenue: number;
  avgFare: number;
}

// 小时分布
interface HourlyDistribution {
  hour: number;
  tripCount: number;
  avgFare: number;
}

// 支付方式分布
interface PaymentDistribution {
  paymentType: string;
  paymentTypeName: string;
  tripCount: number;
  percentage: number;
}

// 供应商表现
interface VendorPerformance {
  vendorId: string;
  vendorName: string;
  tripCount: number;
  totalRevenue: number;
  avgFare: number;
  rating: number;
}
```

### AI 模块类型（ai.ts）

```typescript
// AI 查询请求
interface AiQueryRequest {
  query: string;
  sessionId?: string;
  database?: string;
}

// AI 查询响应
interface AiQueryResponse {
  sessionId: string;
  sql: string;
  explanation: string;
  data: Record<string, unknown>[];
  chartConfig?: ChartConfig;
}

// ETL 生成请求/响应
interface EtlGenRequest {
  sourceTable: string;
  targetTable: string;
  description: string;
}

interface EtlGenResponse {
  sparkSql: string;
  explanation: string;
  fieldMappings: FieldMapping[];
}

// 数据倾斜诊断
interface SkewDiagnoseRequest {
  jobId: string;
  executionPlan: string;
}

interface SkewDiagnoseResponse {
  hasSkew: boolean;
  skewedStages: string[];
  skewFactor: number;
  recommendations: string[];
}

// 任务诊断
interface TaskDiagnoseRequest {
  taskId: string;
  taskLog: string;
}

interface TaskDiagnoseResponse {
  rootCause: string;
  category: string;
  suggestions: string[];
  confidence: number;
}

// Flink 反压分析
interface FlinkBackpressureRequest {
  jobId: string;
  operatorMetrics: string[];
}

interface FlinkBackpressureResponse {
  hasBackpressure: boolean;
  bottleneckOperator: string;
  backpressureRatio: number;
  recommendations: string[];
}

// 并行度推荐
interface ParallelismRecommendRequest {
  jobId: string;
  currentThroughput: number;
  currentParallelism: number;
}

interface ParallelismRecommendResponse {
  recommendedParallelism: number;
  expectedImprovement: number;
  operatorRecommendations: OperatorRecommendation[];
}
```

### 实时监控类型（realtime.ts）

```typescript
// 实时 KPI
interface RealtimeKpi {
  currentHourTrips: number;
  todayTrips: number;
  todayRevenue: number;
  currentHourRevenue: number;
}

// 热点区域
interface Hotspot {
  locationId: number;
  locationName: string;
  tripCount: number;
  rank: number;
}

// 费用组成
interface FeeComposition {
  fareAmount: number;
  extra: number;
  mtaTax: number;
  tipAmount: number;
  tollsAmount: number;
  improvementSurcharge: number;
  totalAmount: number;
}
```

### 质量监控类型（quality.ts）

```typescript
// 质量汇总
interface QualitySummary {
  totalRecords: number;
  validRecords: number;
  invalidRecords: number;
  qualityScore: number;
}

// 表健康状态
interface TableHealthStatus {
  tableName: string;
  recordCount: number;
  qualityScore: number;
  lastCheckTime: string;
  status: 'healthy' | 'warning' | 'critical';
}

// 告警信息
interface Alert {
  alertId: string;
  alertType: string;
  alertLevel: 'info' | 'warning' | 'error';
  message: string;
  timestamp: string;
  acknowledged: boolean;
}
```

### 地图类型（map.ts）

```typescript
// 热力图数据点
interface HeatmapPoint {
  longitude: number;
  latitude: number;
  weight: number;
}

// 热力图请求
interface HeatmapRequest {
  type: 'pickup' | 'dropoff';
  startDate: string;
  endDate: string;
  borough?: string;
}

// 热力图响应
interface HeatmapResponse {
  points: HeatmapPoint[];
  totalCount: number;
  bounds: {
    north: number;
    south: number;
    east: number;
    west: number;
  };
}
```

### 数据分析类型（analysis.ts）

```typescript
// 多维分析请求
interface MultiDimensionAnalysisRequest extends DateRangeParams {
  dimensions: string[];
  metrics: string[];
}

// 趋势分析
interface TrendAnalysis {
  statDate: string;
  value: number;
  changeRate: number;
}

// 异常检测
interface AnomalyDetection {
  timestamp: string;
  metric: string;
  value: number;
  expectedValue: number;
  deviation: number;
  severity: 'low' | 'medium' | 'high';
}
```

## 🚀 使用方式

### 安装依赖

```bash
npm install @taxi-analytics/shared-types
```

### 导入类型

```typescript
import type {
  // 通用类型
  ApiResponse,
  PageParams,
  ChartConfig,
  // 仪表盘类型
  KpiSummary,
  TrendData,
  // AI 类型
  AiQueryRequest,
  AiQueryResponse,
} from '@taxi-analytics/shared-types';

// 使用类型
const response: ApiResponse<KpiSummary> = {
  code: 200,
  message: 'success',
  data: {
    tripCount: 1000,
    totalRevenue: 50000,
    avgFare: 50,
    avgDistance: 5.5
  },
  success: true
};
```

## 📝 文档链接

- [模块分析报告](共享类型模块-模块分析报告.md)
