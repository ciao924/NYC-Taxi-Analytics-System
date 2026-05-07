package com.taxi.analytics.modules.ai.service.dto;

public class FieldMapping {
    private String sourceField;
    private String targetField;
    private String transformation;

    public String getSourceField() { return sourceField; }
    public void setSourceField(String sourceField) { this.sourceField = sourceField; }
    public String getTargetField() { return targetField; }
    public void setTargetField(String targetField) { this.targetField = targetField; }
    public String getTransformation() { return transformation; }
    public void setTransformation(String transformation) { this.transformation = transformation; }
}
