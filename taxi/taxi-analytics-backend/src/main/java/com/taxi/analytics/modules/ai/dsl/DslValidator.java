package com.taxi.analytics.modules.ai.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class DslValidator {

    private static final Logger log = LoggerFactory.getLogger(DslValidator.class);
    
    // 指标白名单
    private final Set<String> ALLOWED_METRICS;
    // 维度白名单
    private final Set<String> ALLOWED_DIMENSIONS;
    // 图表类型白名单
    private final Set<String> ALLOWED_CHARTS;
    // 过滤器合法KEY
    private final Set<String> ALLOWED_FILTER_KEYS;

    // 时间范围正则：数字+d
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d+d$");
    // 最大天数
    private static final int MAX_DAYS = 30;

    private final ObjectMapper objectMapper;

    // ===================== 构造方法：修复所有 Set.of() =====================
    public DslValidator() {
        this.objectMapper = new ObjectMapper();

        // 1. 指标白名单（无报错版本）
        ALLOWED_METRICS = new HashSet<>();
        ALLOWED_METRICS.add("order_cnt");
        ALLOWED_METRICS.add("revenue");
        ALLOWED_METRICS.add("fare_amount");
        ALLOWED_METRICS.add("tip_amount");
        ALLOWED_METRICS.add("trip_distance");
        ALLOWED_METRICS.add("passenger_count");
        ALLOWED_METRICS.add("avg_fare");
        ALLOWED_METRICS.add("avg_tip");
        ALLOWED_METRICS.add("avg_distance");

        // 2. 维度白名单
        ALLOWED_DIMENSIONS = new HashSet<>();
        ALLOWED_DIMENSIONS.add("taxi_type");
        ALLOWED_DIMENSIONS.add("pickup_borough");
        ALLOWED_DIMENSIONS.add("dropoff_borough");
        ALLOWED_DIMENSIONS.add("payment_type");
        ALLOWED_DIMENSIONS.add("vendor_id");
        ALLOWED_DIMENSIONS.add("hour");
        ALLOWED_DIMENSIONS.add("day_of_week");
        ALLOWED_DIMENSIONS.add("date");

        // 3. 图表类型
        ALLOWED_CHARTS = new HashSet<>();
        ALLOWED_CHARTS.add("bar");
        ALLOWED_CHARTS.add("line");
        ALLOWED_CHARTS.add("pie");
        ALLOWED_CHARTS.add("scatter");
        ALLOWED_CHARTS.add("histogram");

        // 4. 过滤器KEY
        ALLOWED_FILTER_KEYS = new HashSet<>();
        ALLOWED_FILTER_KEYS.add("taxi_type");
        ALLOWED_FILTER_KEYS.add("pickup_borough");
        ALLOWED_FILTER_KEYS.add("dropoff_borough");
        ALLOWED_FILTER_KEYS.add("payment_type");
        ALLOWED_FILTER_KEYS.add("vendor_id");
        ALLOWED_FILTER_KEYS.add("date_start");
        ALLOWED_FILTER_KEYS.add("date_end");
        ALLOWED_FILTER_KEYS.add("min_fare");
        ALLOWED_FILTER_KEYS.add("max_fare");
    }

    // ===================== 校验方法 =====================
    public Dsl parseAndValidate(String dslJson) {
        try {
            Dsl dsl = objectMapper.readValue(dslJson, Dsl.class);
            validate(dsl);
            return dsl;
        } catch (Exception e) {
            log.error("DSL 解析/校验失败", e);
            throw new RuntimeException("DSL 格式非法：" + e.getMessage());
        }
    }

    public void validate(Dsl dsl) {
        // 指标校验
        if (dsl.getMetric() == null || !ALLOWED_METRICS.contains(dsl.getMetric())) {
            throw new RuntimeException("不支持的指标：" + dsl.getMetric());
        }
        // 维度校验
        if (dsl.getDimension() == null || !ALLOWED_DIMENSIONS.contains(dsl.getDimension())) {
            throw new RuntimeException("不支持的维度：" + dsl.getDimension());
        }
        // 时间范围校验
        validateTime(dsl.getTimeRange());
        // 过滤器校验
        validateFilters(dsl.getFilters());
        // 图表校验
        if (dsl.getChart() != null && !ALLOWED_CHARTS.contains(dsl.getChart())) {
            throw new RuntimeException("不支持的图表类型：" + dsl.getChart());
        }
    }

    private void validateTime(String timeRange) {
        if (timeRange == null || !TIME_PATTERN.matcher(timeRange).matches()) {
            throw new RuntimeException("时间格式错误，示例：7d");
        }
        int days = Integer.parseInt(timeRange.replace("d", ""));
        if (days > MAX_DAYS) {
            throw new RuntimeException("时间范围不能超过30天");
        }
    }

    private void validateFilters(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) return;
        for (String key : filters.keySet()) {
            if (!ALLOWED_FILTER_KEYS.contains(key)) {
                throw new RuntimeException("不支持的过滤条件：" + key);
            }
        }
    }
}