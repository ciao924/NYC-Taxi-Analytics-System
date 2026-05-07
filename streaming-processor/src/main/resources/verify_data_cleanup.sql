-- 验证事件调度器状态
SHOW VARIABLES LIKE 'event_scheduler';

-- 查看已创建的事件
SHOW EVENTS FROM nyc_taxi_realtime;

-- 验证事件状态
SELECT event_name, status FROM information_schema.events WHERE event_schema = 'nyc_taxi_realtime';

-- 手动测试清理事件（可选）
-- DELETE FROM nyc_taxi_realtime.realtime_trip_green WHERE process_time < DATE_SUB(NOW(), INTERVAL 24 HOUR);
-- SELECT COUNT(*) FROM nyc_taxi_realtime.realtime_trip_green;

-- 查看事件执行历史（需要开启事件日志）
-- SHOW GLOBAL VARIABLES LIKE 'log_events';
-- SET GLOBAL log_events = ON;
