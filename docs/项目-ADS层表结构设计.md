# NYC Taxi ADS层数据库表结构文档

## 数据库概述

- **数据库名称**: nyc\_taxi\_ads

- **数据库说明**: 纽约出租车 ADS 层分析表

- **表数量**: 18 张

- **数据时间范围**: 2025 年 1 月 1 日 至 2025 年 3 月 31 日

- **适用场景**: AI 智能查询、数据分析、可视化报表

---

## 表结构总览

|序号|表名|中文名称|主要用途|
|---|---|---|---|
|1|analysis\_kpi\_daily|KPI 日报表|核心 KPI 指标统计|
|2|analysis\_hourly\_distribution|小时分布表|每小时订单分布分析|
|3|analysis\_weekday\_analysis|工作日分析表|工作日与周末分析|
|4|analysis\_payment\_analysis|支付方式分析表|各支付方式订单统计|
|5|analysis\_pickup\_hotspots|上车热点分析表|上车热点区域排名|
|6|analysis\_dropoff\_hotspots|下车热点分析表|下车热点区域排名|
|7|analysis\_borough\_flow|区域流量分析表|行政区之间流量分析|
|8|analysis\_fee\_composition|费用组成分析表|各项费用组成分析|
|9|analysis\_fee\_percentage|费用百分比分析表|费用占比分析|
|10|analysis\_fee\_by\_borough|区域费用分析表|各行政区费用统计|
|11|analysis\_fee\_trend|费用趋势表|费用趋势变化分析|
|12|analysis\_fee\_by\_taxi\_type|出租车类型费用表|不同车型费用对比|
|13|analysis\_distance\_distribution|距离分布分析表|行程距离分布统计|
|14|analysis\_duration\_distribution|时长分布分析表|行程时长分布统计|
|15|analysis\_passenger\_distribution|乘客人数分布表|乘客人数分布统计|
|16|analysis\_revenue\_contribution|收入贡献分析表|区域收入贡献排名|
|17|analysis\_airport|机场订单分析表|机场订单专项分析|
|18|analysis\_vendor|供应商分析表|供应商业绩分析|

---

## 详细表结构

### 1\. analysis\_kpi\_daily \- KPI 日报表

**说明**: 核心 KPI 指标日报表，记录每日整体运营数据

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|total\_trips|double|总订单数|452380|
|total\_revenue|double|总收入 \(美元\)|8934567\.50|
|avg\_fare|double|平均车费 \(美元\)|19\.75|
|avg\_distance|double|平均距离 \(英里\)|3\.25|
|avg\_duration|double|平均时长 \(分钟\)|15\.30|
|total\_tip|double|总小费 \(美元\)|892345\.60|
|avg\_tip|double|平均小费 \(美元\)|1\.97|
|airport\_trips|double|机场订单数|45678|
|peak\_hours|text|高峰时段|\&\#34;8,17,18\&\#34;|
|update\_time|text|更新时间|\-|

**常用查询场景**:

- 每日订单量统计

- 每日收入趋势

- 平均车费分析

- 机场订单占比

---

### 2\. analysis\_hourly\_distribution \- 小时分布表

**说明**: 记录每小时订单分布，用于分析出行高峰期

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|hour\_of\_day|int|小时 \(0\-23\)|8|
|trip\_count|double|订单数|18234|
|avg\_fare|double|平均车费 \(美元\)|18\.50|
|avg\_tip|double|平均小费 \(美元\)|2\.15|
|total\_revenue|double|总收入 \(美元\)|337329\.00|
|update\_time|text|更新时间|\-|

**常用查询场景**:

- 早高峰 / 晚高峰订单分析

- 每小时收入对比

- 凌晨订单分布

- 热门时段识别

---

### 3\. analysis\_weekday\_analysis \- 工作日分析表

**说明**: 分析工作日与周末的订单差异

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|day\_of\_week|int|星期 \(1\-7,1 = 周一\)|3|
|day\_of\_week\_name|text|星期名称|Tuesday|
|total\_trips|double|订单数|423456|
|total\_revenue|double|总收入 \(美元\)|7892345\.00|
|avg\_fare|double|平均车费 \(美元\)|18\.65|
|avg\_distance|double|平均距离 \(英里\)|3\.20|
|update\_time|text|更新时间|\-|

**常用查询场景**:

- 工作日 vs 周末订单对比

- 周五晚上订单高峰

- 工作日平均收入分析

- 周末出行偏好分析

---

### 4\. analysis\_payment\_analysis \- 支付方式分析表

**说明**: 分析各支付方式的使用情况

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|payment\_name|text|支付方式名称|Credit Card|
|is\_cashless|bit|是否无现金|1|
|trip\_count|double|订单数|312456|
|total\_amount|double|总金额 \(美元\)|5892345\.00|
|avg\_amount|double|平均金额 \(美元\)|18\.86|
|total\_tip|double|总小费 \(美元\)|456789\.00|
|avg\_tip|double|平均小费 \(美元\)|1\.46|
|trip\_ratio|double|订单占比|0\.69|
|update\_time|text|更新时间|\-|

**常用查询场景**:

- 信用卡 vs 现金支付占比

- 各支付方式平均小费

- 支付方式偏好分析

- 无现金交易趋势

---

### 5\. analysis\_pickup\_hotspots \- 上车热点分析表

**说明**: 记录上车热点区域及排名

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|zone\_name|text|上车区域名称|Upper East Side South|
|borough|text|行政区|Manhattan|
|service\_zone|text|服务区|Yellow Zone|
|trip\_count|double|上车次数|25678|
|total\_revenue|double|总收入 \(美元\)|577755\.00|
|update\_time|text|更新时间|\-|

**常用查询场景**:

- 最热门上车区域 TOP10

- 各行政区上车热度对比

---

### 6\. analysis\_dropoff\_hotspots \- 下车热点分析表

**说明**: 记录下车热点区域及排名

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|zone\_name|text|下车区域名称|Upper East Side South|
|borough|text|行政区|Manhattan|
|service\_zone|text|服务区|Yellow Zone|
|trip\_count|double|下车次数|24321|
|update\_time|text|更新时间|\-|

**常用查询场景**:

- 最热门下车区域 TOP10

---

### 7\. analysis\_borough\_flow \- 区域流量分析表

**说明**: 分析各行政区之间的流量流向

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|pu\_borough|text|上车行政区|Manhattan|
|do\_borough|text|下车行政区|Brooklyn|
|pickup\_count|double|上车次数|18234|
|dropoff\_count|double|下车次数|18234|
|update\_time|text|更新时间|\-|

**常用查询场景**:

- 跨区出行流量分析

---

### 8\. analysis\_fee\_composition \- 费用组成分析表

**说明**: 分析各项费用的组成结构

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|fee\_code|text|费用类型编码|fare|
|fee\_name|text|费用类型名称|Base Fare|
|total\_amount|double|总金额 \(美元\)|4567890\.00|
|update\_time|text|更新时间|\-|

---

### 9\. analysis\_fee\_percentage \- 费用百分比分析表

**说明**: 简化版费用占比分析

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|fee\_code|text|费用类型编码|tip|
|fee\_name|text|费用类型名称|Tips|
|percentage|double|占比|0\.15|
|update\_time|text|更新时间|\-|

---

### 10\. analysis\_fee\_by\_borough \- 区域费用分析表

**说明**: 按行政区统计费用情况

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|borough|text|行政区|Manhattan|
|trip\_count|double|订单数|289012|
|total\_revenue|double|总收入 \(美元\)|6789000\.00|
|avg\_fare|double|平均车费 \(美元\)|18\.90|
|total\_tip|double|总小费 \(美元\)|567890\.00|
|avg\_tip|double|平均小费 \(美元\)|1\.89|
|revenue\_ratio|double|收入占比|0\.45|
|update\_time|text|更新时间|\-|

---

### 11\. analysis\_fee\_trend \- 费用趋势表

**说明**: 记录各项费用的趋势变化

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|taxi\_type|text|出租车类型|Yellow|
|trip\_count|double|订单数|389012|
|total\_revenue|double|总收入 \(美元\)|7890123\.00|
|avg\_fare|double|平均车费 \(美元\)|18\.75|
|total\_tip|double|总小费 \(美元\)|789012\.00|
|avg\_tip\_rate|double|平均小费率|0\.12|
|cashless\_rate|double|无现金率|0\.68|
|revenue\_growth|double|收入增长率 \(%\)|2\.5|
|update\_time|text|更新时间|\-|

---

### 12\. analysis\_fee\_by\_taxi\_type \- 出租车类型费用表

**说明**: 分析不同出租车类型的费用差异

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|taxi\_type|text|出租车类型|Yellow|
|trip\_count|double|订单数|389012|
|total\_revenue|double|总收入 \(美元\)|9234567\.00|
|avg\_fare|double|平均车费 \(美元\)|20\.28|
|total\_tip|double|总小费 \(美元\)|789012\.00|
|avg\_tip|double|平均小费 \(美元\)|2\.03|
|tip\_rate|double|小费率|0\.085|
|update\_time|text|更新时间|\-|

---

### 13\. analysis\_distance\_distribution \- 距离分布分析表

**说明**: 分析行程距离的分布情况

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|distance\_range|text|距离区间 \(英里\)|1\-2 Miles|
|trip\_count|double|订单数|89234|
|avg\_distance|double|平均距离 \(英里\)|1\.5|
|update\_time|text|更新时间|\-|

---

### 14\. analysis\_duration\_distribution \- 时长分布分析表

**说明**: 分析行程时长的分布情况

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|duration\_range|text|时长区间 \(分钟\)|5\-10 Minutes|
|trip\_count|double|订单数|78234|
|avg\_duration|double|平均时长 \(分钟\)|7\.5|
|update\_time|text|更新时间|\-|

---

### 15\. analysis\_passenger\_distribution \- 乘客人数分布表

**说明**: 分析每单乘客人数分布

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|passenger\_count|int|乘客人数|2|
|passenger\_range|text|乘客范围描述|2 passengers|
|trip\_count|double|订单数|156789|
|update\_time|text|更新时间|\-|

---

### 16\. analysis\_revenue\_contribution \- 收入贡献分析表

**说明**: 分析各区域对总收入的贡献度

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|pu\_zone|text|上车区域|Upper East Side South|
|trip\_count|double|订单数|5678|
|total\_revenue|double|总收入 \(美元\)|156789\.00|
|revenue\_ratio|double|收入占比|0\.018|
|update\_time|text|更新时间|\-|

---

### 17\. analysis\_airport \- 机场订单分析表

**说明**: 专门分析机场相关订单

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|airport\_trip|text|机场类型|JFK|
|trip\_count|double|订单数|12345|
|trip\_ratio|double|订单占比|0\.15|
|update\_time|text|更新时间|\-|

---

### 18\. analysis\_vendor \- 供应商分析表

**说明**: 分析各供应商的业绩表现

|字段名|数据类型|中文描述|示例值|
|---|---|---|---|
|stat\_date|date|统计日期|2025\-01\-15|
|vendor\_name|text|供应商名称|Creative Mobile Technologies|
|trip\_count|double|订单数|245678|
|total\_revenue|double|总收入 \(美元\)|4567890\.00|
|avg\_fare|double|平均车费 \(美元\)|18\.60|
|avg\_distance|double|平均距离 \(英里\)|3\.20|
|revenue\_ratio|double|收入占比|0\.52|
|update\_time|text|更新时间|\-|

---

## 注意事项

1. **stat\_date 字段**: 所有表都包含此字段，用于按日期过滤数据

2. **数据时间范围**: 仅包含 **2025 年 1 月 1 日 至 2025 年 3 月 31 日** 的数据

3. **分区键**: stat\_date 是核心分区字段，查询时务必带上日期条件

4. **数据类型**: 金额 / 计数类字段主要使用 double，部分使用 int/bit/text

5. **时区说明**: 数据默认使用纽约时区 \(EST/EDT\)

---

*文档生成时间: 2026\-05\-15*
*文档版本: v1\.2 \(基于 CSV 修正 \+ 数据时间范围补充\)*

> （注：文档部分内容可能由 AI 生成）
