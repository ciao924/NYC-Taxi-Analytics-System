package com.taxi.analytics.modules.ai.metrics;

public class MetricDef {
    private String tableName;
    private String fieldName;
    private String aggregation;

    public MetricDef(String tableName, String fieldName, String aggregation) {
        this.tableName = tableName;
        this.fieldName = fieldName;
        this.aggregation = aggregation;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getAggregation() {
        return aggregation;
    }

    public void setAggregation(String aggregation) {
        this.aggregation = aggregation;
    }
}