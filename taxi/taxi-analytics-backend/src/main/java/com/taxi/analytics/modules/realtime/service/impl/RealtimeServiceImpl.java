package com.taxi.analytics.modules.realtime.service.impl;

import com.taxi.analytics.modules.realtime.config.ZoneMapping;
import com.taxi.analytics.modules.realtime.mapper.RealtimeMapper;
import com.taxi.analytics.modules.realtime.service.RealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeServiceImpl implements RealtimeService {

    private final RealtimeMapper realtimeMapper;

    @Override
    public Map<String, Object> getLatestKpi() {
        try {
            Map<String, Object> latestKpi = realtimeMapper.getLatestKpi();

            Map<String, Object> result = new HashMap<>();
            result.put("orderCount", getLongValue(latestKpi, "trip_count"));
            result.put("totalFare", getDoubleValue(latestKpi, "total_fare"));
            result.put("avgFare", getDoubleValue(latestKpi, "avg_fare"));
            result.put("windowEnd", latestKpi.get("window_end"));

            long currentTrips = getLongValue(latestKpi, "trip_count");
            double currentFare = getDoubleValue(latestKpi, "total_fare");
            double currentAvgFare = getDoubleValue(latestKpi, "avg_fare");

            result.put("orderGrowth", 0.0);
            result.put("fareGrowth", 0.0);
            result.put("avgFareGrowth", 0.0);

            log.debug("Latest KPI: orderCount={}, totalFare={}, avgFare={}",
                    result.get("orderCount"), result.get("totalFare"), result.get("avgFare"));

            return result;
        } catch (Exception e) {
            log.error("Failed to get latest KPI", e);
            return getEmptyKpi();
        }
    }

    @Override
    public List<Map<String, Object>> getHotspot(int limit) {
        try {
            List<Map<String, Object>> hotspots = realtimeMapper.getHotspotTopn(limit);
            log.debug("Fetched {} hotspots", hotspots.size());
            
            for (Map<String, Object> hotspot : hotspots) {
                String zoneId = String.valueOf(hotspot.get("zone"));
                String zoneName = ZoneMapping.getZoneName(zoneId);
                hotspot.put("zoneName", zoneName);
            }
            
            return hotspots;
        } catch (Exception e) {
            log.error("Failed to get hotspots", e);
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> getFeeComposition() {
        try {
            List<Map<String, Object>> composition = realtimeMapper.getFeeComposition();
            log.debug("Fetched {} fee composition records", composition.size());
            return composition;
        } catch (Exception e) {
            log.error("Failed to get fee composition", e);
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> getTrend() {
        try {
            List<Map<String, Object>> trend = realtimeMapper.getTrend();
            log.debug("Fetched {} trend data points", trend.size());
            return trend;
        } catch (Exception e) {
            log.error("Failed to get trend", e);
            return List.of();
        }
    }

    private Map<String, Object> getEmptyKpi() {
        Map<String, Object> empty = new HashMap<>();
        empty.put("orderCount", 0L);
        empty.put("totalFare", 0.0);
        empty.put("avgFare", 0.0);
        empty.put("orderGrowth", 0.0);
        empty.put("fareGrowth", 0.0);
        empty.put("avgFareGrowth", 0.0);
        return empty;
    }

    private long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0L;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}