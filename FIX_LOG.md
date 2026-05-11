# 修复日志

## 版本：v1.0.0
**修复日期：** 2024年1月

---

### 问题1：行程特征子模块乘客数量分布与小费比率分布可视化图tooltip显示整百整千问题

**问题描述：**
行程特征子模块中，乘客数量分布与小费比率分布图表的tooltip显示的订单数为整百整千，不符合实际数据逻辑。

**根本原因：**
后端Spark作业 `PassengerDistributionBuilder.scala` 使用固定比率和round函数来估算订单数，而非基于真实数据统计。

**修复方案：**
修改 `batch-processor/src/main/scala/com/taxi/etl/ads/distribution/PassengerDistributionBuilder.scala`：
- 移除固定比率估算逻辑
- 使用真实的 `passenger_count` 字段进行分组统计
- 按实际乘客数量分组计算订单数

**修改文件：**
- `batch-processor/src/main/scala/com/taxi/etl/ads/distribution/PassengerDistributionBuilder.scala`

---

### 问题2：数据看板模块时间范围选项不起作用问题

**问题描述：**
数据看板模块订单趋势可视化图右上角的时间范围选项（7日/30日/90日）完全不起作用，且未联动数据展示和查询日期范围。

**根本原因：**
前端 `Index.vue` 中的时间范围按钮未绑定到日期选择器和数据获取逻辑。

**修复方案：**
修改 `taxi-analytics-frontend/src/views/dashboard/Index.vue`：
- 添加 `calculateDateRange` 函数计算日期范围
- 更新 `handlePeriodChange` 函数实现日期联动
- 修改 `activePeriod` watcher 触发数据刷新
- 确保日期选择器与时间范围按钮双向同步

**修改文件：**
- `taxi-analytics-frontend/src/views/dashboard/Index.vue`

---

### 问题3：数据看板模块可视化效果增强

**问题描述：**
数据看板模块的可视化图展示较为简单，需要参考深度分析模块的设计效果进行优化。

**修复方案：**
修改 `taxi-analytics-frontend/src/views/dashboard/Index.vue`：
- 为趋势图、支付方式分布图、时段分布图添加渐变色效果
- 优化tooltip样式，使用白色背景、边框阴影、自定义格式化
- 添加图表动画效果
- 优化坐标轴和图例样式

**修改文件：**
- `taxi-analytics-frontend/src/views/dashboard/Index.vue`

---

### 问题4：供应商数据写死问题

**问题描述：**
数据看板模块中的供应商数据使用写死的模拟数据，而非从后端API获取真实数据。

**修复方案：**
1. 前端修改：
   - 在 `dashboard.ts` store中添加 `vendorAnalysis` 状态和 `fetchVendorAnalysis` 方法
   - 在 `dashboard.ts` API中添加 `getVendorAnalysis` 接口
   - 修改 `Index.vue` 使用真实数据替换写死数据

2. 后端修改：
   - 在 `DashboardKpiMapper.java` 中添加 `getVendorAnalysis` 查询方法
   - 在 `DashboardKpiService.java` 中添加接口定义
   - 在 `DashboardKpiServiceImpl.java` 中实现方法
   - 在 `DashboardController.java` 中添加API端点

**修改文件：**
- `taxi-analytics-frontend/src/stores/dashboard.ts`
- `taxi-analytics-frontend/src/api/dashboard.ts`
- `taxi-analytics-frontend/src/views/dashboard/Index.vue`
- `taxi-analytics-backend/src/main/java/com/taxi/analytics/modules/dashboard/mapper/DashboardKpiMapper.java`
- `taxi-analytics-backend/src/main/java/com/taxi/analytics/modules/dashboard/service/DashboardKpiService.java`
- `taxi-analytics-backend/src/main/java/com/taxi/analytics/modules/dashboard/service/impl/DashboardKpiServiceImpl.java`
- `taxi-analytics-backend/src/main/java/com/taxi/analytics/modules/dashboard/controller/DashboardController.java`

---

### 问题5：formatNumber函数小数位数格式化问题

**问题描述：**
`formatNumber` 函数不支持小数位数格式化，导致金额显示不够精确。

**修复方案：**
修改 `taxi-analytics-frontend/src/views/dashboard/Index.vue` 中的 `formatNumber` 函数：
- 添加 `decimals` 参数支持小数位数配置
- 使用 `toLocaleString` 实现千分位和小数格式化

**修改文件：**
- `taxi-analytics-frontend/src/views/dashboard/Index.vue`

---

## 修复验证

### 数据一致性验证
- 确保深度分析模块与数据看板模块使用相同的数据来源
- 验证指标数值与支付方式数据的一致性
- 确认所有数据均从后端API获取，无写死数据

### 功能验证
- 时间范围选择器正常工作，联动数据刷新
- 可视化图tooltip显示真实订单数，非整百整千
- 供应商数据动态加载，显示真实统计结果

---

## 工程化与规范化

### 代码规范
- 遵循项目现有的代码风格和命名规范
- 使用类型安全的DTO和接口定义
- 添加必要的空值检查和错误处理

### 性能优化
- 后端使用Caffeine缓存减少数据库查询
- 前端使用Pinia状态管理统一数据管理
- API调用支持日期范围参数过滤

### 日志记录
- 修复内容记录到修复日志文件
- 关键操作添加日志输出
- 异常处理记录错误信息

---

**修复人：** 开发团队
**审核人：** TBD
**测试状态：** 待测试
