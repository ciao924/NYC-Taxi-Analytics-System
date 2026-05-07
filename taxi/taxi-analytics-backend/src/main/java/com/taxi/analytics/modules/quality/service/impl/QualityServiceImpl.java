package com.taxi.analytics.modules.quality.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.taxi.analytics.modules.quality.mapper.QualityMapper;
import com.taxi.analytics.modules.quality.service.QualityService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings("unchecked")
public class QualityServiceImpl implements QualityService {

    private final QualityMapper qualityMapper;
    private final Cache<String, Object> cache;

    // 默认告警查询条数
    private static final int DEFAULT_ALERT_LIMIT = 100;

    public QualityServiceImpl(QualityMapper qualityMapper) {
        this.qualityMapper = qualityMapper;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(50)
                .build();
    }

    @Override
    public Map<String, Object> getQualitySummary(LocalDate date) {
        String cacheKey = "quality_summary:" + date;
        return (Map<String, Object>) cache.get(cacheKey, k -> 
            qualityMapper.getQualitySummary(date)
        );
    }

    @Override
    public List<Map<String, Object>> getTableHealthStatus(LocalDate date) {
        String cacheKey = "table_health:" + date;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            qualityMapper.getTableHealthStatus(date)
        );
    }

    @Override
    public List<Map<String, Object>> getCompletenessDetails(LocalDate date) {
        String cacheKey = "completeness:" + date;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            qualityMapper.getCompletenessDetails(date)
        );
    }

    @Override
    public List<Map<String, Object>> getNullRateDetails(LocalDate date) {
        String cacheKey = "null_rate:" + date;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            qualityMapper.getNullRateDetails(date)
        );
    }

    @Override
    public List<Map<String, Object>> getUniquenessDetails(LocalDate date) {
        String cacheKey = "uniqueness:" + date;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            qualityMapper.getUniquenessDetails(date)
        );
    }

    @Override
    public List<Map<String, Object>> getConsistencyDetails(LocalDate date) {
        String cacheKey = "consistency:" + date;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            qualityMapper.getConsistencyDetails(date)
        );
    }

    @Override
    public List<Map<String, Object>> getRangeDetails(LocalDate date) {
        String cacheKey = "range:" + date;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            qualityMapper.getRangeDetails(date)
        );
    }

    @Override
    public List<Map<String, Object>> getFreshnessDetails(LocalDate date) {
        String cacheKey = "freshness:" + date;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            qualityMapper.getFreshnessDetails(date)
        );
    }

    /**
     * 获取告警列表（使用默认 limit = 100）
     */
    @Override
    public List<Map<String, Object>> getAlerts(LocalDate startDate, LocalDate endDate) {
        return getAlerts(startDate, endDate, DEFAULT_ALERT_LIMIT);
    }

    /**
     * 获取告警列表（指定 limit）
     */
    @Override
    public List<Map<String, Object>> getAlerts(LocalDate startDate, LocalDate endDate, int limit) {
        String cacheKey = "alerts:" + startDate + ":" + endDate + ":" + limit;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            qualityMapper.getAlerts(startDate, endDate, limit)
        );
    }

    @Override
    public List<Map<String, Object>> getQualityHistory(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "quality_history:" + startDate + ":" + endDate;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            qualityMapper.getQualityHistory(startDate, endDate)
        );
    }

    @Override
    public List<Map<String, Object>> getThresholds() {
        String cacheKey = "thresholds";
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            qualityMapper.getThresholds()
        );
    }

    @Override
    public void executeQualityCheck() {
        // 这里实现质量检测的逻辑
        // 1. 遍历所有表
        // 2. 执行各项检测
        // 3. 写入检测结果到数据库
        // 4. 触发告警
        cache.invalidateAll();
    }

    @Override
    public Map<String, Object> generateQualityReport(LocalDate date) {
        String cacheKey = "quality_report:" + date;
        return (Map<String, Object>) cache.get(cacheKey, k -> {
            Map<String, Object> report = new java.util.HashMap<>();
            report.put("summary", getQualitySummary(date));
            report.put("tableHealth", getTableHealthStatus(date));
            report.put("completeness", getCompletenessDetails(date));
            report.put("nullRate", getNullRateDetails(date));
            report.put("uniqueness", getUniquenessDetails(date));
            report.put("consistency", getConsistencyDetails(date));
            report.put("range", getRangeDetails(date));
            report.put("freshness", getFreshnessDetails(date));
            return report;
        });
    }
}