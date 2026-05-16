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
('2025-01-01', 18, 13500),
('2025-01-02', 8, 9000),
('2025-01-02', 9, 11000),
('2025-01-02', 17, 12500),
('2025-01-02', 18, 14000);

INSERT IGNORE INTO `analysis_weekday_analysis` (`stat_date`, `day_of_week`, `day_of_week_name`, `total_trips`, `total_revenue`, `avg_fare`, `avg_distance`) VALUES
('2025-01-01', 3, 'Wednesday', 120500, 2500000.00, 20.75, 4.2),
('2025-01-02', 4, 'Thursday', 115000, 2350000.00, 20.43, 4.1),
('2025-01-03', 5, 'Friday', 130000, 2700000.00, 20.77, 4.3),
('2025-01-04', 6, 'Saturday', 145000, 3100000.00, 21.38, 4.5),
('2025-01-05', 7, 'Sunday', 100000, 2100000.00, 21.00, 4.4);

INSERT IGNORE INTO `analysis_airport` (`stat_date`, `airport_trip`, `trip_count`, `trip_ratio`) VALUES
('2025-01-01', 'JFK', 3500, 2.90),
('2025-01-01', 'LaGuardia', 1500, 1.24),
('2025-01-01', 'non_airport', 115500, 95.86),
('2025-01-02', 'JFK', 3200, 2.78),
('2025-01-02', 'LaGuardia', 1600, 1.39),
('2025-01-02', 'non_airport', 110200, 95.83);

INSERT IGNORE INTO `analysis_vendor` (`stat_date`, `vendor_name`, `trip_count`, `total_revenue`, `avg_fare`, `avg_distance`, `revenue_ratio`) VALUES
('2025-01-01', '1', 65000, 1350000.00, 20.77, 4.1, 54.00),
('2025-01-01', '2', 55500, 1150000.00, 20.72, 4.3, 46.00),
('2025-01-02', '1', 62000, 1280000.00, 20.65, 4.0, 54.47),
('2025-01-02', '2', 53000, 1070000.00, 20.19, 4.2, 45.53);

INSERT IGNORE INTO `analysis_payment_analysis` (`stat_date`, `payment_name`, `is_cashless`, `trip_count`, `total_amount`, `avg_amount`, `total_tip`, `avg_tip`, `trip_ratio`) VALUES
('2025-01-01', 'Credit Card', 1, 75000, 1650000.00, 22.00, 250000.00, 3.33, 62.24),
('2025-01-01', 'Cash', 0, 40000, 700000.00, 17.50, 40000.00, 1.00, 33.20),
('2025-01-01', 'No Charge', 1, 3000, 60000.00, 20.00, 0.00, 0.00, 2.49),
('2025-01-01', 'Dispute', 1, 2000, 50000.00, 25.00, 0.00, 0.00, 1.66),
('2025-01-01', 'Unknown', 1, 500, 40000.00, 80.00, 10000.00, 20.00, 0.41),
('2025-01-02', 'Credit Card', 1, 72000, 1580000.00, 21.94, 240000.00, 3.33, 62.61),
('2025-01-02', 'Cash', 0, 38000, 660000.00, 17.37, 38000.00, 1.00, 33.04),
('2025-01-02', 'No Charge', 1, 2800, 56000.00, 20.00, 0.00, 0.00, 2.43),
('2025-01-02', 'Dispute', 1, 1800, 45000.00, 25.00, 0.00, 0.00, 1.57),
('2025-01-02', 'Unknown', 1, 400, 9000.00, 22.50, 2000.00, 5.00, 0.35);

INSERT IGNORE INTO `analysis_distance_distribution` (`stat_date`, `distance_range`, `trip_count`) VALUES
('2025-01-01', '0-5英里', 65000),
('2025-01-01', '5-10英里', 35000),
('2025-01-01', '10-20英里', 15000),
('2025-01-01', '20-50英里', 4000),
('2025-01-01', '50-120英里', 1500),
('2025-01-02', '0-5英里', 62000),
('2025-01-02', '5-10英里', 33000),
('2025-01-02', '10-20英里', 14000),
('2025-01-02', '20-50英里', 3800),
('2025-01-02', '50-120英里', 1200);

INSERT IGNORE INTO `analysis_duration_distribution` (`stat_date`, `duration_range`, `trip_count`) VALUES
('2025-01-01', '0-10分钟', 35000),
('2025-01-01', '10-20分钟', 45000),
('2025-01-01', '20-30分钟', 25000),
('2025-01-01', '30-60分钟', 12000),
('2025-01-01', '60分钟以上', 3500),
('2025-01-02', '0-10分钟', 33000),
('2025-01-02', '10-20分钟', 43000),
('2025-01-02', '20-30分钟', 24000),
('2025-01-02', '30-60分钟', 11000),
('2025-01-02', '60分钟以上', 3000);

INSERT IGNORE INTO `analysis_passenger_distribution` (`stat_date`, `passenger_count`, `passenger_range`, `trip_count`) VALUES
('2025-01-01', 1, '1人', 75000),
('2025-01-01', 2, '2人', 28000),
('2025-01-01', 3, '3人', 12000),
('2025-01-01', 4, '4人', 4000),
('2025-01-01', 5, '5人及以上', 1500),
('2025-01-02', 1, '1人', 72000),
('2025-01-02', 2, '2人', 26000),
('2025-01-02', 3, '3人', 11000),
('2025-01-02', 4, '4人', 3800),
('2025-01-02', 5, '5人及以上', 1200);

INSERT IGNORE INTO `analysis_fee_composition` (`stat_date`, `total_fare`, `total_extra`, `total_mta_tax`, `total_tip`, `total_tolls`, `total_improvement_surcharge`) VALUES
('2025-01-01', 1800000.00, 150000.00, 60000.00, 300000.00, 120000.00, 70000.00),
('2025-01-02', 1700000.00, 140000.00, 57000.00, 280000.00, 110000.00, 63000.00);

INSERT IGNORE INTO `analysis_revenue_contribution` (`stat_date`, `pu_zone`, `trip_count`, `total_revenue`, `revenue_ratio`) VALUES
('2025-01-01', 'Manhattan', 45000, 1000000.00, 40.00),
('2025-01-01', 'Brooklyn', 30000, 600000.00, 24.00),
('2025-01-01', 'Queens', 25000, 500000.00, 20.00),
('2025-01-01', 'Bronx', 12000, 250000.00, 10.00),
('2025-01-01', 'Staten Island', 8500, 150000.00, 6.00),
('2025-01-02', 'Manhattan', 43000, 950000.00, 40.43),
('2025-01-02', 'Brooklyn', 28000, 560000.00, 23.83),
('2025-01-02', 'Queens', 24000, 480000.00, 20.43),
('2025-01-02', 'Bronx', 11500, 230000.00, 9.79),
('2025-01-02', 'Staten Island', 8500, 130000.00, 5.52);

INSERT IGNORE INTO `analysis_borough_flow` (`stat_date`, `pu_borough`, `do_borough`, `trip_count`) VALUES
('2025-01-01', 'Manhattan', 'Manhattan', 25000),
('2025-01-01', 'Manhattan', 'Brooklyn', 10000),
('2025-01-01', 'Manhattan', 'Queens', 8000),
('2025-01-01', 'Brooklyn', 'Manhattan', 8000),
('2025-01-01', 'Brooklyn', 'Brooklyn', 12000),
('2025-01-01', 'Queens', 'Manhattan', 6000),
('2025-01-01', 'Queens', 'Queens', 10000);

INSERT IGNORE INTO `analysis_pickup_hotspots` (`stat_date`, `zone_name`, `borough`, `service_zone`, `trip_count`, `total_revenue`) VALUES
('2025-01-01', 'Midtown Center', 'Manhattan', 'Yellow Zone', 15000, 350000.00),
('2025-01-01', 'Times Square', 'Manhattan', 'Yellow Zone', 12000, 300000.00),
('2025-01-01', 'JFK Airport', 'Queens', 'Airport Zone', 8000, 250000.00),
('2025-01-01', 'LaGuardia Airport', 'Queens', 'Airport Zone', 5000, 150000.00),
('2025-01-01', 'Downtown Brooklyn', 'Brooklyn', 'Yellow Zone', 6000, 120000.00);

INSERT IGNORE INTO `analysis_dropoff_hotspots` (`stat_date`, `zone_name`, `borough`, `service_zone`, `trip_count`) VALUES
('2025-01-01', 'Midtown Center', 'Manhattan', 'Yellow Zone', 14000),
('2025-01-01', 'Times Square', 'Manhattan', 'Yellow Zone', 11000),
('2025-01-01', 'JFK Airport', 'Queens', 'Airport Zone', 7500),
('2025-01-01', 'LaGuardia Airport', 'Queens', 'Airport Zone', 4500),
('2025-01-01', 'Downtown Brooklyn', 'Brooklyn', 'Yellow Zone', 5500);

INSERT IGNORE INTO `analysis_tip_distribution` (`stat_date`, `tip_range`, `trip_count`, `avg_tip`, `tip_rate`) VALUES
('2025-01-01', '无小费', 45000, 0.00, 0.00),
('2025-01-01', '0-5%', 15000, 0.80, 3.50),
('2025-01-01', '5-10%', 25000, 1.50, 7.50),
('2025-01-01', '10-15%', 20000, 2.80, 12.50),
('2025-01-01', '15-20%', 10000, 4.00, 17.50),
('2025-01-01', '20%以上', 5500, 6.50, 25.00),
('2025-01-02', '无小费', 43000, 0.00, 0.00),
('2025-01-02', '0-5%', 14000, 0.75, 3.40),
('2025-01-02', '5-10%', 24000, 1.45, 7.40),
('2025-01-02', '10-15%', 19000, 2.70, 12.30),
('2025-01-02', '15-20%', 9500, 3.90, 17.30),
('2025-01-02', '20%以上', 5100, 6.30, 24.80);