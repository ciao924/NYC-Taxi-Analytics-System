-- 创建 Hive ODS 数据库
CREATE DATABASE IF NOT EXISTS nyc_taxi_ods;

USE nyc_taxi_ods;

-- 绿表 ODS 表（分区表，按日期分区）
CREATE TABLE IF NOT EXISTS ods_trip_green (
    vendor_id INT,
    pickup_datetime DATETIME,
    dropoff_datetime DATETIME,
    passenger_count INT,
    trip_distance DECIMAL(10,2),
    pu_location_id INT,
    do_location_id INT,
    fare_amount DECIMAL(10,2),
    tip_amount DECIMAL(10,2),
    total_amount DECIMAL(10,2),
    process_time DATETIME
) 
PARTITIONED BY (dt STRING) 
STORED AS PARQUET
TBLPROPERTIES (
    'parquet.compression'='SNAPPY',
    'partition.time.format'='yyyy-MM-dd'
);

-- 黄表 ODS 表（分区表，按日期分区）
CREATE TABLE IF NOT EXISTS ods_trip_yellow (
    vendor_id INT,
    pickup_datetime DATETIME,
    dropoff_datetime DATETIME,
    passenger_count INT,
    trip_distance DECIMAL(10,2),
    pu_location_id INT,
    do_location_id INT,
    fare_amount DECIMAL(10,2),
    tip_amount DECIMAL(10,2),
    total_amount DECIMAL(10,2),
    process_time DATETIME
) 
PARTITIONED BY (dt STRING) 
STORED AS PARQUET
TBLPROPERTIES (
    'parquet.compression'='SNAPPY',
    'partition.time.format'='yyyy-MM-dd'
);
