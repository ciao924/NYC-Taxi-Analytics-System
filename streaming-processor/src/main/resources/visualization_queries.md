# 实时数据可视化查询文档

## 一、查询 SQL 清单

### 1. 今日累计指标

#### 今日累计订单
```sql
SELECT COUNT(*) FROM realtime_trip_green WHERE DATE(process_time) = CURDATE()
```

#### 今日累计营收
```sql
SELECT SUM(total_amount) FROM realtime_trip_green WHERE DATE(process_time) = CURDATE()
```

#### 今日平均 fare
```sql
SELECT AVG(fare_amount) FROM realtime_trip_green WHERE DATE(process_time) = CURDATE()
```

#### 今日平均 tip
```sql
SELECT AVG(tip_amount) FROM realtime_trip_green WHERE DATE(process_time) = CURDATE()
```

#### 今日平均距离
```sql
SELECT AVG(trip_distance) FROM realtime_trip_green WHERE DATE(process_time) = CURDATE()
```

### 2. 实时趋势图

#### 5分钟趋势
```sql
SELECT DATE_FORMAT(process_time, '%H:%i') as minute, COUNT(*) as cnt 
FROM realtime_trip_green 
WHERE process_time > DATE_SUB(NOW(), INTERVAL 30 MINUTE) 
GROUP BY minute
ORDER BY minute
```

#### 1小时趋势
```sql
SELECT DATE_FORMAT(process_time, '%H:00') as hour, COUNT(*) as cnt 
FROM realtime_trip_green 
WHERE process_time > DATE_SUB(NOW(), INTERVAL 6 HOUR) 
GROUP BY hour
ORDER BY hour
```

#### 营收趋势
```sql
SELECT DATE_FORMAT(process_time, '%H:%i') as minute, SUM(total_amount) as revenue 
FROM realtime_trip_green 
WHERE process_time > DATE_SUB(NOW(), INTERVAL 30 MINUTE) 
GROUP BY minute
ORDER BY minute
```

### 3. 热点区域

#### 热点上车区域 TOP10
```sql
SELECT pu_location_id, COUNT(*) as cnt 
FROM realtime_trip_green 
WHERE process_time > DATE_SUB(NOW(), INTERVAL 15 MINUTE) 
GROUP BY pu_location_id 
ORDER BY cnt DESC 
LIMIT 10
```

#### 热点下车区域 TOP10
```sql
SELECT do_location_id, COUNT(*) as cnt 
FROM realtime_trip_green 
WHERE process_time > DATE_SUB(NOW(), INTERVAL 15 MINUTE) 
GROUP BY do_location_id 
ORDER BY cnt DESC 
LIMIT 10
```

### 4. 最新订单列表

```sql
SELECT * 
FROM realtime_trip_green 
ORDER BY process_time DESC 
LIMIT 20
```

### 5. 数据质量统计

```sql
SELECT 
    COUNT(*) as total_records,
    SUM(CASE WHEN vendor_id IS NULL THEN 1 ELSE 0 END) as null_vendor_id,
    SUM(CASE WHEN pu_location_id IS NULL THEN 1 ELSE 0 END) as null_pu_location_id,
    SUM(CASE WHEN dropoff_datetime < pickup_datetime THEN 1 ELSE 0 END) as invalid_time,
    SUM(CASE WHEN trip_distance <= 0 OR trip_distance > 120 THEN 1 ELSE 0 END) as invalid_distance,
    SUM(CASE WHEN passenger_count < 1 OR passenger_count > 6 THEN 1 ELSE 0 END) as invalid_passenger_count,
    SUM(CASE WHEN fare_amount < 0 OR fare_amount > 800 THEN 1 ELSE 0 END) as invalid_fare_amount
FROM realtime_trip_green 
WHERE process_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)
```

## 二、DataEase 数据集创建指南

### 1. 创建数据源

1. 登录 DataEase 平台
2. 点击左侧菜单 "数据源"
3. 点击 "新建数据源"
4. 选择 "MySQL"
5. 填写以下信息：
   - 数据源名称：`实时出租车数据`
   - 主机：`localhost`
   - 端口：`3306`
   - 数据库：`nyc_taxi_realtime`
   - 用户名：`root`
   - 密码：`password`
6. 点击 "测试连接"，确保连接成功
7. 点击 "保存"

### 2. 创建数据集

1. 点击左侧菜单 "数据集"
2. 点击 "新建数据集"
3. 选择刚刚创建的 "实时出租车数据" 数据源
4. 选择 "SQL 查询"
5. 输入查询名称和 SQL 语句（例如 "今日累计订单"）
6. 点击 "预览"，确保数据正确
7. 点击 "保存"

### 3. 创建仪表板

1. 点击左侧菜单 "仪表板"
2. 点击 "新建仪表板"
3. 选择空白模板
4. 点击 "添加组件"
5. 选择 "图表"
6. 选择相应的数据集
7. 配置图表类型和字段
8. 点击 "保存"

### 4. 常用图表配置

#### 累计指标卡片
- 图表类型：数字卡片
- 数据集：选择相应的累计指标查询
- 配置：显示数值和标题

#### 趋势图
- 图表类型：折线图
- 数据集：选择相应的趋势查询
- X 轴：时间字段（minute 或 hour）
- Y 轴：计数或金额字段

#### 热点区域
- 图表类型：柱状图
- 数据集：选择相应的热点区域查询
- X 轴：区域 ID
- Y 轴：计数
- 排序：按计数降序

#### 最新订单
- 图表类型：表格
- 数据集：选择最新订单查询
- 配置：显示需要的字段

## 三、注意事项

1. 确保 MySQL 数据库和表已创建
2. 确保 Flink 作业正在运行，数据正在写入 MySQL
3. 实时查询可能会影响数据库性能，建议适当调整查询频率
4. 对于大量数据的查询，建议添加索引以提高性能
5. 数据保留时间为 24 小时，超过 24 小时的数据会被自动清理
