package com.taxi.analytics.modules.analysis.controller;

import com.taxi.analytics.common.result.Result;
import com.taxi.analytics.modules.analysis.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "Analysis", description = "数据分析API")
@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "获取机场统计", description = "三大机场（肯尼迪、拉瓜迪亚、纽瓦克）统计")
    @GetMapping("/airport")
    public Result<List<Map<String, Object>>> getAirportStatistics(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.getAirportStatistics(startDate, endDate));
    }

    @Operation(summary = "获取机场详细统计（按机场和小时）")
    @GetMapping("/airport/detailed")
    public Result<List<Map<String, Object>>> getAirportDetailedStatistics(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "机场代码：JFK/LGA/EWR/all")
            @RequestParam(defaultValue = "all") String airportCode) {
        return Result.success(analysisService.getAirportDetailedStatistics(startDate, endDate, airportCode));
    }

    @Operation(summary = "获取供应商对比", description = "不同供应商（Creative Mobile/VeriFone等）数据对比")
    @GetMapping("/vendor")
    public Result<List<Map<String, Object>>> getVendorComparison(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.getVendorComparison(startDate, endDate));
    }

    @Operation(summary = "获取供应商趋势")
    @GetMapping("/vendor/trend")
    public Result<List<Map<String, Object>>> getVendorTrend(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "供应商ID")
            @RequestParam(defaultValue = "all") String vendorId) {
        return Result.success(analysisService.getVendorTrend(startDate, endDate, vendorId));
    }

    @Operation(summary = "获取支付方式分布")
    @GetMapping("/payment")
    public Result<List<Map<String, Object>>> getPaymentDistribution(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.getPaymentDistribution(startDate, endDate));
    }

    @Operation(summary = "获取支付方式趋势")
    @GetMapping("/payment/trend")
    public Result<List<Map<String, Object>>> getPaymentTrend(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.getPaymentTrend(startDate, endDate));
    }

    @Operation(summary = "获取行程距离分布")
    @GetMapping("/distance/distribution")
    public Result<List<Map<String, Object>>> getDistanceDistribution(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.getDistanceDistribution(startDate, endDate));
    }

    @Operation(summary = "获取行程时长分布")
    @GetMapping("/duration/distribution")
    public Result<List<Map<String, Object>>> getDurationDistribution(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.getDurationDistribution(startDate, endDate));
    }
}