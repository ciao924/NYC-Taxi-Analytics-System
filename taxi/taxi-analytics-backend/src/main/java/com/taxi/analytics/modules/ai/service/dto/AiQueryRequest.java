package com.taxi.analytics.modules.ai.service.dto;

public class AiQueryRequest {
    private String query;
    private String sessionId;
    private String database;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
}
