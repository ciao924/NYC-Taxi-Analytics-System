package com.taxi.analytics.modules.export.service.impl;

import com.taxi.analytics.modules.export.service.ExportService;
import com.taxi.analytics.modules.export.task.ExportRequest;
import com.taxi.analytics.modules.export.task.ExportTask;
import com.taxi.analytics.modules.export.task.TaskStatus;
import com.taxi.analytics.modules.export.task.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ExportServiceImpl implements ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);

    // 任务存储
    private final Map<String, ExportTask> taskMap = new ConcurrentHashMap<>();
    // 任务计数器
    private final AtomicInteger taskCounter = new AtomicInteger(0);
    // 最大并发任务数
    private static final int MAX_CONCURRENT_TASKS = 5;
    // 线程池
    private final ExecutorService executorService = new ThreadPoolExecutor(
            5, 10, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Override
    public String createExportTask(ExportRequest request) {
        String taskId = "export_" + System.currentTimeMillis() + "_" + taskCounter.incrementAndGet();
        ExportTask task = new ExportTask();
        task.setTaskId(taskId);
        task.setTaskType(request.getTaskType());
        task.setParams(request.getParams());
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        
        taskMap.put(taskId, task);
        
        // 异步执行任务
        executorService.submit(() -> {
            try {
                executeExportTask(taskId);
            } catch (Exception e) {
                log.error("导出任务执行失败", e);
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMsg(e.getMessage());
                task.setUpdatedAt(LocalDateTime.now());
            }
        });
        
        return taskId;
    }

    @Override
    public ExportTask getTaskStatus(String taskId) {
        return taskMap.get(taskId);
    }

    @Override
    public CompletableFuture<ExportTask> executeExportTask(String taskId) {
        return CompletableFuture.supplyAsync(() -> {
            ExportTask task = taskMap.get(taskId);
            if (task == null) {
                throw new RuntimeException("任务不存在");
            }
            
            try {
                task.setStatus(TaskStatus.RUNNING);
                task.setUpdatedAt(LocalDateTime.now());
                
                // 模拟导出过程
                Thread.sleep(5000);
                
                // 生成导出文件
                String filePath = "/tmp/exports/" + taskId + ".xlsx";
                File file = new File(filePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
                
                task.setStatus(TaskStatus.COMPLETED);
                task.setFilePath(filePath);
                task.setFileSize(file.length());
                task.setUpdatedAt(LocalDateTime.now());
                
                log.info("导出任务完成: {}", taskId);
            } catch (Exception e) {
                log.error("导出任务执行失败", e);
                task.setStatus(TaskStatus.FAILED);
                task.setErrorMsg(e.getMessage());
                task.setUpdatedAt(LocalDateTime.now());
            }
            
            return task;
        }, executorService);
    }

    @Override
    public boolean cancelExportTask(String taskId) {
        ExportTask task = taskMap.get(taskId);
        if (task != null && task.getStatus() == TaskStatus.RUNNING) {
            task.setStatus(TaskStatus.CANCELLED);
            task.setUpdatedAt(LocalDateTime.now());
            return true;
        }
        return false;
    }
}