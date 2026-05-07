package com.taxi.analytics.modules.export.task;

import java.util.Map;

public class ExportRequest {
    private TaskType taskType;
    private Map<String, Object> params;
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
    
    public Map<String, Object> getParams() {
        return params;
    }
    
    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
