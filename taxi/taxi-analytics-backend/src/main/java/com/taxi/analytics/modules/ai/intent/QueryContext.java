package com.taxi.analytics.modules.ai.intent;

public class QueryContext {
    private final String query;
    private final String database;
    private final String sessionId;

    public QueryContext(String query, String database, String sessionId) {
        this.query = query;
        this.database = database;
        this.sessionId = sessionId;
    }

    public String getQuery() {
        return query;
    }

    public String getDatabase() {
        return database;
    }

    public String getSessionId() {
        return sessionId;
    }
}
