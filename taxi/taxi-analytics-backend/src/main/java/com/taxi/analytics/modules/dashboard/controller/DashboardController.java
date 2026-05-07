package com.taxi.analytics.modules.dashboard.controller;

import com.taxi.analytics.common.result.Result;
import com.taxi.analytics.modules.dashboard.service.DashboardKpiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Dashboard", description = "仪表盘API")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardKpiService kpiService;

    @Operation(summary = "获取KPI汇总卡片数据")
    @GetMapping("/kpi/summary")
    public Result<Map<String, Object>> getKpiSummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(kpiService.getKpiSummary(startDate, endDate));
    }

    @Operation(summary = "获取核心指标趋势")
    @GetMapping("/kpi/trend")
    public Result<List<Map<String, Object>>> getKpiTrend(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(kpiService.getKpiTrend(startDate, endDate));
    }

    @Operation(summary = "获取小时分布数据")
    @GetMapping("/hourly/distribution")
    public Result<List<Map<String, Object>>> getHourlyDistribution(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(kpiService.getHourlyDistribution(startDate, endDate));
    }

    @Operation(summary = "获取星期分析数据")
    @GetMapping("/weekday/analysis")
    public Result<List<Map<String, Object>>> getWeekdayAnalysis(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(kpiService.getWeekdayAnalysis(startDate, endDate));
    }

    @Operation(summary = "获取支付方式分析")
    @GetMapping("/payment/analysis")
    public Result<List<Map<String, Object>>> getPaymentAnalysis(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(kpiService.getPaymentAnalysis(startDate, endDate));
    }

    @Operation(summary = "获取费用构成")
    @GetMapping("/fee/composition")
    public Result<List<Map<String, Object>>> getFeeComposition(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(kpiService.getFeeComposition(startDate, endDate));
    }

    @Operation(summary = "获取费用占比")
    @GetMapping("/fee/percentage")
    public Result<List<Map<String, Object>>> getFeePercentage(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(kpiService.getFeePercentage(startDate, endDate));
    }

    @Operation(summary = "获取上下客热点")
    @GetMapping("/zone/hotspots")
    public Result<List<Map<String, Object>>> getZoneHotspots(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "30") int limit) {
        return Result.success(kpiService.getZoneHotspots(startDate, endDate, limit));
    }

    @Operation(summary = "获取行政区流量")
    @GetMapping("/borough/flow")
    public Result<List<Map<String, Object>>> getBoroughFlow(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(kpiService.getBoroughFlow(startDate, endDate));
    }

    @Operation(summary = "获取可用日期范围")
    @GetMapping("/date/range")
    public Result<Map<String, LocalDate>> getAvailableDateRange() {
        return Result.success(kpiService.getAvailableDateRange());
    }

    @Operation(summary = "手动刷新缓存")
    @PostMapping("/refresh")
    public Result<Void> refreshCache() {
        kpiService.refreshCache();
        return Result.success();
    }
}