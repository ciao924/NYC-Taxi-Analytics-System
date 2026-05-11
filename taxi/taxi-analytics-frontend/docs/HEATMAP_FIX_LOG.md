# 热力图模块修复日志

## 修复概述

修复时间：2026-05-09

修复人员：系统维护团队

## 问题描述

### 问题1：el-loading 组件未找到

**错误信息**：
```
[Vue warn]: Failed to resolve component: el-loading
```

**问题原因**：Element Plus 组件库中不存在名为 `el-loading` 的独立组件。Element Plus 提供加载状态的方式是：
- 指令 `v-loading`
- 图标组件 `<el-icon><Loading /></el-icon>`
- 全屏加载服务 `ElLoading.service()`

**影响范围**：UI 上期望的加载动画无法显示

---

### 问题2：高德地图 API 重命名警告

**错误信息**：
```
jsapi2.0 AMap.Heatmap is renamed, please use AMap.HeatMap
```

**问题原因**：高德地图 JS API 2.0 版本中，热力图插件从 `AMap.Heatmap` 更名为 `AMap.HeatMap`（注意大写 M）

**影响范围**：不影响执行，但可能导致后续 API 调用失败

---

### 问题3：heatmap.setDataSet is not a function

**错误信息**：
```
TypeError: heatmap.setDataSet is not a function
```

**问题原因**：由于使用了错误的 API 名称 `AMap.Heatmap`（小写 m），导致热力图对象创建失败，`heatmap` 实际为 `undefined`，因此没有 `setDataSet` 方法

**影响范围**：热力图完全无法渲染

---

## 解决方案

### 修复1：替换 el-loading 组件

将独立组件 `<el-loading>` 替换为 `v-loading` 指令

**修改位置**：`src/views/map/HeatmapView.vue:28`

```vue
<!-- 修改前 -->
<div id="heatmap-map" class="map-container" :class="{ 'map-hidden': !mapReady }"></div>
<div v-show="!mapReady && !error" class="loading-overlay">
  <div class="loading-content">
    <el-loading :visible="true" text="地图加载中..." />
  </div>
</div>

<!-- 修改后 -->
<div id="heatmap-map" class="map-container" v-loading="isMapLoading"></div>
```

---

### 修复2：修正高德地图 API 大小写

将 `AMap.Heatmap`（小写 m）修改为 `AMap.HeatMap`（大写 M）

**修改位置**：`src/views/map/HeatmapView.vue`

```typescript
// 修改前
plugins: ['AMap.Heatmap']
// 修改后
plugins: ['AMap.HeatMap']

// 修改前
heatmap = new AMap.Heatmap(map, { ... })
// 修改后
heatmap = new AMap.HeatMap(map, { ... })
```

---

### 修复3：增加 API 兼容性处理

在设置热力图数据时，增加对不同 API 版本的兼容性处理：

```typescript
if (typeof heatmap.setData === 'function') {
  heatmap.setData(heatmapData, maxCount);
} else if (typeof heatmap.setDataSet === 'function') {
  heatmap.setDataSet({
    data: heatmapData,
    max: maxCount
  });
} else {
  throw new Error('Heatmap data setter method not found');
}
```

---

### 修复4：增加前置检查和错误处理

在初始化热力图前，增加插件存在性检查：

```typescript
if (typeof AMap.HeatMap !== 'function') {
  throw new Error('AMap.HeatMap plugin not loaded');
}
```

---

### 修复5：修复日期参数问题

修复查询时使用固定日期的问题，改为使用用户选择的日期：

```typescript
// 修改前
return await mapApi.getPickupHeatmap({ date: '2025-03-31' });

// 修改后
return await mapApi.getPickupHeatmap({ date: selectedDate.value });
```

---

### 修复6：增加组件销毁时的资源清理

```typescript
onUnmounted(() => {
  if (heatmap) {
    heatmap.hide();
    heatmap = null;
  }
  if (map) {
    map.destroy();
    map = null;
  }
});
```

---

## 验证方式

1. 启动前端开发服务器：
   ```bash
   npm run dev
   ```

2. 访问热力图页面：
   ```
   http://localhost:5173/map/heatmap
   ```

3. 验证项目：
   - 页面加载时显示加载动画（v-loading 指令）
   - 地图成功加载后显示热力图
   - 控制台无报错信息
   - 可切换上车/下车热点类型
   - 可选择不同日期查询

---

## 修复结果

| 问题 | 状态 | 修复方式 |
|------|------|----------|
| el-loading 组件不存在 | ✅ 已修复 | 替换为 v-loading 指令 |
| AMap.Heatmap API 重命名 | ✅ 已修复 | 改为 AMap.HeatMap（大写 M） |
| setDataSet is not a function | ✅ 已修复 | 修正 API 名称后热力图对象正常创建 |
| 日期参数固定 | ✅ 已修复 | 使用用户选择的日期 |
| 资源未清理 | ✅ 已修复 | 添加 onUnmounted 生命周期清理 |

---

## 相关文件

- `src/views/map/HeatmapView.vue` - 热力图主组件
- `docs/HEATMAP_FIX_LOG.md` - 修复日志文档