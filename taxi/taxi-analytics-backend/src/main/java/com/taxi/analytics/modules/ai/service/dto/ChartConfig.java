package com.taxi.analytics.modules.ai.service.dto;

import java.util.List;
import java.util.Map;

public class ChartConfig {
    private String chartType;
    private String xAxisField;
    private String yAxisField;
    private String title;
    private List<Map<String, Object>> data;

    public String getChartType() { return chartType; }
    public void setChartType(String chartType) { this.chartType = chartType; }
    public String getXAxisField() { return xAxisField; }
    public void setXAxisField(String xAxisField) { this.xAxisField = xAxisField; }
    public String getYAxisField() { return yAxisField; }
    public void setYAxisField(String yAxisField) { this.yAxisField = yAxisField; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public List<Map<String, Object>> getData() { return data; }
    public void setData(List<Map<String, Object>> data) { this.data = data; }
}
