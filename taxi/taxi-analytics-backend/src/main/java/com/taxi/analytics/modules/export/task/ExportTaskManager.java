package com.taxi.analytics.modules.export.task;

import com.taxi.analytics.common.exception.BusinessException;
import com.taxi.analytics.common.result.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ExportTaskManager {

    private static final Logger log = LoggerFactory.getLogger(ExportTaskManager.class);
    
    @Value("${taxi.analytics.export.max-concurrent-tasks:5}")
    private int maxConcurrentTasks;
    
    @Value("${taxi.analytics.export.max-rows:100000}")
    private int maxRows;
    
    @Value("${taxi.analytics.export.chunk-size:10000}")
    private int chunkSize;
    
    @Value("${taxi.analytics.export.temp-dir:/tmp/exports}")
    private String tempDir;
    
    private final ConcurrentHashMap<String, ExportTask> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger runningCount = new AtomicInteger(0);
    
    /**
     * 创建导出任务
     */
    public ExportTask createTask(ExportRequest request) {
        // 检查并发限制
        if (runningCount.get() >= maxConcurrentTasks) {
            throw new BusinessException(ResultCode.EXPORT_CONCURRENT_LIMIT);
        }
        
        // 预检查导出数量
        long totalCount = countExportData(request);
        if (totalCount > maxRows) {
            throw new BusinessException(ResultCode.EXPORT_TOO_MANY_ROWS);
        }
        
        // ✅ 修复1：不使用 builder，直接使用构造函数
        ExportTask task = new ExportTask();
        task.setTaskId(generateTaskId());
        task.setTaskType(request.getTaskType());
        task.setStatus(TaskStatus.PENDING);
        task.setTotalRows((int) totalCount);
        task.setProgress(0);
        task.setQueryParams(request.getParams() != null ? request.getParams().toString() : "{}");
        task.setCreatedAt(LocalDateTime.now());
        task.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        tasks.put(task.getTaskId(), task);
        
        return task;
    }
    
    /**
     * 异步执行导出
     */
    @Async("exportExecutor")
    public void submitExportTask(ExportTask task, ExportRequest request) {
        runningCount.incrementAndGet();
        task.setStatus(TaskStatus.RUNNING);
        
        try {
            String filePath = doExport(task, request);
            task.setStatus(TaskStatus.SUCCESS);
            task.setFilePath(filePath);
            task.setProgress(100);
        } catch (Exception e) {
            log.error("导出任务失败: {}", task.getTaskId(), e);
            task.setStatus(TaskStatus.FAILED);
            // ✅ 修复2：使用 setErrorMsg 而不是 setErrorMessage
            task.setErrorMsg(e.getMessage());
        } finally {
            runningCount.decrementAndGet();
        }
    }
    
    /**
     * 执行导出(分批查询+流式写入)
     */
    private String doExport(ExportTask task, ExportRequest request) throws Exception {
        // 确保临时目录存在
        Files.createDirectories(Paths.get(tempDir));
        
        String fileName = generateFileName(task);
        String tempFilePath = tempDir + "/" + fileName;
        
        // 模拟导出过程
        int offset = 0;
        int currentProgress = 0;
        Integer totalRows = task.getTotalRows();
        
        if (totalRows != null && totalRows > 0) {
            while (offset < totalRows) {
                List<?> dataList = queryExportData(request, offset, chunkSize);
                if (dataList.isEmpty()) break;
                
                offset += dataList.size();
                currentProgress = (int) ((double) offset / totalRows * 100);
                task.setProgress(currentProgress);
                
                log.debug("导出进度: {}% ({} / {})", currentProgress, offset, totalRows);
                
                // 模拟数据处理耗时
                Thread.sleep(100);
            }
        }
        
        task.setProgress(100);
        
        // 创建临时文件（模拟）
        Files.createFile(Paths.get(tempFilePath));
        
        // 上传到文件存储预留
        String fileUrl = uploadToStorage(tempFilePath, fileName);
        
        // 清理临时文件
        Files.deleteIfExists(Paths.get(tempFilePath));
        
        return fileUrl;
    }
    
    /**
     * 定时清理过期任务
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredTasks() {
        LocalDateTime now = LocalDateTime.now();
        tasks.values().removeIf(task -> {
            if (task.getExpiresAt() != null && task.getExpiresAt().isBefore(now)) {
                deleteFile(task.getFilePath());
                return true;
            }
            return false;
        });
        log.info("清理过期导出任务完成，当前任务数: {}", tasks.size());
    }
    
    /**
     * 获取任务
     */
    public ExportTask getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * 获取所有任务
     */
    public List<ExportTask> getAllTasks() {
        return tasks.values().stream().toList();
    }

    // ---------------- 私有方法 ----------------

    private String generateTaskId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private long countExportData(ExportRequest request) {
        // TODO: 实现真实数据量统计
        return 5000L;
    }

    private String generateFileName(ExportTask task) {
        return task.getTaskType() + "_" + System.currentTimeMillis() + ".xlsx";
    }

    @SuppressWarnings("unused")
    private List<?> queryExportData(ExportRequest request, int offset, int limit) {
        // TODO: 实现真实分页查询
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private List<String> getHeader(String taskType) {
        return Collections.singletonList("Column");
    }

    private String uploadToStorage(String tempPath, String fileName) {
        // TODO: 实现真实文件上传
        return "/storage/" + fileName;
    }

    private void deleteFile(String filePath) {
        if (filePath != null) {
            try {
                Files.deleteIfExists(Paths.get(filePath));
                log.info("删除过期文件: {}", filePath);
            } catch (Exception e) {
                log.warn("删除文件失败: {}", filePath, e);
            }
        }
    }
}