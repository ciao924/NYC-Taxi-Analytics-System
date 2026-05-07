package com.taxi.analytics.modules.ai.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MetricRegistry {

    private static final Logger log = LoggerFactory.getLogger(MetricRegistry.class);
    
    private final Map<String, MetricDef> metrics;

    public MetricRegistry() {
        this.metrics = new HashMap<>();
        initMetrics();
    }

    private void initMetrics() {
        metrics.put("order_cnt", new MetricDef("dws_taxi_day", "order_cnt", "SUM"));
        metrics.put("revenue", new MetricDef("dws_taxi_day", "revenue", "SUM"));
        metrics.put("fare_amount", new MetricDef("dws_taxi_day", "fare_amount", "SUM"));
        metrics.put("tip_amount", new MetricDef("dws_taxi_day", "tip_amount", "SUM"));
        metrics.put("trip_distance", new MetricDef("dws_taxi_day", "trip_distance", "SUM"));
        metrics.put("passenger_count", new MetricDef("dws_taxi_day", "passenger_count", "SUM"));
        metrics.put("avg_fare", new MetricDef("dws_taxi_day", "fare_amount", "AVG"));
        metrics.put("avg_tip", new MetricDef("dws_taxi_day", "tip_amount", "AVG"));
        metrics.put("avg_distance", new MetricDef("dws_taxi_day", "trip_distance", "AVG"));

        log.info("指标注册器初始化完成，注册指标数：{}", metrics.size());
    }

    public MetricDef getMetricDef(String metricName) {
        MetricDef metricDef = metrics.get(metricName);
        if (metricDef == null) {
            throw new RuntimeException("指标不存在：" + metricName);
        }
        return metricDef;
    }

    public boolean containsMetric(String metricName) {
        return metrics.containsKey(metricName);
    }
}