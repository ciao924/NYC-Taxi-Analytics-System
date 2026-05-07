package com.taxi.analytics.modules.ai.service.dto;

import java.util.List;

public class SkewDiagnoseResponse {
    private boolean hasSkew;
    private List<String> skewedStages;
    private double skewFactor;
    private List<String> recommendations;

    public boolean isHasSkew() { return hasSkew; }
    public void setHasSkew(boolean hasSkew) { this.hasSkew = hasSkew; }
    public List<String> getSkewedStages() { return skewedStages; }
    public void setSkewedStages(List<String> skewedStages) { this.skewedStages = skewedStages; }
    public double getSkewFactor() { return skewFactor; }
    public void setSkewFactor(double skewFactor) { this.skewFactor = skewFactor; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}
