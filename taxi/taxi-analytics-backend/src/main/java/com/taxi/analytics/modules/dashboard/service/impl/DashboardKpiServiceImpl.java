package com.taxi.analytics.modules.dashboard.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.taxi.analytics.modules.dashboard.mapper.DashboardKpiMapper;
import com.taxi.analytics.modules.dashboard.service.DashboardKpiService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings("unchecked")
public class DashboardKpiServiceImpl extends ServiceImpl<DashboardKpiMapper, com.taxi.analytics.modules.dashboard.entity.DashboardKpi> implements DashboardKpiService {

    // Caffeine 缓存
    private final Cache<String, Object> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    @Override
    public Map<String, Object> getKpiSummary(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "kpi_summary:" + startDate + ":" + endDate;
        return (Map<String, Object>) cache.get(cacheKey, k -> {
            Map<String, Object> summary = baseMapper.getKpiSummary(startDate, endDate);
            // 计算环比（假设环比是与前一个周期比较）
            long daysDiff = endDate.toEpochDay() - startDate.toEpochDay() + 1;
            LocalDate prevStartDate = startDate.minusDays(daysDiff);
            LocalDate prevEndDate = startDate.minusDays(1);
            Map<String, Object> prevSummary = baseMapper.getKpiSummary(prevStartDate, prevEndDate);
            
            if (prevSummary != null && summary != null) {
                summary.put("tripCountGrowth", calculateGrowth(summary.get("trip_count"), prevSummary.get("trip_count")));
                summary.put("totalFareGrowth", calculateGrowth(summary.get("total_revenue"), prevSummary.get("total_revenue")));
            }
            return summary;
        });
    }

    @Override
    public List<Map<String, Object>> getKpiTrend(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "kpi_trend:" + startDate + ":" + endDate;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            baseMapper.getKpiTrend(startDate, endDate)
        );
    }

    @Override
    public List<Map<String, Object>> getHourlyDistribution(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "hourly_distribution:" + startDate + ":" + endDate;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            baseMapper.getHourlyDistribution(startDate, endDate)
        );
    }

    @Override
    public List<Map<String, Object>> getWeekdayAnalysis(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "weekday_analysis:" + startDate + ":" + endDate;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            baseMapper.getWeekdayAnalysis(startDate, endDate)
        );
    }

    @Override
    public List<Map<String, Object>> getPaymentAnalysis(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "payment_analysis:" + startDate + ":" + endDate;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            baseMapper.getPaymentAnalysis(startDate, endDate)
        );
    }

    @Override
    public List<Map<String, Object>> getFeeComposition(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "fee_composition:" + startDate + ":" + endDate;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            baseMapper.getFeeComposition(startDate, endDate)
        );
    }

    @Override
    public List<Map<String, Object>> getFeePercentage(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "fee_percentage:" + startDate + ":" + endDate;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            baseMapper.getFeePercentage(startDate, endDate)
        );
    }

    @Override
    public List<Map<String, Object>> getZoneHotspots(LocalDate startDate, LocalDate endDate, int limit) {
        String cacheKey = "zone_hotspots:" + startDate + ":" + endDate + ":" + limit;
        // ✅ 修复：将 getZoneHotspots 改为 getPickupHotspots
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            baseMapper.getPickupHotspots(startDate, endDate, limit)
        );
    }

    @Override
    public List<Map<String, Object>> getBoroughFlow(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "borough_flow:" + startDate + ":" + endDate;
        return (List<Map<String, Object>>) cache.get(cacheKey, k -> 
            baseMapper.getBoroughFlow(startDate, endDate)
        );
    }

    @Override
    public Map<String, LocalDate> getAvailableDateRange() {
        String cacheKey = "available_date_range";
        return (Map<String, LocalDate>) cache.get(cacheKey, k -> 
            baseMapper.getAvailableDateRange()
        );
    }

    @Override
    public void refreshCache() {
        cache.invalidateAll();
    }

    // 计算环比增长率
    private Double calculateGrowth(Object current, Object previous) {
        if (current == null || previous == null) {
            return null;
        }
        try {
            double currentValue = Double.parseDouble(current.toString());
            double previousValue = Double.parseDouble(previous.toString());
            if (previousValue == 0) {
                return null;
            }
            return ((currentValue - previousValue) / previousValue) * 100;
        } catch (Exception e) {
            return null;
        }
    }
}