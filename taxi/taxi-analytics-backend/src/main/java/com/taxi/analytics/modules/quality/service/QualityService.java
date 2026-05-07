package com.taxi.analytics.modules.quality.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface QualityService {
    
    /**
     * 获取质量总览评分
     */
    Map<String, Object> getQualitySummary(LocalDate date);
    
    /**
     * 获取所有表健康状态
     */
    List<Map<String, Object>> getTableHealthStatus(LocalDate date);
    
    /**
     * 获取完整性检测详情
     */
    List<Map<String, Object>> getCompletenessDetails(LocalDate date);
    
    /**
     * 获取空值率详情
     */
    List<Map<String, Object>> getNullRateDetails(LocalDate date);
    
    /**
     * 获取唯一性检测
     */
    List<Map<String, Object>> getUniquenessDetails(LocalDate date);
    
    /**
     * 获取一致性检测
     */
    List<Map<String, Object>> getConsistencyDetails(LocalDate date);
    
    /**
     * 获取范围检测
     */
    List<Map<String, Object>> getRangeDetails(LocalDate date);
    
    /**
     * 获取数据及时性
     */
    List<Map<String, Object>> getFreshnessDetails(LocalDate date);
    
    /**
     * 获取告警列表（使用默认 limit = 100）
     */
    List<Map<String, Object>> getAlerts(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取告警列表（指定返回条数）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param limit 返回条数限制
     */
    List<Map<String, Object>> getAlerts(LocalDate startDate, LocalDate endDate, int limit);
    
    /**
     * 获取质量历史趋势
     */
    List<Map<String, Object>> getQualityHistory(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取阈值配置
     */
    List<Map<String, Object>> getThresholds();
    
    /**
     * 执行质量检测
     */
    void executeQualityCheck();
    
    /**
     * 生成质量报告
     */
    Map<String, Object> generateQualityReport(LocalDate date);
}