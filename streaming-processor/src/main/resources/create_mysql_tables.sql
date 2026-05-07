-- 创建数据库
CREATE DATABASE IF NOT EXISTS nyc_taxi_realtime;

USE nyc_taxi_realtime;

-- 绿表明细表
CREATE TABLE IF NOT EXISTS realtime_trip_green (
    -- 业务字段（与 Kafka JSON 一一对应）
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    -- 系统字段
    process_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- 索引（支撑可视化查询）
    INDEX idx_process_time (process_time),
    INDEX idx_pickup_date (DATE(pickup_datetime)),
    INDEX idx_pu_location (pu_location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='绿表实时明细数据，保留24小时';

-- 黄表明细表（预留）
CREATE TABLE IF NOT EXISTS realtime_trip_yellow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    process_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_process_time (process_time),
    INDEX idx_pickup_date (DATE(pickup_datetime)),
    INDEX idx_pu_location (pu_location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='黄表实时明细数据，保留24小时';

-- 死信队列表
CREATE TABLE IF NOT EXISTS dead_letter_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_message TEXT,
    error_reason VARCHAR(255),
    process_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- 索引
    INDEX idx_process_time (process_time),
    INDEX idx_error_reason (error_reason)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='死信队列，存储处理失败的消息';

-- 启用事件调度器
SET GLOBAL event_scheduler = ON;

-- 创建事件：每小时清理24小时前的数据
CREATE EVENT IF NOT EXISTS clean_realtime_trip_green
ON SCHEDULE EVERY 1 HOUR
DO
    DELETE FROM realtime_trip_green
    WHERE process_time < DATE_SUB(NOW(), INTERVAL 24 HOUR);

CREATE EVENT IF NOT EXISTS clean_realtime_trip_yellow
ON SCHEDULE EVERY 1 HOUR
DO
    DELETE FROM realtime_trip_yellow
    WHERE process_time < DATE_SUB(NOW(), INTERVAL 24 HOUR);

-- 清理死信队列：保留7天数据
CREATE EVENT IF NOT EXISTS clean_dead_letter_queue
ON SCHEDULE EVERY 1 HOUR
DO
    DELETE FROM dead_letter_queue
    WHERE process_time < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- ==================== ADS 指标表（支持幂等写入）====================

-- 订单指标表
CREATE TABLE IF NOT EXISTS realtime_order_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    window_start DATETIME NOT NULL,
    window_end DATETIME NOT NULL,
    city VARCHAR(50) NOT NULL DEFAULT 'NYC',
    order_count BIGINT NOT NULL DEFAULT 0,
    total_fare DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    avg_fare DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    process_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_window (window_start, window_end, city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='实时订单指标表，支持幂等写入';

-- 热点区域TopN表
CREATE TABLE IF NOT EXISTS realtime_hotspot_topn (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    window_start DATETIME NOT NULL,
    window_end DATETIME NOT NULL,
    zone VARCHAR(100) NOT NULL,
    cnt BIGINT NOT NULL DEFAULT 0,
    rank INT NOT NULL,
    process_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_window_zone_rank (window_start, window_end, zone, rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='热点区域TopN表，支持幂等写入';

-- 费用构成表
CREATE TABLE IF NOT EXISTS realtime_fee_composition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    window_start DATETIME NOT NULL,
    window_end DATETIME NOT NULL,
    payment_type VARCHAR(50) NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    process_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_window_payment (window_start, window_end, payment_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='费用构成表，支持幂等写入';
