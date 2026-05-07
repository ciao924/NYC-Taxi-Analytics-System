-- 分析系统初始化建表脚本

CREATE TABLE IF NOT EXISTS `analysis_kpi_daily` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_trips` int(11) DEFAULT 0 COMMENT '总单量',
  `total_revenue` decimal(10,2) DEFAULT 0.00 COMMENT '总收入',
  `avg_fare` decimal(10,2) DEFAULT 0.00 COMMENT '平均客单价',
  `avg_distance` decimal(10,2) DEFAULT 0.00 COMMENT '平均行驶距离',
  `avg_duration` decimal(10,2) DEFAULT 0.00 COMMENT '平均行驶时间',
  `total_tip` decimal(10,2) DEFAULT 0.00 COMMENT '总小费',
  `avg_tip` decimal(10,2) DEFAULT 0.00 COMMENT '平均小费',
  `airport_trips` int(11) DEFAULT 0 COMMENT '机场单量',
  `peak_hour` varchar(10) DEFAULT '' COMMENT '早晚高峰',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日核心KPI统计';

CREATE TABLE IF NOT EXISTS `analysis_airport` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `airport_trip` varchar(50) DEFAULT NULL,
  `trip_count` int(11) DEFAULT 0,
  `trip_ratio` decimal(5,2) DEFAULT 0.00,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_borough_flow` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `pu_borough` varchar(50) DEFAULT NULL,
  `do_borough` varchar(50) DEFAULT NULL,
  `trip_count` int(11) DEFAULT 0,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_distance_distribution` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `distance_range` varchar(50) DEFAULT NULL,
  `trip_count` int(11) DEFAULT 0,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_dropoff_hotspots` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `zone_name` varchar(100) DEFAULT NULL,
  `borough` varchar(50) DEFAULT NULL,
  `service_zone` varchar(50) DEFAULT NULL,
  `trip_count` int(11) DEFAULT 0,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_duration_distribution` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `duration_range` varchar(50) DEFAULT NULL,
  `trip_count` int(11) DEFAULT 0,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_fee_composition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `total_fare` decimal(10,2) DEFAULT 0.00,
  `total_extra` decimal(10,2) DEFAULT 0.00,
  `total_mta_tax` decimal(10,2) DEFAULT 0.00,
  `total_tip` decimal(10,2) DEFAULT 0.00,
  `total_tolls` decimal(10,2) DEFAULT 0.00,
  `total_improvement_surcharge` decimal(10,2) DEFAULT 0.00,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_fee_percentage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `fee_code` varchar(50) DEFAULT NULL,
  `fee_name` varchar(50) DEFAULT NULL,
  `percentage` decimal(5,2) DEFAULT 0.00,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_hourly_distribution` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `hour_of_day` int(11) DEFAULT 0,
  `trip_count` int(11) DEFAULT 0,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_passenger_distribution` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `passenger_count` int(11) DEFAULT 0,
  `passenger_range` varchar(50) DEFAULT NULL,
  `trip_count` int(11) DEFAULT 0,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_payment_analysis` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `payment_name` varchar(50) DEFAULT NULL,
  `is_cashless` int(11) DEFAULT 0,
  `trip_count` int(11) DEFAULT 0,
  `total_amount` decimal(10,2) DEFAULT 0.00,
  `avg_amount` decimal(10,2) DEFAULT 0.00,
  `total_tip` decimal(10,2) DEFAULT 0.00,
  `avg_tip` decimal(10,2) DEFAULT 0.00,
  `trip_ratio` decimal(5,2) DEFAULT 0.00,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_pickup_hotspots` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `zone_name` varchar(100) DEFAULT NULL,
  `borough` varchar(50) DEFAULT NULL,
  `service_zone` varchar(50) DEFAULT NULL,
  `trip_count` int(11) DEFAULT 0,
  `total_revenue` decimal(10,2) DEFAULT 0.00,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_revenue_contribution` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `pu_zone` varchar(100) DEFAULT NULL,
  `trip_count` int(11) DEFAULT 0,
  `total_revenue` decimal(10,2) DEFAULT 0.00,
  `revenue_ratio` decimal(5,2) DEFAULT 0.00,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_vendor` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `vendor_name` varchar(50) DEFAULT NULL,
  `trip_count` int(11) DEFAULT 0,
  `total_revenue` decimal(10,2) DEFAULT 0.00,
  `avg_fare` decimal(10,2) DEFAULT 0.00,
  `avg_distance` decimal(10,2) DEFAULT 0.00,
  `revenue_ratio` decimal(5,2) DEFAULT 0.00,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `analysis_weekday_analysis` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `day_of_week` int(11) DEFAULT 0,
  `day_of_week_name` varchar(20) DEFAULT NULL,
  `total_trips` int(11) DEFAULT 0,
  `total_revenue` decimal(10,2) DEFAULT 0.00,
  `avg_fare` decimal(10,2) DEFAULT 0.00,
  `avg_distance` decimal(10,2) DEFAULT 0.00,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
