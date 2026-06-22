# chart\_config 图表配置规范

## 1\. 整体说明

chart\_config 是智能体在生成 SQL 时必须同时输出的图表可视化配置，用于前端自动渲染高质量图表。
输出位置：仅在第二优先级（生成 SQL 模式）中使用，作为 JSON 对象的一部分。

## 2\. 字段规范总览

|字段名|类型|是否必填|默认值|说明|
|---|---|---|---|---|
|chart\_type|string|是|\-|图表类型|
|title|string|是|\-|图表标题（中文，清晰且专业）|
|x\_field|string|是|\-|X 轴字段名|
|y\_field|string/array|是|\-|Y 轴主指标字段（支持单个字符串或数组）|
|y2\_field|string|否|null|副 Y 轴字段（用于双轴图）|
|group\_field|string|否|null|分组 / 系列字段（用于堆叠柱状图、多系列线图）|
|sort|string|否|null|排序方式：desc、asc|
|limit|integer|否|null|显示前 N 项（常用于 TOPN）|
|legend|boolean|否|true|是否显示图例|
|percentage|boolean|否|false|是否显示百分比（适用于饼图、堆叠图）|
|description|string|是|\-|一句话描述该图表业务含义|
|tooltip\_format|string|否|null|提示框格式提示（如金额加 $、保留 2 位小数）|

## 3\. chart\_type 支持类型

|chart\_type|适用场景|推荐使用场景|
|---|---|---|
|line|趋势分析|时间序列、趋势变化|
|bar|对比、排名|TOPN、分类对比|
|stacked\_bar|堆叠对比|组成结构、多分类|
|pie|占比分析|支付方式、车型占比|
|dual\_axis|双指标趋势|收入 \+ 订单量、收入 \+ 小费|
|table|明细数据、长列表|详细报表、热点列表|
|horizontal\_bar|横向条形图（名称较长时）|区域名称、路线名称|

## 4\. 配置示例

### 示例 1：趋势图（单指标）

```json
{
  "chart_type": "line",
  "title": "最近7天总收入趋势",
  "x_field": "stat_date",
  "y_field": "total_revenue",
  "description": "展示最近7天每日总收入变化情况"
}
```

### 示例 2：TOPN 柱状图

```json
{
  "chart_type": "bar",
  "title": "最近7天上车热点区域TOP10",
  "x_field": "pu_zone",
  "y_field": "pickup_count",
  "sort": "desc",
  "limit": 10,
  "description": "上车次数最多的前10个区域"
}
```

### 示例 3：双轴图（收入 \+ 订单）

```json
{
  "chart_type": "dual_axis",
  "title": "最近30天收入与订单量趋势",
  "x_field": "stat_date",
  "y_field": "total_revenue",
  "y2_field": "total_trips",
  "description": "橙色折线为收入，蓝色柱状为订单量"
}
```

### 示例 4：堆叠柱状图

```json
{
  "chart_type": "stacked_bar",
  "title": "最近7天各支付方式收入组成",
  "x_field": "stat_date",
  "y_field": "total_amount",
  "group_field": "payment_name",
  "percentage": true,
  "description": "按支付方式堆叠显示每日收入构成"
}
```

### 示例 5：饼图

```json
{
  "chart_type": "pie",
  "title": "最近30天出租车类型订单占比",
  "x_field": "taxi_type",
  "y_field": "trip_count",
  "percentage": true,
  "description": "黄车与绿车订单分布情况"
}
```

## 5\. 使用规则

title 必须使用自然流畅的中文。
description 必须简洁说明业务价值。
y\_field 支持数组形式（如 \[\&\#34;total\_revenue\&\#34;, \&\#34;total\_tip\&\#34;\]），用于多 Y 轴同类型图。
前端可根据 chart\_type 自动选择对应图表组件。

> （注：文档部分内容可能由 AI 生成）
