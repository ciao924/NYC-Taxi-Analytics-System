package com.taxi.analytics.modules.analysis.controller;

import com.taxi.analytics.common.result.Result;
import com.taxi.analytics.modules.analysis.dto.*;
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

    @Operation(summary = "多维交叉分析", description = "支持供应商×支付方式×时间等维度组合分析")
    @GetMapping("/multi-dimension")
    public Result<List<MultiDimensionAnalysisDTO>> getMultiDimensionAnalysis(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "维度1：vendor/payment/airport")
            @RequestParam(defaultValue = "vendor") String dimension1,
            @Parameter(description = "维度2：vendor/payment/airport")
            @RequestParam(defaultValue = "payment") String dimension2) {
        return Result.success(analysisService.getMultiDimensionAnalysis(startDate, endDate, dimension1, dimension2));
    }

    @Operation(summary = "异常检测", description = "识别异常波动，自动分析原因")
    @GetMapping("/anomaly-detection")
    public Result<List<AnomalyDetectionDTO>> detectAnomalies(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.detectAnomalies(startDate, endDate));
    }

    @Operation(summary = "需求预测", description = "基于历史数据预测未来订单需求")
    @GetMapping("/prediction")
    public Result<List<PredictionDTO>> getPredictions(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "预测天数")
            @RequestParam(defaultValue = "7") int days) {
        return Result.success(analysisService.getPredictions(startDate, endDate, days));
    }

    @Operation(summary = "业务洞察", description = "生成数据驱动的业务洞察和建议")
    @GetMapping("/insights")
    public Result<List<BusinessInsightDTO>> generateBusinessInsights(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.generateBusinessInsights(startDate, endDate));
    }

    @Operation(summary = "趋势分析", description = "获取带增长率和移动平均的趋势数据")
    @GetMapping("/trend")
    public Result<List<TrendAnalysisDTO>> getTrendAnalysis(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.getTrendAnalysis(startDate, endDate));
    }

    @Operation(summary = "交叉表分析", description = "获取行维度和列维度的交叉分析数据")
    @GetMapping("/cross-tab")
    public Result<List<Map<String, Object>>> getCrossTabAnalysis(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "行维度")
            @RequestParam(defaultValue = "vendor") String rowDimension,
            @Parameter(description = "列维度")
            @RequestParam(defaultValue = "payment") String colDimension) {
        return Result.success(analysisService.getCrossTabAnalysis(startDate, endDate, rowDimension, colDimension));
    }

    @Operation(summary = "获取KPI汇总", description = "获取与数据看板一致的KPI汇总数据")
    @GetMapping("/kpi-summary")
    public Result<Map<String, Object>> getKpiSummary(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.getKpiSummary(startDate, endDate));
    }

    @Operation(summary = "获取乘客数量分布", description = "获取不同乘客数量的订单分布数据")
    @GetMapping("/passenger/distribution")
    public Result<List<Map<String, Object>>> getPassengerDistribution(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.getPassengerDistribution(startDate, endDate));
    }

    @Operation(summary = "获取小费比率分布", description = "获取不同小费比率区间的订单分布数据")
    @GetMapping("/tip/distribution")
    public Result<List<Map<String, Object>>> getTipDistribution(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(analysisService.getTipDistribution(startDate, endDate));
    }
}
