package com.taxi.analytics.modules.ai.client;

import java.util.Map;

public class ChatMessage {
    private String messageId;
    private String role;
    private String content;
    private String explanation;
    private String sql;
    private Map<String, Object> chartConfig;
    private Integer executionTime;
    private long timestamp;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public ChatMessage(String role, String content, long timestamp) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }
    public Map<String, Object> getChartConfig() { return chartConfig; }
    public void setChartConfig(Map<String, Object> chartConfig) { this.chartConfig = chartConfig; }
    public Integer getExecutionTime() { return executionTime; }
    public void setExecutionTime(Integer executionTime) { this.executionTime = executionTime; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
