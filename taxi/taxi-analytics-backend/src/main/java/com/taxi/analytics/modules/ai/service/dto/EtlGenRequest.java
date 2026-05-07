package com.taxi.analytics.modules.ai.service.dto;

public class EtlGenRequest {
    private String description;
    private String sourceTable;
    private String targetTable;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    public String getTargetTable() { return targetTable; }
    public void setTargetTable(String targetTable) { this.targetTable = targetTable; }
}
