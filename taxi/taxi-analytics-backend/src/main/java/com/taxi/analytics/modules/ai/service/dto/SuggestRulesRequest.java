package com.taxi.analytics.modules.ai.service.dto;

import java.util.List;

public class SuggestRulesRequest {
    private String tableName;
    private List<String> columnSamples;

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public List<String> getColumnSamples() { return columnSamples; }
    public void setColumnSamples(List<String> columnSamples) { this.columnSamples = columnSamples; }
}
