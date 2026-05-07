package com.taxi.analytics.modules.ai.service.dto;

import java.util.List;

public class FlinkBackpressureResponse {
    private boolean hasBackpressure;
    private String bottleneckOperator;
    private double backpressureRatio;
    private List<String> recommendations;

    public boolean isHasBackpressure() { return hasBackpressure; }
    public void setHasBackpressure(boolean hasBackpressure) { this.hasBackpressure = hasBackpressure; }
    public String getBottleneckOperator() { return bottleneckOperator; }
    public void setBottleneckOperator(String bottleneckOperator) { this.bottleneckOperator = bottleneckOperator; }
    public double getBackpressureRatio() { return backpressureRatio; }
    public void setBackpressureRatio(double backpressureRatio) { this.backpressureRatio = backpressureRatio; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}
