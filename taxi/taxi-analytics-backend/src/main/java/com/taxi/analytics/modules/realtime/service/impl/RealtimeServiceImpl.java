package com.taxi.analytics.modules.realtime.service.impl;

import com.taxi.analytics.modules.realtime.service.RealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RealtimeServiceImpl implements RealtimeService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> getLatestKpi() {
        String sql = "SELECT trip_count, total_fare, avg_fare, avg_distance FROM realtime_kpi_5min ORDER BY window_end DESC LIMIT 1";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
        return result.isEmpty() ? Map.of() : result.get(0);
    }

    @Override
    public List<Map<String, Object>> getHotspot(String type, int limit) {
        String table = "pickup".equals(type) ? "realtime_pickup_hotspot" : "realtime_dropoff_hotspot";
        String sql = String.format("SELECT location_id, name, trip_count, rank FROM %s ORDER BY trip_count DESC LIMIT ?", table);
        return jdbcTemplate.queryForList(sql, limit);
    }

    @Override
    public List<Map<String, Object>> getFeeComposition() {
        String sql = "SELECT payment_type, trip_count, total_fare, avg_fare, tip_rate FROM realtime_fee_composition";
        return jdbcTemplate.queryForList(sql);
    }

    @Override
    public List<Map<String, Object>> getTrend() {
        String sql = "SELECT DATE_FORMAT(window_end, '%Y-%m-%d %H:00:00') as hour, SUM(trip_count) as trip_count, SUM(total_fare) as total_fare " +
                "FROM realtime_kpi_5min WHERE window_end >= DATE_SUB(NOW(), INTERVAL 24 HOUR) " +
                "GROUP BY hour ORDER BY hour";
        return jdbcTemplate.queryForList(sql);
    }
}
