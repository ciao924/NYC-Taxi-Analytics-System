package com.taxi.analytics.modules.export.controller;

import com.taxi.analytics.common.result.Result;
import com.taxi.analytics.modules.export.service.ExportService;
import com.taxi.analytics.modules.export.task.ExportRequest;
import com.taxi.analytics.modules.export.task.ExportTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@Tag(name = "Export", description = "导出服务API")
@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @Operation(summary = "创建导出任务")
    @PostMapping("/task")
    public Result<String> createExportTask(@RequestBody ExportRequest request) {
        String taskId = exportService.createExportTask(request);
        return Result.success(taskId);
    }

    @Operation(summary = "获取任务状态")
    @GetMapping("/task/{id}")
    public Result<ExportTask> getTaskStatus(@PathVariable("id") String taskId) {
        return Result.success(exportService.getTaskStatus(taskId));
    }

    @Operation(summary = "取消导出任务")
    @DeleteMapping("/task/{id}")
    public Result<Boolean> cancelExportTask(@PathVariable("id") String taskId) {
        return Result.success(exportService.cancelExportTask(taskId));
    }

    @Operation(summary = "下载导出文件")
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") String taskId) {
        ExportTask task = exportService.getTaskStatus(taskId);
        if (task == null || task.getStatus() != com.taxi.analytics.modules.export.task.TaskStatus.COMPLETED) {
            throw new IllegalArgumentException("导出任务不存在或未完成");
        }

        File file = new File(task.getFilePath());
        if (!file.exists()) {
            throw new IllegalArgumentException("导出文件不存在");
        }

        Resource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}