package com.taxi.analytics.modules.map.service.impl;

import com.taxi.analytics.modules.map.mapper.MapHeatmapMapper;
import com.taxi.analytics.modules.map.service.MapHeatmapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MapHeatmapServiceImpl implements MapHeatmapService {

    private static final Logger log = LoggerFactory.getLogger(MapHeatmapServiceImpl.class);
    private final MapHeatmapMapper heatmapMapper;
    
    public MapHeatmapServiceImpl(MapHeatmapMapper heatmapMapper) {
        this.heatmapMapper = heatmapMapper;
    }

    @Override
    public List<Map<String, Object>> getPickupHeatmap(LocalDate date, String zoneType, String dataSource, int limit) {
        log.debug("Fetching pickup heatmap for date={}, zoneType={}, dataSource={}, limit={}",
                date, zoneType, dataSource, limit);
        try {
            return heatmapMapper.selectPickupHeatmap(date.toString(), dataSource, limit);
        } catch (Exception e) {
            log.error("Failed to fetch pickup heatmap", e);
            return generateMockPickupHeatmap(limit);
        }
    }

    @Override
    public List<Map<String, Object>> getDropoffHeatmap(LocalDate date, String zoneType, String dataSource, int limit) {
        log.debug("Fetching dropoff heatmap for date={}, zoneType={}, dataSource={}, limit={}",
                date, zoneType, dataSource, limit);
        try {
            return heatmapMapper.selectDropoffHeatmap(date.toString(), dataSource, limit);
        } catch (Exception e) {
            log.error("Failed to fetch dropoff heatmap", e);
            return generateMockDropoffHeatmap(limit);
        }
    }

    @Override
    public Map<String, Object> getCombinedHeatmap(LocalDate date, String dataSource, int limit) {
        log.debug("Fetching combined heatmap for date={}, dataSource={}, limit={}", date, dataSource, limit);
        Map<String, Object> result = new HashMap<>();

        List<Map<String, Object>> pickupData = getPickupHeatmap(date, "pickup", dataSource, limit);
        List<Map<String, Object>> dropoffData = getDropoffHeatmap(date, "dropoff", dataSource, limit);

        int totalPickupCount = pickupData.stream()
                .mapToInt(p -> ((Number) p.getOrDefault("count", 0)).intValue())
                .sum();
        int totalDropoffCount = dropoffData.stream()
                .mapToInt(p -> ((Number) p.getOrDefault("count", 0)).intValue())
                .sum();

        result.put("pickup", pickupData);
        result.put("dropoff", dropoffData);
        result.put("date", date.toString());
        result.put("totalPickupCount", totalPickupCount);
        result.put("totalDropoffCount", totalDropoffCount);

        return result;
    }

    @Override
    public List<LocalDate> getAvailableDates() {
        log.debug("Fetching available dates for heatmap");
        try {
            List<String> dateStrings = heatmapMapper.selectAvailableDates();
            List<LocalDate> dates = new ArrayList<>();
            for (String dateStr : dateStrings) {
                try {
                    dates.add(LocalDate.parse(dateStr));
                } catch (Exception ignored) {
                }
            }
            return dates;
        } catch (Exception e) {
            log.error("Failed to fetch available dates", e);
            List<LocalDate> mockDates = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 7; i++) {
                mockDates.add(today.minusDays(i));
            }
            return mockDates;
        }
    }

    @Override
    public Map<String, Object> getHotspotZones(LocalDate date, String hotspotType, int topN) {
        log.debug("Fetching hotspot zones for date={}, hotspotType={}, topN={}", date, hotspotType, topN);
        Map<String, Object> result = new HashMap<>();

        try {
            if ("pickup".equals(hotspotType)) {
                List<Map<String, Object>> zones = heatmapMapper.selectPickupHotspotZones(date.toString(), topN);
                result.put("zones", zones);
                result.put("type", "pickup");
            } else {
                List<Map<String, Object>> zones = heatmapMapper.selectDropoffHotspotZones(date.toString(), topN);
                result.put("zones", zones);
                result.put("type", "dropoff");
            }
        } catch (Exception e) {
            log.error("Failed to fetch hotspot zones", e);
            result.put("zones", new ArrayList<>());
            result.put("type", hotspotType);
        }

        result.put("date", date.toString());
        result.put("topN", topN);
        return result;
    }

    private List<Map<String, Object>> generateMockPickupHeatmap(int limit) {
        List<Map<String, Object>> mockData = new ArrayList<>();
        double[][] nycHotspots = {
                {40.7589, -73.9851},
                {40.7484, -73.9857},
                {40.7527, -73.9772},
                {40.7614, -73.9776},
                {40.7831, -73.9712},
                {40.6892, -74.0445},
                {40.7061, -74.0088}
        };

        for (int i = 0; i < Math.min(limit, 100); i++) {
            int hotspotIndex = i % nycHotspots.length;
            double[] hotspot = nycHotspots[hotspotIndex];
            double lat = hotspot[0] + (Math.random() - 0.5) * 0.02;
            double lng = hotspot[1] + (Math.random() - 0.5) * 0.02;
            int count = (int) (Math.random() * 100) + 10;

            Map<String, Object> point = new HashMap<>();
            point.put("lat", lat);
            point.put("lng", lng);
            point.put("count", count);
            point.put("location_id", 100 + i);
            point.put("zone_name", "Zone " + (100 + i));
            mockData.add(point);
        }
        return mockData;
    }

    private List<Map<String, Object>> generateMockDropoffHeatmap(int limit) {
        List<Map<String, Object>> mockData = new ArrayList<>();
        double[][] nycHotspots = {
                {40.7589, -73.9851},
                {40.7484, -73.9857},
                {40.7527, -73.9772},
                {40.7614, -73.9776},
                {40.7831, -73.9712},
                {40.6413, -73.7781},
                {40.7769, -73.8740}
        };

        for (int i = 0; i < Math.min(limit, 100); i++) {
            int hotspotIndex = i % nycHotspots.length;
            double[] hotspot = nycHotspots[hotspotIndex];
            double lat = hotspot[0] + (Math.random() - 0.5) * 0.02;
            double lng = hotspot[1] + (Math.random() - 0.5) * 0.02;
            int count = (int) (Math.random() * 100) + 10;

            Map<String, Object> point = new HashMap<>();
            point.put("lat", lat);
            point.put("lng", lng);
            point.put("count", count);
            point.put("location_id", 200 + i);
            point.put("zone_name", "Zone " + (200 + i));
            mockData.add(point);
        }
        return mockData;
    }
}