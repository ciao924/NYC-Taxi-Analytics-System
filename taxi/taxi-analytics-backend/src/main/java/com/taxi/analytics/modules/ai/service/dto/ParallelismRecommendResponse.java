package com.taxi.analytics.modules.ai.service.dto;

import java.util.List;

public class ParallelismRecommendResponse {
    private int recommendedParallelism;
    private double expectedImprovement;
    private List<String> operatorRecommendations;

    public int getRecommendedParallelism() { return recommendedParallelism; }
    public void setRecommendedParallelism(int recommendedParallelism) { this.recommendedParallelism = recommendedParallelism; }
    public double getExpectedImprovement() { return expectedImprovement; }
    public void setExpectedImprovement(double expectedImprovement) { this.expectedImprovement = expectedImprovement; }
    public List<String> getOperatorRecommendations() { return operatorRecommendations; }
    public void setOperatorRecommendations(List<String> operatorRecommendations) { this.operatorRecommendations = operatorRecommendations; }
}
