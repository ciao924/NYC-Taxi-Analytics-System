package com.taxi.analytics.modules.analysis.service.impl;

import com.taxi.analytics.modules.analysis.dto.*;
import com.taxi.analytics.modules.analysis.mapper.AnalysisMapper;
import com.taxi.analytics.modules.analysis.service.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisServiceImpl.class);
    private final AnalysisMapper analysisMapper;
    
    public AnalysisServiceImpl(AnalysisMapper analysisMapper) {
        this.analysisMapper = analysisMapper;
    }

    @Override
    public List<Map<String, Object>> getAirportStatistics(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching airport statistics for {} to {}", startDate, endDate);
        try {
            List<Map<String, Object>> result = analysisMapper.selectAirportStatistics(startDate.toString(), endDate.toString());
            if (result.isEmpty()) {
                log.warn("Airport statistics query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch airport statistics", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getAirportDetailedStatistics(LocalDate startDate, LocalDate endDate, String airportCode) {
        log.debug("Fetching detailed airport statistics for {} to {}, airport={}", startDate, endDate, airportCode);
        try {
            return analysisMapper.selectAirportDetailedStatistics(startDate.toString(), endDate.toString(), airportCode);
        } catch (Exception e) {
            log.error("Failed to fetch detailed airport statistics", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getVendorComparison(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching vendor comparison for {} to {}", startDate, endDate);
        try {
            List<Map<String, Object>> result = analysisMapper.selectVendorComparison(startDate.toString(), endDate.toString());
            if (result.isEmpty()) {
                log.warn("Vendor comparison query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            double totalTrips = result.stream()
                .mapToDouble(m -> ((Number) m.getOrDefault("trip_count", 0)).doubleValue())
                .sum();
            for (Map<String, Object> vendor : result) {
                long trips = ((Number) vendor.getOrDefault("trip_count", 0)).longValue();
                double marketShare = totalTrips > 0 ? (trips * 100.0 / totalTrips) : 0;
                vendor.put("market_share", Math.round(marketShare * 100) / 100.0);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch vendor comparison", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getVendorTrend(LocalDate startDate, LocalDate endDate, String vendorId) {
        log.debug("Fetching vendor trend for {} to {}, vendor={}", startDate, endDate, vendorId);
        try {
            return analysisMapper.selectVendorTrend(startDate.toString(), endDate.toString(), vendorId);
        } catch (Exception e) {
            log.error("Failed to fetch vendor trend", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getPaymentDistribution(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching payment distribution for {} to {}", startDate, endDate);
        try {
            List<Map<String, Object>> result = analysisMapper.selectPaymentDistribution(startDate.toString(), endDate.toString());
            
            if (result.isEmpty()) {
                log.warn("Payment distribution query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch payment distribution", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getPaymentTrend(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching payment trend for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectPaymentTrend(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch payment trend", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getDistanceDistribution(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching distance distribution for {} to {}", startDate, endDate);
        try {
            List<Map<String, Object>> result = analysisMapper.selectDistanceDistribution(startDate.toString(), endDate.toString());
            if (result.isEmpty()) {
                log.warn("Distance distribution query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch distance distribution", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getDurationDistribution(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching duration distribution for {} to {}", startDate, endDate);
        try {
            List<Map<String, Object>> result = analysisMapper.selectDurationDistribution(startDate.toString(), endDate.toString());
            if (result.isEmpty()) {
                log.warn("Duration distribution query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch duration distribution", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MultiDimensionAnalysisDTO> getMultiDimensionAnalysis(LocalDate startDate, LocalDate endDate, String dimension1, String dimension2) {
        log.debug("Fetching multi-dimension analysis: {} × {} for {} to {}", dimension1, dimension2, startDate, endDate);
        List<MultiDimensionAnalysisDTO> results = new ArrayList<>();
        
        try {
            List<Map<String, Object>> rawData;
            
            String dim1 = dimension1.toLowerCase();
            String dim2 = dimension2.toLowerCase();
            
            if ((dim1.equals("vendor") && dim2.equals("payment")) || (dim1.equals("payment") && dim2.equals("vendor"))) {
                rawData = analysisMapper.selectMultiDimensionAnalysis(startDate.toString(), endDate.toString(), dim1, dim2);
            } else if ((dim1.equals("vendor") && dim2.equals("airport")) || (dim1.equals("airport") && dim2.equals("vendor"))) {
                rawData = analysisMapper.selectMultiDimensionAnalysisVendorAirport(startDate.toString(), endDate.toString());
            } else if ((dim1.equals("airport") && dim2.equals("payment")) || (dim1.equals("payment") && dim2.equals("airport"))) {
                rawData = analysisMapper.selectMultiDimensionAnalysisAirportPayment(startDate.toString(), endDate.toString());
            } else {
                rawData = analysisMapper.selectMultiDimensionAnalysis(startDate.toString(), endDate.toString(), dim1, dim2);
            }
            
            if (rawData.isEmpty()) {
                log.warn("Multi-dimension analysis query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            
            long totalTrips = rawData.stream()
                .mapToLong(row -> ((Number) row.getOrDefault("trip_count", 0)).longValue())
                .sum();
            
            for (Map<String, Object> row : rawData) {
                long tripCount = ((Number) row.getOrDefault("trip_count", 0)).longValue();
                double percentage = totalTrips > 0 ? (tripCount * 100.0 / totalTrips) : 0;
                
                String dim1Value = String.valueOf(row.getOrDefault("dim1_value", ""));
                String dim2Value = String.valueOf(row.getOrDefault("dim2_value", ""));
                
                MultiDimensionAnalysisDTO dto = MultiDimensionAnalysisDTO.builder()
                    .dimension1(dim1.equals("payment") && dim2.equals("vendor") ? dim2Value : dim1Value)
                    .dimension1Name(dim1.equals("payment") && dim2.equals("vendor") ? String.valueOf(row.getOrDefault("dim2_name", "")) : String.valueOf(row.getOrDefault("dim1_name", "")))
                    .dimension2(dim1.equals("payment") && dim2.equals("vendor") ? dim1Value : dim2Value)
                    .dimension2Name(dim1.equals("payment") && dim2.equals("vendor") ? String.valueOf(row.getOrDefault("dim1_name", "")) : String.valueOf(row.getOrDefault("dim2_name", "")))
                    .tripCount(tripCount)
                    .totalAmount(Math.round(((Number) row.getOrDefault("total_amount", 0)).doubleValue() * 100) / 100.0)
                    .avgAmount(Math.round(((Number) row.getOrDefault("avg_amount", 0)).doubleValue() * 100) / 100.0)
                    .avgDistance(Math.round(((Number) row.getOrDefault("avg_distance", 0)).doubleValue() * 100) / 100.0)
                    .percentage(Math.round(percentage * 100) / 100.0)
                    .statDate(row.containsKey("stat_date") ? LocalDate.parse(String.valueOf(row.get("stat_date"))) : null)
                    .build();
                results.add(dto);
            }
        } catch (Exception e) {
            log.error("Failed to fetch multi-dimension analysis", e);
            return Collections.emptyList();
        }
        
        return results;
    }

    @Override
    public List<AnomalyDetectionDTO> detectAnomalies(LocalDate startDate, LocalDate endDate) {
        log.debug("Detecting anomalies for {} to {}", startDate, endDate);
        List<AnomalyDetectionDTO> anomalies = new ArrayList<>();
        
        try {
            List<Map<String, Object>> rawData = analysisMapper.selectAnomalyData(startDate.toString(), endDate.toString());
            if (rawData.isEmpty()) {
                log.warn("Anomaly detection query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            anomalies = analyzeAnomalies(rawData);
        } catch (Exception e) {
            log.error("Failed to detect anomalies from database", e);
            return Collections.emptyList();
        }
        
        return anomalies;
    }

    @Override
    public List<PredictionDTO> getPredictions(LocalDate startDate, LocalDate endDate, int days) {
        log.debug("Generating predictions for {} to {}, days={}", startDate, endDate, days);
        List<PredictionDTO> predictions = new ArrayList<>();
        
        try {
            List<Map<String, Object>> historicalData = analysisMapper.selectHistoricalTrend(startDate.toString(), endDate.toString());
            if (historicalData.isEmpty()) {
                log.warn("Historical trend query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            predictions = generatePredictionsFromHistory(historicalData, days);
        } catch (Exception e) {
            log.error("Failed to generate predictions from database", e);
            return Collections.emptyList();
        }
        
        return predictions;
    }

    @Override
    public List<BusinessInsightDTO> generateBusinessInsights(LocalDate startDate, LocalDate endDate) {
        log.debug("Generating business insights for {} to {}", startDate, endDate);
        List<BusinessInsightDTO> insights = new ArrayList<>();
        
        try {
            insights = analyzeAndGenerateInsights(startDate, endDate);
        } catch (Exception e) {
            log.error("Failed to generate business insights", e);
            return Collections.emptyList();
        }
        
        return insights;
    }

    @Override
    public List<TrendAnalysisDTO> getTrendAnalysis(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching trend analysis for {} to {}", startDate, endDate);
        List<TrendAnalysisDTO> trends = new ArrayList<>();
        
        try {
            List<Map<String, Object>> rawData = analysisMapper.selectTrendAnalysis(startDate.toString(), endDate.toString());
            
            if (rawData.isEmpty()) {
                log.warn("Trend analysis query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            
            for (Map<String, Object> row : rawData) {
                TrendAnalysisDTO dto = TrendAnalysisDTO.builder()
                    .date(row.containsKey("stat_date") ? LocalDate.parse(String.valueOf(row.get("stat_date"))) : null)
                    .tripCount(((Number) row.getOrDefault("trip_count", 0)).longValue())
                    .totalAmount(((Number) row.getOrDefault("total_amount", 0)).doubleValue())
                    .avgAmount(((Number) row.getOrDefault("avg_amount", 0)).doubleValue())
                    .avgDistance(((Number) row.getOrDefault("avg_distance", 0)).doubleValue())
                    .passengerCount(((Number) row.getOrDefault("passenger_count", 0)).longValue())
                    .growthRate(((Number) row.getOrDefault("growth_rate", 0)).doubleValue())
                    .movingAverage(((Number) row.getOrDefault("moving_average", 0)).doubleValue())
                    .build();
                trends.add(dto);
            }
            
            calculateMovingAverageAndGrowth(trends);
        } catch (Exception e) {
            log.error("Failed to fetch trend analysis", e);
            return Collections.emptyList();
        }
        
        return trends;
    }

    @Override
    public List<Map<String, Object>> getCrossTabAnalysis(LocalDate startDate, LocalDate endDate, String rowDimension, String colDimension) {
        log.debug("Fetching cross-tab analysis: {} vs {} for {} to {}", rowDimension, colDimension, startDate, endDate);
        
        try {
            List<Map<String, Object>> result = analysisMapper.selectCrossTabAnalysis(startDate.toString(), endDate.toString(), rowDimension, colDimension);
            if (result.isEmpty()) {
                log.warn("Cross-tab analysis query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch cross-tab analysis", e);
            return Collections.emptyList();
        }
    }

    private void calculateMovingAverageAndGrowth(List<TrendAnalysisDTO> trends) {
        if (trends.isEmpty()) return;
        
        for (int i = 0; i < trends.size(); i++) {
            if (i > 0) {
                TrendAnalysisDTO current = trends.get(i);
                TrendAnalysisDTO previous = trends.get(i - 1);
                
                if (previous.getTripCount() > 0) {
                    double growth = ((current.getTripCount() - previous.getTripCount()) * 100.0) / previous.getTripCount();
                    current.setGrowthRate(Math.round(growth * 100) / 100.0);
                }
                
                int windowSize = Math.min(7, i + 1);
                double sum = 0;
                for (int j = i - windowSize + 1; j <= i; j++) {
                    sum += trends.get(j).getTripCount();
                }
                current.setMovingAverage((double) Math.round(sum / windowSize));
            }
        }
    }

    private List<AnomalyDetectionDTO> analyzeAnomalies(List<Map<String, Object>> rawData) {
        List<AnomalyDetectionDTO> anomalies = new ArrayList<>();
        Map<String, List<Double>> metricData = new HashMap<>();
        
        for (Map<String, Object> row : rawData) {
            String metric = String.valueOf(row.get("metric_name"));
            double value = ((Number) row.getOrDefault("value", 0)).doubleValue();
            metricData.computeIfAbsent(metric, k -> new ArrayList<>()).add(value);
        }
        
        for (Map.Entry<String, List<Double>> entry : metricData.entrySet()) {
            List<Double> values = entry.getValue();
            if (values.size() < 7) continue;
            
            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
            double stdDev = Math.sqrt(variance);
            
            for (int i = 0; i < values.size(); i++) {
                double zScore = Math.abs((values.get(i) - mean) / stdDev);
                if (zScore > 3) {
                    String level = zScore > 4 ? "critical" : zScore > 3.5 ? "high" : "medium";
                    String type = values.get(i) > mean ? "spike" : "drop";
                    
                    AnomalyDetectionDTO anomaly = AnomalyDetectionDTO.builder()
                        .metricName(entry.getKey())
                        .metricDisplayName(getMetricDisplayName(entry.getKey()))
                        .anomalyDate(LocalDate.now().minusDays(values.size() - 1 - i))
                        .actualValue(values.get(i))
                        .expectedValue(Math.round(mean * 100) / 100.0)
                        .deviationPercent(Math.round(zScore * 100) / 100.0)
                        .anomalyLevel(level)
                        .anomalyType(type)
                        .description(buildAnomalyDescription(entry.getKey(), type, zScore))
                        .potentialCauses(getPotentialCauses(entry.getKey(), type))
                        .rootCauses(generateRootCauses(entry.getKey()))
                        .build();
                    
                    anomalies.add(anomaly);
                }
            }
        }
        
        return anomalies.stream()
            .sorted((a, b) -> {
                int levelCompare = getLevelPriority(a.getAnomalyLevel()) - getLevelPriority(b.getAnomalyLevel());
                if (levelCompare != 0) return levelCompare;
                return b.getDeviationPercent().compareTo(a.getDeviationPercent());
            })
            .limit(10)
            .collect(Collectors.toList());
    }

    private int getLevelPriority(String level) {
        switch (level) {
            case "critical": return 0;
            case "high": return 1;
            case "medium": return 2;
            default: return 3;
        }
    }

    private String getMetricDisplayName(String metric) {
        Map<String, String> displayNames = new HashMap<>();
        displayNames.put("trip_count", "订单数量");
        displayNames.put("total_amount", "总收入");
        displayNames.put("avg_amount", "平均金额");
        displayNames.put("avg_distance", "平均距离");
        return displayNames.getOrDefault(metric, metric);
    }

    private String buildAnomalyDescription(String metric, String type, double zScore) {
        String direction = type.equals("spike") ? "异常激增" : "异常下降";
        String metricName = getMetricDisplayName(metric);
        return String.format("%s出现%s，偏离均值%.1fσ", metricName, direction, zScore);
    }

    private List<String> getPotentialCauses(String metric, String type) {
        List<String> causes = new ArrayList<>();
        
        if ("trip_count".equals(metric)) {
            if ("spike".equals(type)) {
                causes.add("节假日或大型活动影响");
                causes.add("天气变化导致出行需求增加");
                causes.add("特殊事件（演唱会、体育赛事等）");
            } else {
                causes.add("恶劣天气导致出行减少");
                causes.add("公共交通罢工或延误");
                causes.add("疫情或突发事件影响");
            }
        } else if ("total_amount".equals(metric)) {
            causes.add("价格调整或促销活动");
            causes.add("高价值订单比例变化");
            causes.add("支付方式分布变化");
        }
        
        return causes;
    }

    private List<AnomalyDetectionDTO.RootCauseAnalysis> generateRootCauses(String metric) {
        List<AnomalyDetectionDTO.RootCauseAnalysis> rootCauses = new ArrayList<>();
        
        if ("trip_count".equals(metric)) {
            rootCauses.add(AnomalyDetectionDTO.RootCauseAnalysis.builder()
                .dimension("供应商")
                .value("Creative Mobile Technologies")
                .contribution(45.0)
                .impactDescription("该供应商订单量增长显著")
                .build());
            rootCauses.add(AnomalyDetectionDTO.RootCauseAnalysis.builder()
                .dimension("区域")
                .value("曼哈顿")
                .contribution(30.0)
                .impactDescription("曼哈顿区域订单量占比最高")
                .build());
        }
        
        return rootCauses;
    }

    private List<PredictionDTO> generatePredictionsFromHistory(List<Map<String, Object>> historicalData, int days) {
        List<PredictionDTO> predictions = new ArrayList<>();
        
        if (historicalData.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Double> historicalValues = historicalData.stream()
            .map(row -> ((Number) row.getOrDefault("trip_count", 0)).doubleValue())
            .collect(Collectors.toList());
        
        double avgGrowth = 0;
        for (int i = 1; i < historicalValues.size(); i++) {
            if (historicalValues.get(i - 1) > 0) {
                avgGrowth += (historicalValues.get(i) - historicalValues.get(i - 1)) / historicalValues.get(i - 1);
            }
        }
        avgGrowth = historicalValues.size() > 1 ? avgGrowth / (historicalValues.size() - 1) : 0.02;
        
        double lastValue = historicalValues.get(historicalValues.size() - 1);
        LocalDate lastDate = historicalData.isEmpty() ? LocalDate.now() : 
            LocalDate.parse(String.valueOf(historicalData.get(historicalData.size() - 1).get("stat_date")));
        
        for (int i = 1; i <= days; i++) {
            double predicted = lastValue * Math.pow(1 + avgGrowth, i);
            double variance = 0.1;
            double lowerBound = predicted * (1 - variance);
            double upperBound = predicted * (1 + variance);
            
            predictions.add(PredictionDTO.builder()
                .date(lastDate.plusDays(i))
                .metricName("trip_count")
                .predictedValue((double) Math.round(predicted))
                .lowerBound((double) Math.round(lowerBound))
                .upperBound((double) Math.round(upperBound))
                .confidence(0.85)
                .trend(avgGrowth * 100)
                .trendDirection(avgGrowth > 0 ? "up" : "down")
                .build());
        }
        
        return predictions;
    }

    private List<BusinessInsightDTO> analyzeAndGenerateInsights(LocalDate startDate, LocalDate endDate) {
        List<BusinessInsightDTO> insights = new ArrayList<>();
        
        List<Map<String, Object>> vendorData = getVendorComparison(startDate, endDate);
        List<Map<String, Object>> paymentData = getPaymentDistribution(startDate, endDate);
        List<Map<String, Object>> airportData = getAirportStatistics(startDate, endDate);
        
        double totalTrips = vendorData.stream()
            .mapToDouble(m -> ((Number) m.getOrDefault("trip_count", 0)).doubleValue())
            .sum();
        
        if (totalTrips > 0) {
            Map<String, Object> topVendor = vendorData.stream()
                .max(Comparator.comparing(m -> ((Number) m.getOrDefault("market_share", 0)).doubleValue()))
                .orElse(null);
            
            if (topVendor != null) {
                double marketShare = ((Number) topVendor.getOrDefault("market_share", 0)).doubleValue();
                if (marketShare > 60) {
                    insights.add(BusinessInsightDTO.builder()
                        .insightId("INSIGHT_001")
                        .title("供应商市场集中度较高")
                        .category("市场竞争")
                        .level("high")
                        .description(String.format("%s占据了%.1f%%的市场份额，市场集中度较高", 
                            topVendor.get("vendor_name"), marketShare))
                        .recommendation("建议关注供应商多样性，降低单一供应商依赖风险")
                        .supportingData(Arrays.asList(
                            String.format("市场份额: %.1f%%", marketShare),
                            "供应商数量: 2家"
                        ))
                        .discoveryDate(LocalDate.now())
                        .impactScore(85.0)
                        .build());
                }
            }
        }
        
        double creditCardPercentage = paymentData.stream()
            .filter(p -> "credit_card".equals(p.get("payment_code")))
            .mapToDouble(p -> ((Number) p.getOrDefault("percentage", 0)).doubleValue())
            .findFirst().orElse(0);
        
        if (creditCardPercentage < 50) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_002")
                .title("现金支付占比偏高")
                .category("支付行为")
                .level("medium")
                .description(String.format("现金支付占比%.1f%%，高于行业平均水平", creditCardPercentage))
                .recommendation("建议推广电子支付方式，提升支付效率和安全性")
                .supportingData(Arrays.asList(
                    String.format("信用卡支付: %.1f%%", creditCardPercentage),
                    String.format("现金支付: %.1f%%", 100 - creditCardPercentage)
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(70.0)
                .build());
        }
        
        double jfkPercentage = airportData.stream()
            .filter(a -> "JFK".equals(a.get("airport_code")))
            .mapToDouble(a -> {
                long trips = ((Number) a.getOrDefault("trip_count", 0)).longValue();
                double total = airportData.stream()
                    .mapToDouble(m -> ((Number) m.getOrDefault("trip_count", 0)).doubleValue())
                    .sum();
                return total > 0 ? trips * 100.0 / total : 0;
            })
            .findFirst().orElse(0);
        
        if (jfkPercentage > 40) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_003")
                .title("JFK机场订单占比领先")
                .category("交通枢纽")
                .level("medium")
                .description(String.format("肯尼迪国际机场贡献了%.1f%%的机场订单", jfkPercentage))
                .recommendation("考虑在JFK机场增加运力配置，优化服务质量")
                .supportingData(Arrays.asList(
                    String.format("JFK订单占比: %.1f%%", jfkPercentage),
                    "三大机场: JFK、LGA、EWR"
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(65.0)
                .build());
        }
        
        return insights;
    }

    

    @Override
    public Map<String, Object> getKpiSummary(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching KPI summary for {} to {}", startDate, endDate);
        try {
            Map<String, Object> result = analysisMapper.selectKpiSummary(startDate.toString(), endDate.toString());
            if (result.isEmpty() || result.get("trip_count") == null) {
                log.warn("KPI summary query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyMap();
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch KPI summary", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public List<Map<String, Object>> getPassengerDistribution(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching passenger distribution for {} to {}", startDate, endDate);
        try {
            List<Map<String, Object>> result = analysisMapper.selectPassengerDistribution(startDate.toString(), endDate.toString());
            if (result.isEmpty()) {
                log.warn("Passenger distribution query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch passenger distribution", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getTipDistribution(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching tip distribution for {} to {}", startDate, endDate);
        try {
            List<Map<String, Object>> result = analysisMapper.selectTipDistribution(startDate.toString(), endDate.toString());
            if (result.isEmpty()) {
                log.warn("Tip distribution query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch tip distribution", e);
            return Collections.emptyList();
        }
    }

}