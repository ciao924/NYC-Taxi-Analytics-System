package com.taxi.analytics.modules.ai.service.dto;

import java.util.List;

public class FlinkBackpressureRequest {
    private String jobId;
    private List<String> operatorMetrics;

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    public List<String> getOperatorMetrics() { return operatorMetrics; }
    public void setOperatorMetrics(List<String> operatorMetrics) { this.operatorMetrics = operatorMetrics; }
}
