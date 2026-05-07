package com.taxi.analytics.modules.ai.service.dto;

import java.util.List;

public class EtlGenResponse {
    private String sparkSql;
    private String explanation;
    private List<FieldMapping> fieldMappings;

    public String getSparkSql() { return sparkSql; }
    public void setSparkSql(String sparkSql) { this.sparkSql = sparkSql; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public List<FieldMapping> getFieldMappings() { return fieldMappings; }
    public void setFieldMappings(List<FieldMapping> fieldMappings) { this.fieldMappings = fieldMappings; }
}
