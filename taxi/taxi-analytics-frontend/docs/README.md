# 前端应用模块

基于 Vue 3 的数据可视化前端应用。

## 📌 模块定位

本模块是系统的**用户界面层**，提供数据可视化、实时监控、多维分析、热力地图、质量监控、AI 智能查询等交互功能。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **数据仪表盘** | 核心 KPI 指标卡片、趋势图表、支付分布可视化 |
| **实时监控** | 实时订单量、热点区域、费用组成的实时数据大屏 |
| **多维分析** | 支持多维度数据钻取分析（机场/区域/时间/支付方式/车型） |
| **热力地图** | 基于高德地图的地理数据热力图可视化 |
| **质量监控** | 数据质量仪表盘，展示数据健康状态和告警 |
| **AI 智能查询** | 自然语言查询界面，支持多轮对话和图表生成 |

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.4+ | 前端框架 |
| TypeScript | 5.4+ | 类型安全 |
| Element Plus | 2.6+ | UI 组件库 |
| ECharts | 5.5+ | 图表库 |
| vue-echarts | 6.7+ | Vue ECharts 封装 |
| Pinia | 2.1+ | 状态管理 |
| Vue Router | 4.3+ | 路由管理 |
| Axios | 1.6+ | HTTP 客户端 |
| Vite | 5.2+ | 构建工具 |
| @amap/amap-jsapi-loader | 1.0+ | 高德地图加载器 |

## 📁 目录结构

```
taxi-analytics-frontend/
├── src/
│   ├── api/                      # API 接口层
│   │   ├── index.ts             # 接口导出入口
│   │   ├── request.ts           # Axios 实例配置
│   │   ├── types.ts            # 请求/响应类型
│   │   ├── ai.ts               # AI 模块接口
│   │   ├── analysis.ts         # 数据分析接口
│   │   ├── dashboard.ts        # 仪表盘接口
│   │   ├── export.ts           # 导出接口
│   │   ├── map.ts              # 热力地图接口
│   │   ├── quality.ts         # 质量监控接口
│   │   └── realtime.ts         # 实时数据接口
│   ├── assets/
│   │   └── styles/             # 全局样式
│   │       ├── global.scss     # 全局样式
│   │       └── variables.scss  # 样式变量
│   ├── components/              # 公共组件
│   │   ├── ai/                 # AI 组件
│   │   │   └── AiQuery.vue    # AI 查询组件
│   │   └── analysis/           # 分析组件
│   │       └── tabs/          # 分析 Tab 组件
│   │           ├── AirportAnalysis.vue       # 机场分析
│   │           ├── BoroughRevenueAnalysis.vue # 区域营收
│   │           ├── FeeCompositionAnalysis.vue # 费用组成
│   │           ├── HotspotsAnalysis.vue      # 热点分析
│   │           ├── HourlyAnalysis.vue        # 小时分析
│   │           ├── PaymentAnalysis.vue      # 支付分析
│   │           ├── TaxiTypeAnalysis.vue     # 车型分析
│   │           ├── TripFeatureAnalysis.vue  # 行程特征
│   │           ├── VendorAnalysis.vue       # 供应商分析
│   │           ├── WeekdayAnalysis.vue      # 工作日分析
│   │           └── multi/                  # 多维分析子组件
│   │               ├── AirportBoroughAnalysis.vue
│   │               ├── AirportTimeAnalysis.vue
│   │               ├── BoroughPaymentAnalysis.vue
│   │               ├── DistancePaymentAnalysis.vue
│   │               ├── TaxiTypeFeeAnalysis.vue
│   │               ├── TimePaymentAnalysis.vue
│   │               ├── VendorPaymentAnalysis.vue
│   │               ├── VendorTaxiTypeAnalysis.vue
│   │               └── WeekdayTimeAnalysis.vue
│   ├── composables/             # 组合式函数
│   │   └── useExport.ts        # 导出逻辑
│   ├── router/                 # 路由配置
│   │   └── index.ts
│   ├── stores/                 # Pinia 状态管理
│   │   ├── index.ts            # store 入口
│   │   └── dashboard.ts       # 仪表盘状态
│   ├── utils/                 # 工具函数
│   │   └── validator.ts       # 表单验证
│   ├── views/                  # 页面视图
│   │   ├── ai/                # AI 智能查询
│   │   │   └── Index.vue
│   │   ├── analysis/          # 数据分析
│   │   │   ├── Index.vue
│   │   │   ├── BasicAnalysis.vue    # 基础分析
│   │   │   └── MultiDimensionAnalysis.vue # 多维分析
│   │   ├── dashboard/         # 仪表盘
│   │   │   └── Index.vue
│   │   ├── error/             # 错误页面
│   │   │   └── 404.vue
│   │   ├── map/               # 热力地图
│   │   │   └── HeatmapView.vue
│   │   ├── quality/           # 质量监控
│   │   │   └── Index.vue
│   │   └── realtime/          # 实时监控
│   │       └── Index.vue
│   ├── App.vue                 # 根组件
│   ├── env.d.ts               # 环境类型声明
│   └── main.ts                # 应用入口
├── public/                    # 静态资源
├── docs/                      # 模块文档
├── package.json               # 依赖配置
├── vite.config.ts            # Vite 配置
├── tsconfig.json             # TypeScript 配置
└── .eslintrc.js              # ESLint 配置
```

## 🌐 页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | 仪表盘 | 核心指标概览、数据趋势 |
| `/realtime` | 实时监控 | 实时数据大屏、订单量/热点/费用 |
| `/analysis` | 数据分析 | 多维分析报表 |
| `/map` | 热力地图 | 上下车热点地理可视化 |
| `/quality` | 质量监控 | 数据质量仪表盘、告警 |
| `/ai` | AI 查询 | 自然语言智能查询助手 |

## 🚀 运行方式

### 安装依赖

```bash
cd taxi/taxi-analytics-frontend
npm install
```

### 开发模式

```bash
npm run dev
```

### 生产构建

```bash
npm run build
```

### 预览构建结果

```bash
npm run preview
```

### 代码检查

```bash
# 检查代码规范
npm run lint

# 自动修复代码规范问题
npm run lint:fix

# 代码格式化
npm run format
```

## 📊 页面功能详情

### 仪表盘（Dashboard）

- **KPI 卡片**：总订单量、总营收、平均车费、完单率
- **趋势图表**：按天/周/月展示订单和营收趋势
- **支付分布**：饼图展示各支付方式占比
- **供应商表现**：柱状图对比各供应商数据

### 实时监控（Realtime）

- **实时 KPI**：当前小时订单量、实时营收
- **热点区域**：Top10 上车/下车热点
- **费用组成**：实时费用结构分布

### 数据分析（Analysis）

- **基础分析**：
  - 机场分析（Airport）
  - 区域营收（Borough Revenue）
  - 费用组成（Fee Composition）
  - 热点分析（Hotspots）
  - 小时分布（Hourly）
  - 支付分析（Payment）
  - 车型分析（Taxi Type）
  - 行程特征（Trip Feature）
  - 供应商分析（Vendor）
  - 工作日分析（Weekday）

- **多维分析**：
  - 机场 × 区域
  - 机场 × 时间
  - 区域 × 支付方式
  - 距离 × 支付方式
  - 车型 × 费用
  - 时间 × 支付方式
  - 供应商 × 支付方式
  - 供应商 × 车型
  - 工作日 × 时间

### 热力地图（Map）

- 基于高德地图的散点热力图
- 支持上车/下车热点切换
- 区域筛选功能

### 质量监控（Quality）

- 数据质量概览
- 表健康状态
- 告警记录

### AI 查询（AI）

- 自然语言输入框
- 多轮对话界面
- 查询结果图表展示

## 📝 文档链接

- [模块分析报告](前端应用模块-模块分析报告.md)
