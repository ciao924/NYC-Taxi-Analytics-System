package com.taxi.analytics.modules.quality.controller;

import com.taxi.analytics.common.result.Result;
import com.taxi.analytics.modules.quality.service.QualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Quality", description = "数据质量检测API")
@RestController
@RequestMapping("/quality")
@RequiredArgsConstructor
public class QualityController {

    private final QualityService qualityService;

    @Operation(summary = "质量总览评分")
    @GetMapping("/summary")
    public Result<Map<String, Object>> getQualitySummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return Result.success(qualityService.getQualitySummary(date));
    }

    @Operation(summary = "所有表健康状态")
    @GetMapping("/tables")
    public Result<List<Map<String, Object>>> getTableHealthStatus(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return Result.success(qualityService.getTableHealthStatus(date));
    }

    @Operation(summary = "完整性检测详情")
    @GetMapping("/completeness")
    public Result<List<Map<String, Object>>> getCompletenessDetails(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return Result.success(qualityService.getCompletenessDetails(date));
    }

    @Operation(summary = "空值率详情")
    @GetMapping("/null-rate")
    public Result<List<Map<String, Object>>> getNullRateDetails(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return Result.success(qualityService.getNullRateDetails(date));
    }

    @Operation(summary = "唯一性检测")
    @GetMapping("/uniqueness")
    public Result<List<Map<String, Object>>> getUniquenessDetails(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return Result.success(qualityService.getUniquenessDetails(date));
    }

    @Operation(summary = "一致性检测")
    @GetMapping("/consistency")
    public Result<List<Map<String, Object>>> getConsistencyDetails(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return Result.success(qualityService.getConsistencyDetails(date));
    }

    @Operation(summary = "范围检测")
    @GetMapping("/range")
    public Result<List<Map<String, Object>>> getRangeDetails(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return Result.success(qualityService.getRangeDetails(date));
    }

    @Operation(summary = "数据及时性")
    @GetMapping("/freshness")
    public Result<List<Map<String, Object>>> getFreshnessDetails(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return Result.success(qualityService.getFreshnessDetails(date));
    }

    @Operation(summary = "告警列表")
    @GetMapping("/alerts")
    public Result<List<Map<String, Object>>> getAlerts(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(qualityService.getAlerts(startDate, endDate));
    }

    @Operation(summary = "生成质量报告")
    @GetMapping("/report")
    public Result<Map<String, Object>> generateQualityReport(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        return Result.success(qualityService.generateQualityReport(date));
    }

    @Operation(summary = "阈值配置管理")
    @GetMapping("/thresholds")
    public Result<List<Map<String, Object>>> getThresholds() {
        return Result.success(qualityService.getThresholds());
    }

    @Operation(summary = "质量历史趋势")
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> getQualityHistory(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(qualityService.getQualityHistory(startDate, endDate));
    }
}