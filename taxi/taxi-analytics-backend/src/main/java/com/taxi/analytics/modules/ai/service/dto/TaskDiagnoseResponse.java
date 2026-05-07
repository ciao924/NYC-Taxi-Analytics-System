package com.taxi.analytics.modules.ai.service.dto;

import java.util.List;

public class TaskDiagnoseResponse {
    private String rootCause;
    private String category;
    private List<String> suggestions;
    private double confidence;

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
