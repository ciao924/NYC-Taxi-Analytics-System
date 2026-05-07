package com.taxi.analytics.modules.map.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MapHeatmapService {

    /**
     * 获取上车点热力图数据
     */
    List<Map<String, Object>> getPickupHeatmap(LocalDate date, String zoneType, String dataSource, int limit);

    /**
     * 获取下车点热力图数据
     */
    List<Map<String, Object>> getDropoffHeatmap(LocalDate date, String zoneType, String dataSource, int limit);

    /**
     * 获取上下车点对比热力图
     */
    Map<String, Object> getCombinedHeatmap(LocalDate date, String dataSource, int limit);

    /**
     * 获取热力图可用日期范围
     */
    List<LocalDate> getAvailableDates();

    /**
     * 获取热点区域统计
     */
    Map<String, Object> getHotspotZones(LocalDate date, String hotspotType, int topN);
}