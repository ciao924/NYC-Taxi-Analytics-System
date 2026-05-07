package com.taxi.analytics.modules.ai.service.dto;

public class AiQueryResponse {
    private String sql;
    private String explanation;
    private Object data;
    private ChartConfig chartConfig;
    private String sessionId;

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public ChartConfig getChartConfig() { return chartConfig; }
    public void setChartConfig(ChartConfig chartConfig) { this.chartConfig = chartConfig; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
