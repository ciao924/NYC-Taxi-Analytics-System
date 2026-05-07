-- 插入一些模拟数据以便于前端测试
INSERT IGNORE INTO `analysis_kpi_daily` (`stat_date`, `total_trips`, `total_revenue`, `avg_fare`, `avg_distance`, `avg_duration`, `total_tip`, `avg_tip`, `airport_trips`, `peak_hour`)
VALUES
('2025-01-01', 120500, 2500000.00, 20.75, 4.2, 15.5, 300000.00, 2.49, 5000, '17:00-19:00'),
('2025-01-02', 115000, 2350000.00, 20.43, 4.1, 14.8, 280000.00, 2.43, 4800, '08:00-10:00'),
('2025-01-03', 130000, 2700000.00, 20.77, 4.3, 16.0, 320000.00, 2.46, 5200, '17:00-19:00'),
('2025-01-04', 145000, 3100000.00, 21.38, 4.5, 16.5, 380000.00, 2.62, 6000, '18:00-20:00'),
('2025-01-05', 100000, 2100000.00, 21.00, 4.4, 14.0, 250000.00, 2.50, 4500, '12:00-14:00');

INSERT IGNORE INTO `analysis_hourly_distribution` (`stat_date`, `hour_of_day`, `trip_count`) VALUES
('2025-01-01', 8, 8500),
('2025-01-01', 9, 10200),
('2025-01-01', 17, 12000),
('2025-01-01', 18, 13500);

INSERT IGNORE INTO `analysis_weekday_analysis` (`stat_date`, `day_of_week`, `day_of_week_name`, `total_trips`, `total_revenue`, `avg_fare`, `avg_distance`) VALUES
('2025-01-01', 3, 'Wednesday', 120500, 2500000.00, 20.75, 4.2),
('2025-01-02', 4, 'Thursday', 115000, 2350000.00, 20.43, 4.1);

INSERT IGNORE INTO `analysis_airport` (`stat_date`, `airport_trip`, `trip_count`, `trip_ratio`) VALUES
('2025-01-01', 'JFK', 3500, 2.90),
('2025-01-01', 'LaGuardia', 1500, 1.24);
