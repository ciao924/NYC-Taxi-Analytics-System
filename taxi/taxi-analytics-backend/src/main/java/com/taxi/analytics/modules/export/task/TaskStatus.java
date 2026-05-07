package com.taxi.analytics.modules.export.task;

/**
 * 导出任务状态枚举
 */
public enum TaskStatus {
    /** 等待中 */
    PENDING,
    
    /** 执行中 */
    RUNNING,
    
    /** 成功（已完成） */
    SUCCESS,
    
    /** 成功（已完成）- 别名 */
    COMPLETED,
    
    /** 失败 */
    FAILED,
    
    /** 已取消 */
    CANCELLED;
    
    /**
     * 判断是否为终态
     */
    public boolean isFinal() {
        return this == SUCCESS || this == COMPLETED || this == FAILED || this == CANCELLED;
    }
    
    /**
     * 判断是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS || this == COMPLETED;
    }
}