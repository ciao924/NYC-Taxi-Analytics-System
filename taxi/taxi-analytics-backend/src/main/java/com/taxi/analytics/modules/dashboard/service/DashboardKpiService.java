package com.taxi.analytics.modules.dashboard.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taxi.analytics.modules.dashboard.entity.DashboardKpi;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DashboardKpiService extends IService<DashboardKpi> {
    
    /**
     * 获取KPI汇总数据
     */
    Map<String, Object> getKpiSummary(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取KPI趋势数据
     */
    List<Map<String, Object>> getKpiTrend(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取小时分布数据
     */
    List<Map<String, Object>> getHourlyDistribution(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取星期分析数据
     */
    List<Map<String, Object>> getWeekdayAnalysis(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取支付方式分析
     */
    List<Map<String, Object>> getPaymentAnalysis(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取费用构成
     */
    List<Map<String, Object>> getFeeComposition(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取费用占比
     */
    List<Map<String, Object>> getFeePercentage(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取上下客热点
     */
    List<Map<String, Object>> getZoneHotspots(LocalDate startDate, LocalDate endDate, int limit);
    
    /**
     * 获取行政区流量
     */
    List<Map<String, Object>> getBoroughFlow(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取可用日期范围
     */
    Map<String, LocalDate> getAvailableDateRange();
    
    /**
     * 刷新缓存
     */
    void refreshCache();
}