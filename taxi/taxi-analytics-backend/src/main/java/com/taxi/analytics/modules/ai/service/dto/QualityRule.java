package com.taxi.analytics.modules.ai.service.dto;

public class QualityRule {
    private String ruleType;
    private String field;
    private double threshold;

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
}
