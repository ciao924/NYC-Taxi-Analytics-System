package com.taxi.analytics.modules.ai.dsl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Dsl {
    private String metric;
    @JsonProperty("time_range")
    private String timeRange;
    private String dimension;
    private Map<String, Object> filters;
    private String chart;

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public String getChart() {
        return chart;
    }

    public void setChart(String chart) {
        this.chart = chart;
    }
}