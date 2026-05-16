# 共享类型模块

前后端共享的 TypeScript 类型定义。

## 📌 模块定位

本模块提供**前后端统一的类型定义**，确保数据结构的一致性，减少类型错误。

## ✨ 核心功能

| 功能 | 描述 |
|------|------|
| **API 类型** | 定义 API 请求/响应数据结构 |
| **图表类型** | 定义图表配置和数据格式 |
| **质量类型** | 定义数据质量相关类型 |
| **分析类型** | 定义数据分析相关类型 |

## 🛠️ 技术栈

- TypeScript 5.4+
- npm/yarn

## 📁 目录结构

```
shared-types/
├── src/
│   ├── api/                  # API 相关类型
│   │   ├── index.ts          # 类型导出入口
│   │   ├── common.ts         # 通用类型
│   │   ├── dashboard.ts      # 仪表盘类型
│   │   ├── realtime.ts       # 实时监控类型
│   │   ├── analysis.ts       # 数据分析类型
│   │   ├── map.ts            # 地图相关类型
│   │   ├── quality.ts        # 质量监控类型
│   │   └── ai.ts             # AI 模块类型
│   └── index.ts              # 全局导出
├── docs/                     # 模块文档
├── package.json              # 依赖配置
└── tsconfig.json             # TypeScript 配置
```

## 🚀 使用方式

### 安装依赖

```bash
npm install @taxi-analytics/shared-types
```

### 导入类型

```typescript
import type { DashboardData, RealtimeMetric } from '@taxi-analytics/shared-types';

const data: DashboardData = {
  // ...
};
```

## 📝 文档链接

- [模块分析报告](共享类型模块-模块分析报告.md)