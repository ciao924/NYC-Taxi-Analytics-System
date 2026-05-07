package com.taxi.analytics.modules.ai.service.dto;

public class SkewDiagnoseRequest {
    private String jobId;
    private String executionPlan;

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public String getExecutionPlan() { return executionPlan; }
    public void setExecutionPlan(String executionPlan) { this.executionPlan = executionPlan; }
}
