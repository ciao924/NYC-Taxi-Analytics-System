package com.taxi.analytics.modules.ai.service.dto;

import java.util.List;

public class FieldMappingResponse {
    private List<FieldMapping> mappings;
    private double confidence;

    public List<FieldMapping> getMappings() { return mappings; }
    public void setMappings(List<FieldMapping> mappings) { this.mappings = mappings; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
