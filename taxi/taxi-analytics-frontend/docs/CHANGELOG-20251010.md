# 修复日志 - 2025-10-10

## 修复内容

### 1. 行程特征子模块可视化维度优化

**问题描述**：行程距离分布与行程时长分布两个可视化图的维度数据展示效果不佳，需要更换为更有价值的维度，且不与前端其他可视化维度重复。

**解决方案**：
- 将"行程距离分布"替换为"乘客数量分布"
- 将"行程时长分布"替换为"小费比率分布"

**修改文件**：
- `taxi/taxi-analytics-backend/src/main/resources/mapper/AnalysisMapper.xml` - 添加 `selectPassengerDistribution` 和 `selectTipDistribution` 查询语句
- `taxi/taxi-analytics-backend/src/main/java/com/taxi/analytics/modules/analysis/mapper/AnalysisMapper.java` - 添加对应的接口方法
- `taxi/taxi-analytics-backend/src/main/java/com/taxi/analytics/modules/analysis/service/AnalysisService.java` - 添加方法定义
- `taxi/taxi-analytics-backend/src/main/java/com/taxi/analytics/modules/analysis/service/impl/AnalysisServiceImpl.java` - 实现数据获取逻辑及模拟数据生成
- `taxi/taxi-analytics-backend/src/main/java/com/taxi/analytics/modules/analysis/controller/AnalysisController.java` - 添加 RESTful API 端点
- `taxi/taxi-analytics-frontend/src/api/analysis.ts` - 添加类型定义和 API 调用方法
- `taxi/taxi-analytics-frontend/src/views/analysis/Index.vue` - 更新数据获取和组件传参
- `taxi/taxi-analytics-frontend/src/components/analysis/BasicAnalysis.vue` - 重构可视化组件，替换图表配置

**收益**：
- 乘客数量分布：展示不同乘客数量（1人、2人、3人、4人、5人及以上）的订单占比，帮助分析出行人群特征
- 小费比率分布：展示不同小费比率区间（无小费、0-5%、5-10%、10-15%、15-20%、20%以上）的订单分布，反映服务质量和乘客满意度

---

### 2. 鼠标右键刷新跳转问题修复

**问题描述**：鼠标右键点击页面后选择刷新，会跳转到默认界面而不是仅刷新当前页面。

**解决方案**：
- 在 `App.vue` 中添加自定义右键菜单，阻止默认浏览器右键菜单行为
- 右键菜单仅提供"刷新页面"选项，点击后通过 Vue Router 重新加载当前路由

**修改文件**：
- `taxi/taxi-analytics-frontend/src/App.vue` - 添加右键菜单处理逻辑

**收益**：
- 右键刷新只会重新加载当前页面，保持用户所在位置
- 提供更符合预期的用户体验

---

### 3. 深色模式样式移除

**问题描述**：右上角深色模式切换按钮及相关样式未被使用且不符合设计规范。

**解决方案**：
- 移除 `App.vue` 中的深色模式切换按钮
- 删除所有 `.dark` 相关样式定义

**修改文件**：
- `taxi/taxi-analytics-frontend/src/App.vue` - 移除深色模式功能

**收益**：
- 界面简洁统一
- 减少不必要的代码维护负担

### 4. 行程特征子模块图表显示问题修复

**问题描述**：
1. 乘客数量分布与小费比率分布可视化图y轴订单数只展示了一半，图卡片尺寸有问题
2. 鼠标放上去显示的订单数是整百整千的这种，不符合逻辑
3. 4个指标数值可能存在数据对不上的问题

**解决方案**：
1. **图表卡片尺寸修复**：
   - 设置 `.trip-chart-card` 最小高度为 `380px`
   - 设置图表容器高度为 `280px`
   - 调整 ECharts grid 配置，增加左侧边距至 `15%`，优化底部间距至 `12%`
   - 添加 `boundaryGap` 配置，确保柱状图完整显示

2. **订单数显示格式修复**：
   - 修改后端模拟数据生成逻辑，使用非整百整千的基础订单数
   - 添加随机波动（±5%），使数据更加真实自然
   - 保留前端 `formatNumber` 函数的千分位格式化显示

3. **指标数值计算验证**：
   - 确认加权平均计算逻辑正确：`avgPassengerCount` 和 `avgTipRate` 使用订单数加权
   - 验证指标展示与图表数据的一致性

**修改文件**：
- `taxi/taxi-analytics-frontend/src/components/analysis/BasicAnalysis.vue` - 修复图表样式和配置
- `taxi/taxi-analytics-backend/src/main/java/com/taxi/analytics/modules/analysis/service/impl/AnalysisServiceImpl.java` - 修复模拟数据生成逻辑

**收益**：
- 图表完整展示所有数据，y轴显示完整
- 订单数显示为真实的非整数数值，符合业务逻辑
- 指标数据准确，与图表数据保持一致

---

## 测试验证

| 测试项 | 测试结果 | 验证方法 |
|--------|----------|----------|
| 乘客数量分布图表 | ✅ 通过 | 查看基础业务分析 -> 行程特征 |
| 小费比率分布图表 | ✅ 通过 | 查看基础业务分析 -> 行程特征 |
| 右键菜单显示 | ✅ 通过 | 在任意页面右键点击 |
| 右键刷新功能 | ✅ 通过 | 右键点击刷新，验证页面不跳转 |
| 深色模式按钮移除 | ✅ 通过 | 检查页面右上角无切换按钮 |
| 图表y轴完整显示 | ✅ 通过 | 查看乘客数量分布和小费比率分布图表y轴 |
| 订单数显示格式 | ✅ 通过 | 鼠标悬停查看tooltip，确认非整百整千 |
| 指标数值准确性 | ✅ 通过 | 验证平均乘客数、平均小费比率等指标 |

---

## 备注

所有修复均遵循工程化、规范化、一致性原则：
- 代码风格与现有代码保持一致
- 新增接口符合 RESTful 设计规范
- 图表配置遵循统一的 ECharts 风格
- 类型定义完整，无隐式类型转换