# Few\-shot 示例

## 示例 1：简单查询 → 输出 JSON DSL

用户查询：最近 7 天总收入趋势，按日期展示，用折线图
正确输出：

```json
{
  "metric": "revenue",
  "time_range": "7d",
  "dimension": "date",
  "filters": {},
  "chart": "line"
}
```

## 示例 2：带筛选的简单查询 → 输出 JSON DSL

用户查询：最近 30 天黄车（Yellow）的订单数，按小时分布
正确输出：

```json
{
  "metric": "order_cnt",
  "time_range": "30d",
  "dimension": "hour",
  "filters": {
    "vendor": "yellow"
  },
  "chart": "bar"
}
```

## 示例 3：复杂查询 → 生成 SQL（第二优先级）

用户查询：最近 7 天各行政区收入排名 TOP10
正确输出：

```sql
SELECT 
    borough,
    SUM(total_revenue) AS total_revenue,
    ROUND(SUM(total_revenue) * 100.0 / SUM(SUM(total_revenue)) OVER (), 2) AS revenue_ratio
FROM analysis_fee_by_borough 
WHERE stat_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
GROUP BY borough
ORDER BY total_revenue DESC 
LIMIT 10;
```

解释：查询最近 7 天各行政区总收入及收入占比排名（TOP10）。

## 示例 4：复杂查询 → 生成 SQL

用户查询：Manhattan 上车到 Brooklyn 下车的跨区流量及平均车费，最近 30 天
正确输出：

```sql
SELECT 
    stat_date,
    SUM(trip_count) AS trip_count,
    ROUND(AVG(avg_distance), 2) AS avg_distance,
    ROUND(AVG(avg_duration), 2) AS avg_duration,
    SUM(total_revenue) AS total_revenue,
    ROUND(AVG(avg_fare), 2) AS avg_fare
FROM analysis_borough_flow 
WHERE stat_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
  AND pu_borough = 'Manhattan' 
  AND do_borough = 'Brooklyn'
GROUP BY stat_date
ORDER BY stat_date;
```

解释：查询最近 30 天 Manhattan 到 Brooklyn 的每日跨区订单量、平均距离、时长和收入趋势。

## 示例 5：复杂查询 → 生成 SQL

用户查询：机场订单分析，最近 7 天 JFK 机场的订单数和收入占比
正确输出：

```sql
SELECT 
    stat_date,
    SUM(trip_count) AS trip_count,
    SUM(total_revenue) AS total_revenue,
    ROUND(SUM(total_revenue) * 100.0 / SUM(SUM(total_revenue)) OVER (), 2) AS percentage
FROM analysis_airport 
WHERE stat_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
  AND airport_trip = 'JFK'
GROUP BY stat_date
ORDER BY stat_date;
```

解释：查询最近 7 天 JFK 机场每日订单数、收入及占总收入比例。

## 示例 6：多维度对比查询 → 生成 SQL

用户查询：黄车和绿车最近 30 天的收入和订单对比
正确输出：

```sql
SELECT 
    stat_date,
    taxi_type,
    SUM(trip_count) AS trip_count,
    SUM(total_revenue) AS total_revenue,
    ROUND(AVG(avg_fare), 2) AS avg_fare
FROM analysis_fee_by_taxi_type 
WHERE stat_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY stat_date, taxi_type
ORDER BY stat_date, total_revenue DESC;
```

解释：对比最近 30 天黄车和绿车的每日订单数、总收入和平均车费。

## 示例 7：无法识别 → 输出 parse\_failed

用户查询：帮我预测下个月收入
正确输出：

```json
{
  "parse_failed": true,
  "error_msg": "无法理解查询，请描述您想要的指标（如收入、订单数）、时间范围和对比维度"
}
```

> （注：文档部分内容可能由 AI 生成）
