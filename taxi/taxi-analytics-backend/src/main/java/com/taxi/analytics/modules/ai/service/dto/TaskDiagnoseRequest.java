package com.taxi.analytics.modules.ai.service.dto;

public class TaskDiagnoseRequest {
    private String taskId;
    private String taskLog;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTaskLog() { return taskLog; }
    public void setTaskLog(String taskLog) { this.taskLog = taskLog; }
}
