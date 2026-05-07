package com.taxi.analytics.modules.ai.service.dto;

import java.util.List;

public class SuggestRulesResponse {
    private List<QualityRule> suggestedRules;
    private double confidence;

    public List<QualityRule> getSuggestedRules() { return suggestedRules; }
    public void setSuggestedRules(List<QualityRule> suggestedRules) { this.suggestedRules = suggestedRules; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
