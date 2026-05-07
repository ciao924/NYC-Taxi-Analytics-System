package com.taxi.analytics.modules.analysis.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AnalysisService {

    /**
     * 获取机场统计
     */
    List<Map<String, Object>> getAirportStatistics(LocalDate startDate, LocalDate endDate);

    /**
     * 获取机场详细统计
     */
    List<Map<String, Object>> getAirportDetailedStatistics(LocalDate startDate, LocalDate endDate, String airportCode);

    /**
     * 获取供应商对比
     */
    List<Map<String, Object>> getVendorComparison(LocalDate startDate, LocalDate endDate);

    /**
     * 获取供应商趋势
     */
    List<Map<String, Object>> getVendorTrend(LocalDate startDate, LocalDate endDate, String vendorId);

    /**
     * 获取支付方式分布
     */
    List<Map<String, Object>> getPaymentDistribution(LocalDate startDate, LocalDate endDate);

    /**
     * 获取支付方式趋势
     */
    List<Map<String, Object>> getPaymentTrend(LocalDate startDate, LocalDate endDate);

    /**
     * 获取行程距离分布
     */
    List<Map<String, Object>> getDistanceDistribution(LocalDate startDate, LocalDate endDate);

    /**
     * 获取行程时长分布
     */
    List<Map<String, Object>> getDurationDistribution(LocalDate startDate, LocalDate endDate);
}