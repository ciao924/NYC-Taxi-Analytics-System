-- 创建实时监控表并插入测试数据

-- 订单指标表
CREATE TABLE IF NOT EXISTS `realtime_order_metrics` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `window_end` DATETIME NOT NULL,
    `trip_count` BIGINT NOT NULL DEFAULT 0,
    `total_fare` DECIMAL(18,2) NOT NULL DEFAULT 0,
    `avg_fare` DECIMAL(10,2) NOT NULL DEFAULT 0,
    `avg_distance` DECIMAL(10,2) NOT NULL DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 热点区域表
CREATE TABLE IF NOT EXISTS `realtime_hotspot_topn` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `window_end` DATETIME NOT NULL,
    `zone` VARCHAR(100) NOT NULL,
    `trip_count` BIGINT NOT NULL DEFAULT 0,
    `rank` INT NOT NULL DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 费用构成表
CREATE TABLE IF NOT EXISTS `realtime_fee_composition` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `window_end` DATETIME NOT NULL,
    `payment_type` VARCHAR(20) NOT NULL,
    `trip_count` BIGINT NOT NULL DEFAULT 0,
    `total_fare` DECIMAL(18,2) NOT NULL DEFAULT 0,
    `avg_fare` DECIMAL(10,2) NOT NULL DEFAULT 0,
    `tip_rate` DECIMAL(5,2) NOT NULL DEFAULT 0,
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试数据（模拟5月7日的数据）
INSERT INTO `realtime_order_metrics` (`window_end`, `trip_count`, `total_fare`, `avg_fare`, `avg_distance`) VALUES
('2026-05-07 10:05:00', 1250, 28750.00, 23.00, 4.5),
('2026-05-07 10:10:00', 1320, 30360.00, 23.00, 4.6),
('2026-05-07 10:15:00', 1450, 34800.00, 24.00, 4.8),
('2026-05-07 10:20:00', 1280, 30720.00, 24.00, 4.7),
('2026-05-07 10:25:00', 1520, 36480.00, 24.00, 4.9),
('2026-05-07 10:30:00', 1380, 33120.00, 24.00, 4.8),
('2026-05-07 10:35:00', 1420, 34080.00, 24.00, 4.7),
('2026-05-07 10:40:00', 1350, 32400.00, 24.00, 4.6),
('2026-05-07 10:45:00', 1480, 35520.00, 24.00, 4.8),
('2026-05-07 10:50:00', 1550, 37200.00, 24.00, 4.9),
('2026-05-07 10:55:00', 1400, 33600.00, 24.00, 4.7),
('2026-05-07 11:00:00', 1680, 42000.00, 25.00, 5.0);

-- 插入热点区域数据
INSERT INTO `realtime_hotspot_topn` (`window_end`, `zone`, `trip_count`, `rank`) VALUES
('2026-05-07 11:00:00', 'Manhattan', 450, 1),
('2026-05-07 11:00:00', 'Brooklyn', 320, 2),
('2026-05-07 11:00:00', 'Queens', 280, 3),
('2026-05-07 11:00:00', 'Bronx', 180, 4),
('2026-05-07 11:00:00', 'Staten Island', 120, 5),
('2026-05-07 11:00:00', 'JFK Airport', 95, 6),
('2026-05-07 11:00:00', 'LaGuardia', 85, 7),
('2026-05-07 11:00:00', 'Times Square', 78, 8),
('2026-05-07 11:00:00', 'Central Park', 65, 9),
('2026-05-07 11:00:00', 'Wall Street', 55, 10);

-- 插入费用构成数据
INSERT INTO `realtime_fee_composition` (`window_end`, `payment_type`, `trip_count`, `total_fare`, `avg_fare`, `tip_rate`) VALUES
('2026-05-07 11:00:00', '1', 980, 24500.00, 25.00, 15.5),
('2026-05-07 11:00:00', '2', 420, 10500.00, 25.00, 8.2),
('2026-05-07 11:00:00', '3', 180, 4500.00, 25.00, 0.0),
('2026-05-07 11:00:00', '4', 60, 1500.00, 25.00, 0.0),
('2026-05-07 11:00:00', '5', 40, 1000.00, 25.00, 12.0);