package com.taxi.analytics.modules.export.service;

import com.taxi.analytics.modules.export.task.ExportRequest;
import com.taxi.analytics.modules.export.task.ExportTask;
import com.taxi.analytics.modules.export.task.TaskStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public interface ExportService {
    
    /**
     * 创建导出任务
     */
    String createExportTask(ExportRequest request);
    
    /**
     * 获取任务状态
     */
    ExportTask getTaskStatus(String taskId);
    
    /**
     * 执行导出任务
     */
    @Async
    CompletableFuture<ExportTask> executeExportTask(String taskId);
    
    /**
     * 取消导出任务
     */
    boolean cancelExportTask(String taskId);
}
