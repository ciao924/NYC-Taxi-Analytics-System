package com.taxi.analytics.modules.dashboard.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.taxi.analytics.modules.dashboard.dto.*;
import com.taxi.analytics.modules.dashboard.entity.DashboardKpi;
import com.taxi.analytics.modules.dashboard.mapper.DashboardKpiMapper;
import com.taxi.analytics.modules.dashboard.service.DashboardKpiService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class DashboardKpiServiceImpl extends ServiceImpl<DashboardKpiMapper, DashboardKpi> implements DashboardKpiService {

    private final Cache<String, Object> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    @Override
    public KpiSummaryDTO getKpiSummary(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "kpi_summary:" + startDate + ":" + endDate;
        return (KpiSummaryDTO) cache.get(cacheKey, k -> {
            Map<String, Object> summary = baseMapper.getKpiSummary(startDate, endDate);
            long daysDiff = endDate.toEpochDay() - startDate.toEpochDay() + 1;
            LocalDate prevStartDate = startDate.minusDays(daysDiff);
            LocalDate prevEndDate = startDate.minusDays(1);
            Map<String, Object> prevSummary = baseMapper.getKpiSummary(prevStartDate, prevEndDate);
            
            KpiSummaryDTO dto = KpiSummaryDTO.builder()
                    .tripCount(getLongValue(summary, "trip_count"))
                    .totalRevenue(getDoubleValue(summary, "total_revenue"))
                    .avgFare(getDoubleValue(summary, "avg_fare"))
                    .avgDistance(getDoubleValue(summary, "avg_distance"))
                    .build();
            
            return dto;
        });
    }

    @Override
    public List<TrendDataDTO> getKpiTrend(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "kpi_trend:" + startDate + ":" + endDate;
        return (List<TrendDataDTO>) cache.get(cacheKey, k -> 
            baseMapper.getKpiTrend(startDate, endDate).stream()
                .map(this::mapToTrendDataDTO)
                .collect(Collectors.toList())
        );
    }

    @Override
    public List<HourlyDistributionDTO> getHourlyDistribution(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "hourly_distribution:" + startDate + ":" + endDate;
        return (List<HourlyDistributionDTO>) cache.get(cacheKey, k -> 
            baseMapper.getHourlyDistribution(startDate, endDate).stream()
                .map(this::mapToHourlyDistributionDTO)
                .collect(Collectors.toList())
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
    public List<PaymentDistributionDTO> getPaymentAnalysis(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "payment_analysis:" + startDate + ":" + endDate;
        return (List<PaymentDistributionDTO>) cache.get(cacheKey, k -> 
            baseMapper.getPaymentAnalysis(startDate, endDate).stream()
                .map(this::mapToPaymentDistributionDTO)
                .collect(Collectors.toList())
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

    private TrendDataDTO mapToTrendDataDTO(Map<String, Object> map) {
        return TrendDataDTO.builder()
                .statDate(getStringValue(map, "stat_date"))
                .totalTrips(getLongValue(map, "total_trips"))
                .totalRevenue(getDoubleValue(map, "total_revenue"))
                .avgFare(getDoubleValue(map, "avg_fare"))
                .build();
    }

    private HourlyDistributionDTO mapToHourlyDistributionDTO(Map<String, Object> map) {
        return HourlyDistributionDTO.builder()
                .hour(getIntValue(map, "hour_of_day"))
                .tripCount(getLongValue(map, "trip_count"))
                .avgFare(getDoubleValue(map, "avg_fare"))
                .build();
    }

    private PaymentDistributionDTO mapToPaymentDistributionDTO(Map<String, Object> map) {
        return PaymentDistributionDTO.builder()
                .paymentType(getStringValue(map, "payment_name"))
                .paymentTypeName(getStringValue(map, "payment_name"))
                .tripCount(getLongValue(map, "trip_count"))
                .percentage(getDoubleValue(map, "trip_ratio"))
                .build();
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}