package com.taxi.analytics.modules.ai.service.dto;

public class ParallelismRecommendRequest {
    private String jobId;
    private double currentThroughput;
    private int currentParallelism;

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public double getCurrentThroughput() { return currentThroughput; }
    public void setCurrentThroughput(double currentThroughput) { this.currentThroughput = currentThroughput; }
    public int getCurrentParallelism() { return currentParallelism; }
    public void setCurrentParallelism(int currentParallelism) { this.currentParallelism = currentParallelism; }
}
