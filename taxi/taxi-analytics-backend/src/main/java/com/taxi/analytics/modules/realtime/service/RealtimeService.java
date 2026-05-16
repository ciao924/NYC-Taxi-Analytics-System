package com.taxi.analytics.modules.realtime.service;

import java.util.List;
import java.util.Map;

public interface RealtimeService {
    Map<String, Object> getLatestKpi();
    List<Map<String, Object>> getHotspot(int limit);
    List<Map<String, Object>> getFeeComposition();
    List<Map<String, Object>> getTrend();
}
