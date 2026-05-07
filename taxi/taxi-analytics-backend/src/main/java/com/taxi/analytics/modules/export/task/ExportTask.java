package com.taxi.analytics.modules.export.task;

import java.time.LocalDateTime;

public class ExportTask {
    private String taskId;
    private TaskType taskType;
    private TaskStatus status;
    private String filePath;
    private long fileSize;
    private String errorMsg;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Object params;
    
    // ========== 新增字段（供 ExportTaskManager 使用） ==========
    
    /** 总行数 */
    private Integer totalRows;
    
    /** 当前进度 (0-100) */
    private Integer progress;
    
    /** 查询参数JSON字符串 */
    private String queryParams;
    
    /** 过期时间 */
    private LocalDateTime expiresAt;
    
    // ========== Getter and Setter Methods ==========
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public TaskType getTaskType() {
        return taskType;
    }
    
    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getErrorMsg() {
        return errorMsg;
    }
    
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Object getParams() {
        return params;
    }
    
    public void setParams(Object params) {
        this.params = params;
    }
    
    public Integer getTotalRows() {
        return totalRows;
    }
    
    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }
    
    public Integer getProgress() {
        return progress;
    }
    
    public void setProgress(Integer progress) {
        this.progress = progress;
    }
    
    public String getQueryParams() {
        return queryParams;
    }
    
    public void setQueryParams(String queryParams) {
        this.queryParams = queryParams;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    // ========== 兼容方法 ==========
    
    public void setErrorMessage(String errorMessage) {
        this.errorMsg = errorMessage;
    }
}