package com.taxi.analytics.modules.analysis.service.impl;

import com.taxi.analytics.modules.analysis.dto.*;
import com.taxi.analytics.modules.analysis.mapper.AnalysisMapper;
import com.taxi.analytics.modules.analysis.service.AnalysisService;
import com.taxi.analytics.modules.analysis.util.DataMiningUtils;
import com.taxi.analytics.modules.analysis.util.DataMiningUtils.Cluster;
import com.taxi.analytics.modules.analysis.util.DataMiningUtils.DataPoint;
import com.taxi.analytics.modules.analysis.util.DataMiningUtils.LinearRegressionResult;
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
    public List<Map<String, Object>> getKpiDailyTrend(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching KPI daily trend for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectKpiDailyTrend(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch KPI daily trend", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getAirportTrend(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching airport trend for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectAirportTrend(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch airport trend", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getHourlyDistribution(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching hourly distribution for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectHourlyDistribution(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch hourly distribution", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getWeekdayAnalysis(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching weekday analysis for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectWeekdayAnalysis(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch weekday analysis", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getBoroughFlow(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching borough flow for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectBoroughFlow(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch borough flow", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getRevenueContribution(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching revenue contribution for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectRevenueContribution(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch revenue contribution", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getFeeComposition(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching fee composition for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectFeeComposition(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch fee composition", e);
            return Collections.emptyList();
        }
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
    public List<Map<String, Object>> getVendorTrend(LocalDate startDate, LocalDate endDate, String vendorName) {
        log.debug("Fetching vendor trend for {} to {}, vendor={}", startDate, endDate, vendorName);
        try {
            return analysisMapper.selectVendorTrend(startDate.toString(), endDate.toString(), vendorName);
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

            long totalCount = result.stream()
                .mapToLong(m -> ((Number) m.getOrDefault("trip_count", 0)).longValue())
                .sum();

            for (Map<String, Object> row : result) {
                long count = ((Number) row.get("trip_count")).longValue();
                double percentage = totalCount > 0 ? (count * 100.0 / totalCount) : 0;
                row.put("percentage", Math.round(percentage * 100) / 100.0);
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

            long totalCount = result.stream()
                .mapToLong(m -> ((Number) m.getOrDefault("trip_count", 0)).longValue())
                .sum();

            for (Map<String, Object> row : result) {
                long count = ((Number) row.get("trip_count")).longValue();
                double percentage = totalCount > 0 ? (count * 100.0 / totalCount) : 0;
                row.put("percentage", Math.round(percentage * 100) / 100.0);
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

        try {
            List<Map<String, Object>> rawData;
            String dim1 = dimension1.toLowerCase();
            String dim2 = dimension2.toLowerCase();

            // 根据维度组合选择正确的查询
            if ((dim1.equals("vendor") && dim2.equals("payment")) || (dim1.equals("payment") && dim2.equals("vendor"))) {
                rawData = analysisMapper.selectMultiDimensionAnalysis(startDate.toString(), endDate.toString());
            } else if ((dim1.equals("airport") && dim2.equals("payment")) || (dim1.equals("payment") && dim2.equals("airport"))) {
                rawData = analysisMapper.selectMultiDimensionAnalysisAirportPayment(startDate.toString(), endDate.toString());
            } else if ((dim1.equals("vendor") && dim2.equals("airport")) || (dim1.equals("airport") && dim2.equals("vendor"))) {
                rawData = analysisMapper.selectMultiDimensionAnalysisVendorAirport(startDate.toString(), endDate.toString());
            } else if ((dim1.equals("vendor") && dim2.equals("hour")) || (dim1.equals("hour") && dim2.equals("vendor"))) {
                rawData = analysisMapper.selectMultiDimensionAnalysisVendorHour(startDate.toString(), endDate.toString());
            } else if ((dim1.equals("vendor") && dim2.equals("distance")) || (dim1.equals("distance") && dim2.equals("vendor"))) {
                rawData = analysisMapper.selectMultiDimensionAnalysisVendorDistance(startDate.toString(), endDate.toString());
            } else if ((dim1.equals("vendor") && dim2.equals("passenger")) || (dim1.equals("passenger") && dim2.equals("vendor"))) {
                rawData = analysisMapper.selectMultiDimensionAnalysisVendorPassenger(startDate.toString(), endDate.toString());
            } else if ((dim1.equals("borough") && dim2.equals("payment")) || (dim1.equals("payment") && dim2.equals("borough"))) {
                rawData = analysisMapper.selectMultiDimensionAnalysisBoroughPayment(startDate.toString(), endDate.toString());
            } else {
                rawData = analysisMapper.selectMultiDimensionAnalysis(startDate.toString(), endDate.toString());
            }

            if (rawData.isEmpty()) {
                log.warn("Multi-dimension analysis query returned empty result for date range {} to {}", startDate, endDate);
                return Collections.emptyList();
            }

            // 使用Set确保维度组合唯一性，key为维度组合的拼接
            Set<String> uniqueDimensionPairs = new HashSet<>();
            List<MultiDimensionAnalysisDTO> results = new ArrayList<>();

            for (Map<String, Object> row : rawData) {
                String outDim1Value = String.valueOf(row.getOrDefault("dim1_value", "")).trim();
                String outDim1Name = String.valueOf(row.getOrDefault("dim1_name", outDim1Value)).trim();
                String outDim2Value = String.valueOf(row.getOrDefault("dim2_value", "")).trim();
                String outDim2Name = String.valueOf(row.getOrDefault("dim2_name", outDim2Value)).trim();

                // 跳过无效数据
                if (outDim1Value.isEmpty() || outDim2Value.isEmpty() || 
                    "null".equalsIgnoreCase(outDim1Value) || "null".equalsIgnoreCase(outDim2Value)) {
                    continue;
                }

                // 根据用户请求的维度顺序决定输出顺序
                boolean swapDimensions = (dim1.equals("payment") && dim2.equals("vendor")) ||
                                         (dim1.equals("airport") && dim2.equals("vendor")) ||
                                         (dim1.equals("payment") && dim2.equals("airport"));

                String finalDim1Value = swapDimensions ? outDim2Value : outDim1Value;
                String finalDim1Name = swapDimensions ? outDim2Name : outDim1Name;
                String finalDim2Value = swapDimensions ? outDim1Value : outDim2Value;
                String finalDim2Name = swapDimensions ? outDim1Name : outDim2Name;

                // 生成唯一键用于去重
                String uniqueKey = finalDim1Value + "|" + finalDim2Value;
                
                if (uniqueDimensionPairs.contains(uniqueKey)) {
                    log.debug("Skipping duplicate dimension pair: {} × {}", finalDim1Value, finalDim2Value);
                    continue;
                }
                uniqueDimensionPairs.add(uniqueKey);

                long tripCount = ((Number) row.getOrDefault("trip_count", 0)).longValue();
                double totalAmount = ((Number) row.getOrDefault("total_amount", 0)).doubleValue();
                double avgAmount = ((Number) row.getOrDefault("avg_amount", 0)).doubleValue();
                double percentage = ((Number) row.getOrDefault("percentage", 0)).doubleValue();

                MultiDimensionAnalysisDTO dto = MultiDimensionAnalysisDTO.builder()
                    .dimension1(finalDim1Value)
                    .dimension1Name(finalDim1Name)
                    .dimension2(finalDim2Value)
                    .dimension2Name(finalDim2Name)
                    .tripCount(tripCount)
                    .totalAmount(Math.round(totalAmount * 100) / 100.0)
                    .avgAmount(Math.round(avgAmount * 100) / 100.0)
                    .percentage(Math.round(percentage * 100) / 100.0)
                    .build();
                results.add(dto);
            }

            log.info("Multi-dimension analysis completed: {} unique dimension combinations found (filtered from {} raw records)", 
                     results.size(), rawData.size());

            return results;
        } catch (Exception e) {
            log.error("Failed to fetch multi-dimension analysis", e);
            return Collections.emptyList();
        }
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
            List<Map<String, Object>> result = analysisMapper.selectCrossTabAnalysis(startDate.toString(), endDate.toString());
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
        Map<String, List<String>> metricDates = new HashMap<>();

        for (Map<String, Object> row : rawData) {
            String metric = String.valueOf(row.get("metric_name"));
            double value = ((Number) row.getOrDefault("value", 0)).doubleValue();
            String date = String.valueOf(row.get("stat_date"));
            
            metricData.computeIfAbsent(metric, k -> new ArrayList<>()).add(value);
            metricDates.computeIfAbsent(metric, k -> new ArrayList<>()).add(date);
        }

        for (Map.Entry<String, List<Double>> entry : metricData.entrySet()) {
            List<Double> values = entry.getValue();
            List<String> dates = metricDates.get(entry.getKey());
            
            if (values.size() < 7) continue;

            double mean = DataMiningUtils.calculateMean(values);
            double stdDev = DataMiningUtils.calculateStandardDeviation(values);

            if (stdDev == 0) continue;

            Map<Integer, DataMiningUtils.AnomalyResult> anomalyResults = DataMiningUtils.detectAnomaliesByZScore(values, 3.0);

            for (Map.Entry<Integer, DataMiningUtils.AnomalyResult> anomalyEntry : anomalyResults.entrySet()) {
                int index = anomalyEntry.getKey();
                DataMiningUtils.AnomalyResult result = anomalyEntry.getValue();

                AnomalyDetectionDTO anomaly = AnomalyDetectionDTO.builder()
                    .metricName(entry.getKey())
                    .metricDisplayName(getMetricDisplayName(entry.getKey()))
                    .anomalyDate(LocalDate.parse(dates.get(index)))
                    .actualValue(result.getValue())
                    .expectedValue(Math.round(mean * 100) / 100.0)
                    .deviationPercent(Math.round(result.getZScore() * 100) / 100.0)
                    .anomalyLevel(result.getSeverity())
                    .anomalyType(result.getValue() > mean ? "spike" : "drop")
                    .description(buildAnomalyDescription(entry.getKey(), result.getValue() > mean ? "spike" : "drop", result.getZScore()))
                    .potentialCauses(getPotentialCauses(entry.getKey(), result.getValue() > mean ? "spike" : "drop"))
                    .rootCauses(generateRootCauses(entry.getKey()))
                    .build();

                anomalies.add(anomaly);
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
                causes.add("促销活动或价格调整");
            } else {
                causes.add("恶劣天气导致出行减少");
                causes.add("公共交通罢工或延误");
                causes.add("疫情或突发事件影响");
                causes.add("竞争对手推出优惠活动");
            }
        } else if ("total_amount".equals(metric)) {
            causes.add("价格调整或促销活动");
            causes.add("高价值订单比例变化");
            causes.add("支付方式分布变化");
            causes.add("服务费或附加费调整");
        } else if ("avg_amount".equals(metric)) {
            causes.add("平均行程距离变化");
            causes.add("价格策略调整");
            causes.add("高峰时段订单比例变化");
            causes.add("服务类型组合变化");
        }

        return causes;
    }

    private List<AnomalyDetectionDTO.RootCauseAnalysis> generateRootCauses(String metric) {
        List<AnomalyDetectionDTO.RootCauseAnalysis> rootCauses = new ArrayList<>();
        List<Map<String, Object>> vendorData = null;

        try {
            vendorData = analysisMapper.selectVendorComparison(LocalDate.now().minusDays(30).toString(), LocalDate.now().toString());
        } catch (Exception e) {
            log.warn("Failed to fetch vendor data for root cause analysis", e);
        }

        if ("trip_count".equals(metric) && vendorData != null && !vendorData.isEmpty()) {
            Map<String, Object> topVendor = vendorData.stream()
                .max(Comparator.comparing(m -> ((Number) m.getOrDefault("market_share", 0)).doubleValue()))
                .orElse(null);

            if (topVendor != null) {
                double contribution = ((Number) topVendor.getOrDefault("market_share", 0)).doubleValue();
                rootCauses.add(AnomalyDetectionDTO.RootCauseAnalysis.builder()
                    .dimension("供应商")
                    .value(String.valueOf(topVendor.get("vendor_name")))
                    .contribution(Double.valueOf(contribution))
                    .impactDescription("该供应商订单量变化对整体影响最大")
                    .build());
            }
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

        List<Double> xValues = new ArrayList<>();
        for (int i = 0; i < historicalValues.size(); i++) {
            xValues.add((double) i);
        }

        LinearRegressionResult regression = DataMiningUtils.performLinearRegression(xValues, historicalValues);
        
        double avgGrowth = regression.getSlope() / (historicalValues.stream().mapToDouble(Double::doubleValue).average().orElse(1)) * 100;

        double variance = DataMiningUtils.calculateVariance(historicalValues);
        double volatility = Math.sqrt(variance) / (historicalValues.stream().mapToDouble(Double::doubleValue).average().orElse(1));
        double predictionVariance = Math.max(0.05, Math.min(0.2, volatility));

        double lastValue = historicalValues.get(historicalValues.size() - 1);
        String lastDateStr = String.valueOf(historicalData.get(historicalData.size() - 1).get("stat_date"));
        LocalDate lastDate = LocalDate.parse(lastDateStr);

        for (int i = 1; i <= days; i++) {
            double predicted = lastValue * Math.pow(1 + avgGrowth / 100, i);
            double lowerBound = predicted * (1 - predictionVariance);
            double upperBound = predicted * (1 + predictionVariance);

            predictions.add(PredictionDTO.builder()
                .date(lastDate.plusDays(i))
                .metricName("trip_count")
                .predictedValue((double) Math.round(predicted))
                .lowerBound((double) Math.round(lowerBound))
                .upperBound((double) Math.round(upperBound))
                .confidence(Math.round((1 - predictionVariance) * 100) / 100.0)
                .trend(Math.round(avgGrowth * 10000) / 100.0)
                .trendDirection(avgGrowth > 0 ? "up" : avgGrowth < 0 ? "down" : "stable")
                .build());
        }

        return predictions;
    }

    private List<BusinessInsightDTO> analyzeAndGenerateInsights(LocalDate startDate, LocalDate endDate) {
        List<BusinessInsightDTO> insights = new ArrayList<>();

        List<Map<String, Object>> vendorData = getVendorComparison(startDate, endDate);
        List<Map<String, Object>> paymentData = getPaymentDistribution(startDate, endDate);
        List<Map<String, Object>> airportData = getAirportStatistics(startDate, endDate);
        List<TrendAnalysisDTO> trendData = getTrendAnalysis(startDate, endDate);
        List<Map<String, Object>> passengerData = getPassengerDistribution(startDate, endDate);
        List<Map<String, Object>> tipData = getTipDistribution(startDate, endDate);

        insights.addAll(analyzeVendorMarketConcentration(vendorData));
        insights.addAll(analyzePaymentBehavior(paymentData));
        insights.addAll(analyzeAirportPerformance(airportData));
        insights.addAll(analyzeBusinessGrowth(trendData));
        insights.addAll(analyzeRevenueQuality(trendData));
        insights.addAll(analyzePassengerBehavior(passengerData));
        insights.addAll(analyzeTipBehavior(tipData));
        insights.addAll(performClusterAnalysis(vendorData, paymentData));

        return insights.stream()
            .sorted((a, b) -> {
                int levelCompare = getLevelPriority(b.getLevel()) - getLevelPriority(a.getLevel());
                if (levelCompare != 0) return levelCompare;
                return Double.compare(b.getImpactScore(), a.getImpactScore());
            })
            .collect(Collectors.toList());
    }

    private List<BusinessInsightDTO> analyzeVendorMarketConcentration(List<Map<String, Object>> vendorData) {
        List<BusinessInsightDTO> insights = new ArrayList<>();

        if (vendorData.isEmpty()) return insights;

        double totalTrips = vendorData.stream()
            .mapToDouble(m -> ((Number) m.getOrDefault("trip_count", 0)).doubleValue())
            .sum();

        if (totalTrips <= 0) return insights;

        Map<String, Object> topVendor = vendorData.stream()
            .max(Comparator.comparing(m -> ((Number) m.getOrDefault("market_share", 0)).doubleValue()))
            .orElse(null);

        if (topVendor == null) return insights;

        double marketShare = ((Number) topVendor.getOrDefault("market_share", 0)).doubleValue();

        if (marketShare > 60) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_VENDOR_001")
                .title("供应商市场集中度较高")
                .category("市场竞争")
                .level("high")
                .description(String.format("%s占据了%.1f%%的市场份额，市场集中度较高",
                    topVendor.get("vendor_name"), marketShare))
                .recommendation("建议关注供应商多样性，降低单一供应商依赖风险。可以考虑引入新供应商或与现有供应商重新协商合作条款。")
                .supportingData(Arrays.asList(
                    String.format("市场份额: %.1f%%", marketShare),
                    String.format("供应商数量: %d家", vendorData.size()),
                    String.format("CR1指数: %.1f%%", marketShare)
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(85.0)
                .build());
        } else if (marketShare < 30) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_VENDOR_001A")
                .title("供应商市场竞争较为均衡")
                .category("市场竞争")
                .level("medium")
                .description(String.format("市场份额最高的供应商仅占%.1f%%，市场竞争较为均衡", marketShare))
                .recommendation("当前市场格局健康，建议保持现有供应商策略，持续关注各供应商绩效表现。")
                .supportingData(Arrays.asList(
                    String.format("市场份额: %.1f%%", marketShare),
                    String.format("供应商数量: %d家", vendorData.size())
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(60.0)
                .build());
        }

        double herfindahlIndex = vendorData.stream()
            .mapToDouble(m -> Math.pow(((Number) m.getOrDefault("market_share", 0)).doubleValue() / 100, 2))
            .sum();
        
        if (herfindahlIndex > 0.25) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_VENDOR_002")
                .title("市场集中度指数偏高")
                .category("市场竞争")
                .level("medium")
                .description(String.format("赫芬达尔指数为%.3f，表明市场存在一定程度的集中", herfindahlIndex))
                .recommendation("建议监控市场动态，评估是否需要引入竞争机制以促进市场健康发展。")
                .supportingData(Arrays.asList(
                    String.format("赫芬达尔指数: %.3f", herfindahlIndex),
                    "指数大于0.25表示高度集中"
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(70.0)
                .build());
        }

        return insights;
    }

    private List<BusinessInsightDTO> analyzePaymentBehavior(List<Map<String, Object>> paymentData) {
        List<BusinessInsightDTO> insights = new ArrayList<>();

        if (paymentData.isEmpty()) return insights;

        double creditCardPercentage = paymentData.stream()
            .filter(p -> "credit_card".equals(p.get("payment_code")))
            .mapToDouble(p -> ((Number) p.getOrDefault("percentage", 0)).doubleValue())
            .findFirst().orElse(0);

        double cashPercentage = paymentData.stream()
            .filter(p -> "cash".equals(p.get("payment_code")))
            .mapToDouble(p -> ((Number) p.getOrDefault("percentage", 0)).doubleValue())
            .findFirst().orElse(0);

        if (creditCardPercentage < 50) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_PAYMENT_001")
                .title("电子支付占比偏低")
                .category("支付行为")
                .level("medium")
                .description(String.format("信用卡支付占比仅%.1f%%，电子支付普及度有待提升", creditCardPercentage))
                .recommendation("建议推广电子支付方式，提供支付优惠激励用户使用非现金支付，提升支付效率和安全性。")
                .supportingData(Arrays.asList(
                    String.format("信用卡支付: %.1f%%", creditCardPercentage),
                    String.format("现金支付: %.1f%%", cashPercentage),
                    "电子支付可降低运营成本约15%"
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(70.0)
                .build());
        } else if (creditCardPercentage > 75) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_PAYMENT_001A")
                .title("电子支付占比领先")
                .category("支付行为")
                .level("medium")
                .description(String.format("信用卡支付占比达%.1f%%，电子支付普及度较高", creditCardPercentage))
                .recommendation("电子支付基础设施完善，建议探索更多数字化支付创新，如扫码支付、数字钱包等。")
                .supportingData(Arrays.asList(
                    String.format("信用卡支付: %.1f%%", creditCardPercentage),
                    "电子支付渗透率高于行业平均水平"
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(65.0)
                .build());
        }

        double unknownPaymentPercentage = paymentData.stream()
            .filter(p -> "unknown".equals(p.get("payment_code")))
            .mapToDouble(p -> ((Number) p.getOrDefault("percentage", 0)).doubleValue())
            .findFirst().orElse(0);

        if (unknownPaymentPercentage > 10) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_PAYMENT_002")
                .title("未知支付方式占比较高")
                .category("支付行为")
                .level("medium")
                .description(String.format("未知支付方式占比达%.1f%%，支付数据完整性有待提升", unknownPaymentPercentage))
                .recommendation("建议加强支付数据采集和清洗，确保支付方式分类准确，以便更好地分析用户支付偏好。")
                .supportingData(Arrays.asList(
                    String.format("未知支付占比: %.1f%%", unknownPaymentPercentage),
                    "建议阈值: <5%"
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(60.0)
                .build());
        }

        return insights;
    }

    private List<BusinessInsightDTO> analyzeAirportPerformance(List<Map<String, Object>> airportData) {
        List<BusinessInsightDTO> insights = new ArrayList<>();

        if (airportData.isEmpty()) return insights;

        // 过滤掉airport_code为null或空的无效数据
        List<Map<String, Object>> validAirportData = airportData.stream()
            .filter(m -> m.get("airport_code") != null && !"null".equalsIgnoreCase(String.valueOf(m.get("airport_code"))))
            .collect(Collectors.toList());

        if (validAirportData.isEmpty()) return insights;

        double totalAirportTrips = validAirportData.stream()
            .mapToDouble(m -> ((Number) m.getOrDefault("trip_count", 0)).doubleValue())
            .sum();

        if (totalAirportTrips <= 0) return insights;

        Map<String, Object> topAirport = validAirportData.stream()
            .max(Comparator.comparing(m -> ((Number) m.getOrDefault("trip_count", 0)).doubleValue()))
            .orElse(null);

        if (topAirport != null) {
            long topTrips = ((Number) topAirport.getOrDefault("trip_count", 0)).longValue();
            double percentage = topTrips * 100.0 / totalAirportTrips;
            // 使用正确的字段名 airport_name 和 airport_code
            String airportName = String.valueOf(topAirport.getOrDefault("airport_name", topAirport.get("airport_code")));

            if (percentage > 40) {
                insights.add(BusinessInsightDTO.builder()
                    .insightId("INSIGHT_AIRPORT_001")
                    .title(String.format("%s订单占比领先", airportName))
                    .category("交通枢纽")
                    .level("medium")
                    .description(String.format("%s贡献了%.1f%%的订单，占比领先",
                        airportName, percentage))
                    .recommendation("考虑在该出行类型上增加运力配置，优化服务质量，提升乘客满意度。")
                    .supportingData(Arrays.asList(
                        String.format("%s订单占比: %.1f%%", airportName, percentage),
                        String.format("订单总量: %.0f", totalAirportTrips)
                    ))
                    .discoveryDate(LocalDate.now())
                    .impactScore(65.0)
                    .build());
            }
        }

        Map<String, Object> bottomAirport = validAirportData.stream()
            .min(Comparator.comparing(m -> ((Number) m.getOrDefault("trip_count", 0)).doubleValue()))
            .orElse(null);

        if (bottomAirport != null && topAirport != null && !bottomAirport.equals(topAirport)) {
            double topAmount = ((Number) topAirport.getOrDefault("trip_count", 0)).doubleValue();
            double bottomAmount = ((Number) bottomAirport.getOrDefault("trip_count", 0)).doubleValue();
            double ratio = topAmount > 0 ? bottomAmount / topAmount : 0;
            // 使用正确的字段名
            String topAirportName = String.valueOf(topAirport.getOrDefault("airport_name", topAirport.get("airport_code")));
            String bottomAirportName = String.valueOf(bottomAirport.getOrDefault("airport_name", bottomAirport.get("airport_code")));

            if (ratio < 0.3) {
                insights.add(BusinessInsightDTO.builder()
                    .insightId("INSIGHT_AIRPORT_002")
                    .title("出行类型订单量差异较大")
                    .category("交通枢纽")
                    .level("medium")
                    .description(String.format("%s与%s订单量差距明显，后者仅为前者的%.0f%%",
                        bottomAirportName, topAirportName, ratio * 100))
                    .recommendation("建议分析订单量较低出行类型的服务质量、价格竞争力等因素，制定针对性提升策略。")
                    .supportingData(Arrays.asList(
                        String.format("%s订单: %.0f", topAirportName, topAmount),
                        String.format("%s订单: %.0f", bottomAirportName, bottomAmount)
                    ))
                    .discoveryDate(LocalDate.now())
                    .impactScore(55.0)
                    .build());
            }
        }

        return insights;
    }

    private List<BusinessInsightDTO> analyzeBusinessGrowth(List<TrendAnalysisDTO> trendData) {
        List<BusinessInsightDTO> insights = new ArrayList<>();

        if (trendData.isEmpty()) return insights;

        List<Double> growthRates = trendData.stream()
            .filter(t -> t.getGrowthRate() != null)
            .map(TrendAnalysisDTO::getGrowthRate)
            .collect(Collectors.toList());

        if (growthRates.isEmpty()) return insights;

        double avgGrowthRate = growthRates.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        if (avgGrowthRate > 10) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_GROWTH_001")
                .title("订单量高速增长")
                .category("业务增长")
                .level("high")
                .description(String.format("订单量平均增长率达%.1f%%，业务发展势头强劲", avgGrowthRate))
                .recommendation("业务增长迅速，需关注运营能力匹配，确保服务质量不随扩张下降。建议提前规划运力和人员配置。")
                .supportingData(Arrays.asList(
                    String.format("平均增长率: %.1f%%", avgGrowthRate),
                    String.format("数据周期: %d天", trendData.size()),
                    String.format("期末订单量: %.0f", trendData.get(trendData.size() - 1).getTripCount())
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(80.0)
                .build());
        } else if (avgGrowthRate < -5) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_GROWTH_001A")
                .title("订单量持续下滑")
                .category("业务增长")
                .level("high")
                .description(String.format("订单量平均下降%.1f%%，需关注业务健康状况", Math.abs(avgGrowthRate)))
                .recommendation("建议立即分析下降原因，检查市场环境变化、竞争对手动态，制定应对策略。")
                .supportingData(Arrays.asList(
                    String.format("平均下降率: %.1f%%", Math.abs(avgGrowthRate)),
                    String.format("数据周期: %d天", trendData.size())
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(85.0)
                .build());
        }

        double growthVariance = DataMiningUtils.calculateVariance(growthRates);
        if (growthVariance > 100) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_GROWTH_002")
                .title("增长率波动较大")
                .category("业务增长")
                .level("medium")
                .description(String.format("增长率方差为%.1f，业务增长稳定性有待提升", growthVariance))
                .recommendation("建议分析波动原因，识别影响业务增长的关键因素，制定稳定增长策略。")
                .supportingData(Arrays.asList(
                    String.format("增长率方差: %.1f", growthVariance),
                    "方差值越大表示波动越剧烈"
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(60.0)
                .build());
        }

        return insights;
    }

    private List<BusinessInsightDTO> analyzeRevenueQuality(List<TrendAnalysisDTO> trendData) {
        List<BusinessInsightDTO> insights = new ArrayList<>();

        if (trendData.isEmpty()) return insights;

        List<Double> amounts = trendData.stream()
            .filter(t -> t.getAvgAmount() > 0)
            .map(TrendAnalysisDTO::getAvgAmount)
            .collect(Collectors.toList());

        if (amounts.isEmpty()) return insights;

        double avgAmount = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        if (avgAmount > 50) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_REVENUE_001")
                .title("平均订单金额较高")
                .category("收入分析")
                .level("medium")
                .description(String.format("平均订单金额达$%.2f，盈利能力较强", avgAmount))
                .recommendation("高价值订单占比较大，建议加强高端客户服务体验，维护品牌形象。")
                .supportingData(Arrays.asList(
                    String.format("平均订单金额: $%.2f", avgAmount),
                    "高于行业平均水平"
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(70.0)
                .build());
        } else if (avgAmount < 15) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_REVENUE_001A")
                .title("平均订单金额偏低")
                .category("收入分析")
                .level("medium")
                .description(String.format("平均订单金额仅$%.2f，盈利能力有待提升", avgAmount))
                .recommendation("建议推出增值服务套餐，优化定价策略，提升客单价。")
                .supportingData(Arrays.asList(
                    String.format("平均订单金额: $%.2f", avgAmount),
                    "低于行业平均水平"
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(65.0)
                .build());
        }

        List<Double> distances = trendData.stream()
            .filter(t -> t.getAvgDistance() > 0)
            .map(TrendAnalysisDTO::getAvgDistance)
            .collect(Collectors.toList());

        if (!distances.isEmpty()) {
            double avgDistance = distances.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double correlation = DataMiningUtils.calculateCorrelation(amounts, distances);
            
            if (correlation > 0.7) {
                insights.add(BusinessInsightDTO.builder()
                    .insightId("INSIGHT_REVENUE_002")
                    .title("订单金额与距离高度相关")
                    .category("收入分析")
                    .level("medium")
                    .description(String.format("订单金额与行程距离相关系数达%.2f，呈高度正相关", correlation))
                    .recommendation("长距离订单贡献较高收入，建议优化长距离订单的服务体验和定价策略。")
                    .supportingData(Arrays.asList(
                        String.format("相关系数: %.2f", correlation),
                        "相关系数>0.7表示高度相关"
                    ))
                    .discoveryDate(LocalDate.now())
                    .impactScore(60.0)
                    .build());
            }
        }

        return insights;
    }

    private List<BusinessInsightDTO> analyzePassengerBehavior(List<Map<String, Object>> passengerData) {
        List<BusinessInsightDTO> insights = new ArrayList<>();

        if (passengerData.isEmpty()) return insights;

        Map<String, Object> mostCommonPassenger = passengerData.stream()
            .max(Comparator.comparing(m -> ((Number) m.getOrDefault("trip_count", 0)).longValue()))
            .orElse(null);

        if (mostCommonPassenger != null) {
            String passengerRange = String.valueOf(mostCommonPassenger.get("passenger_range"));
            double percentage = ((Number) mostCommonPassenger.getOrDefault("percentage", 0)).doubleValue();

            if (percentage > 60) {
                insights.add(BusinessInsightDTO.builder()
                    .insightId("INSIGHT_PASSENGER_001")
                    .title(String.format("%s乘客订单占比极高", passengerRange))
                    .category("乘客分析")
                    .level("medium")
                    .description(String.format("%s乘客的订单占比达%.1f%%，是最主要的乘客群体", passengerRange, percentage))
                    .recommendation("建议针对该乘客群体优化服务，如提供适合多人乘坐的车型选择或优惠套餐。")
                    .supportingData(Arrays.asList(
                        String.format("占比最高乘客群体: %s", passengerRange),
                        String.format("占比: %.1f%%", percentage)
                    ))
                    .discoveryDate(LocalDate.now())
                    .impactScore(55.0)
                    .build());
            }
        }

        return insights;
    }

    private List<BusinessInsightDTO> analyzeTipBehavior(List<Map<String, Object>> tipData) {
        List<BusinessInsightDTO> insights = new ArrayList<>();

        if (tipData.isEmpty()) return insights;

        double avgTipRate = tipData.stream()
            .mapToDouble(m -> ((Number) m.getOrDefault("avg_tip_rate", 0)).doubleValue())
            .average().orElse(0);

        if (avgTipRate < 10) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_TIP_001")
                .title("平均小费比率偏低")
                .category("小费分析")
                .level("medium")
                .description(String.format("平均小费比率仅%.1f%%，低于行业平均水平", avgTipRate))
                .recommendation("建议提升服务质量，优化乘客体验，可考虑设置小费引导提示。")
                .supportingData(Arrays.asList(
                    String.format("平均小费比率: %.1f%%", avgTipRate),
                    "行业平均水平: 15-20%"
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(60.0)
                .build());
        } else if (avgTipRate > 20) {
            insights.add(BusinessInsightDTO.builder()
                .insightId("INSIGHT_TIP_001A")
                .title("平均小费比率较高")
                .category("小费分析")
                .level("medium")
                .description(String.format("平均小费比率达%.1f%%，高于行业平均水平", avgTipRate))
                .recommendation("服务质量获得乘客认可，建议继续保持服务标准，可考虑推出会员奖励计划。")
                .supportingData(Arrays.asList(
                    String.format("平均小费比率: %.1f%%", avgTipRate),
                    "高于行业平均水平"
                ))
                .discoveryDate(LocalDate.now())
                .impactScore(55.0)
                .build());
        }

        return insights;
    }

    private List<BusinessInsightDTO> performClusterAnalysis(List<Map<String, Object>> vendorData, List<Map<String, Object>> paymentData) {
        List<BusinessInsightDTO> insights = new ArrayList<>();

        if (vendorData.size() < 2 || paymentData.size() < 2) return insights;

        try {
            List<DataPoint<String>> dataPoints = new ArrayList<>();
            
            for (Map<String, Object> vendor : vendorData) {
                double tripCount = ((Number) vendor.getOrDefault("trip_count", 0)).doubleValue();
                double totalAmount = ((Number) vendor.getOrDefault("total_amount", 0)).doubleValue();
                double marketShare = ((Number) vendor.getOrDefault("market_share", 0)).doubleValue();
                
                dataPoints.add(new DataPoint<>(String.valueOf(vendor.get("vendor_name")), 
                    new double[]{tripCount, totalAmount, marketShare}));
            }

            List<Cluster<String>> clusters = DataMiningUtils.kMeansClustering(dataPoints, 2, 100);

            if (clusters.size() == 2) {
                Cluster<String> cluster1 = clusters.get(0);
                Cluster<String> cluster2 = clusters.get(1);

                int cluster1Size = cluster1.getPoints().size();
                int cluster2Size = cluster2.getPoints().size();

                if (Math.abs(cluster1Size - cluster2Size) > 1) {
                    double[] centroid1 = cluster1.getCentroid();
                    double[] centroid2 = cluster2.getCentroid();

                    String cluster1Desc = centroid1[1] > centroid2[1] ? "高价值" : "普通";
                    String cluster2Desc = centroid1[1] <= centroid2[1] ? "高价值" : "普通";

                    insights.add(BusinessInsightDTO.builder()
                        .insightId("INSIGHT_CLUSTER_001")
                        .title("供应商聚类分析结果")
                        .category("市场竞争")
                        .level("medium")
                        .description(String.format("供应商可分为两类：%d家%s供应商和%d家%s供应商",
                            cluster1Size, cluster1Desc, cluster2Size, cluster2Desc))
                        .recommendation("建议针对不同类别的供应商制定差异化合作策略，优化资源配置。")
                        .supportingData(Arrays.asList(
                            String.format("%s供应商: %d家", cluster1Desc, cluster1Size),
                            String.format("%s供应商: %d家", cluster2Desc, cluster2Size),
                            String.format("%s供应商平均收入: $%.0f", cluster1Desc, centroid1[1]),
                            String.format("%s供应商平均收入: $%.0f", cluster2Desc, centroid2[1])
                        ))
                        .discoveryDate(LocalDate.now())
                        .impactScore(55.0)
                        .build());
                }
            }
        } catch (Exception e) {
            log.warn("Cluster analysis failed", e);
        }

        return insights;
    }

    @Override
    public List<Map<String, Object>> getBoroughRevenue(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching borough revenue for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectBoroughRevenue(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch borough revenue", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getPickupHotspots(LocalDate startDate, LocalDate endDate, int limit) {
        log.debug("Fetching pickup hotspots for {} to {}, limit={}", startDate, endDate, limit);
        try {
            List<Map<String, Object>> result = analysisMapper.selectPickupHotspots(startDate.toString(), endDate.toString());
            if (result.size() > limit) {
                return result.subList(0, limit);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch pickup hotspots", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getDropoffHotspots(LocalDate startDate, LocalDate endDate, int limit) {
        log.debug("Fetching dropoff hotspots for {} to {}, limit={}", startDate, endDate, limit);
        try {
            List<Map<String, Object>> result = analysisMapper.selectDropoffHotspots(startDate.toString(), endDate.toString());
            if (result.size() > limit) {
                return result.subList(0, limit);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch dropoff hotspots", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getTaxiTypeFee(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching taxi type fee for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectTaxiTypeFee(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch taxi type fee", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getVendorPaymentCross(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching vendor-payment cross analysis for {} to {}", startDate, endDate);
        try {
            String start = startDate != null ? startDate.toString() : null;
            String end = endDate != null ? endDate.toString() : null;
            return analysisMapper.selectVendorPaymentCross(start, end);
        } catch (Exception e) {
            log.error("Failed to fetch vendor-payment cross analysis", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getAirportTimeCross(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching airport-time cross analysis for {} to {}", startDate, endDate);
        try {
            String start = startDate != null ? startDate.toString() : null;
            String end = endDate != null ? endDate.toString() : null;
            return analysisMapper.selectAirportTimeCross(start, end);
        } catch (Exception e) {
            log.error("Failed to fetch airport-time cross analysis", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getBoroughPaymentCross(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching borough-payment cross analysis for {} to {}", startDate, endDate);
        try {
            String start = startDate != null ? startDate.toString() : null;
            String end = endDate != null ? endDate.toString() : null;
            return analysisMapper.selectBoroughPaymentCross(start, end);
        } catch (Exception e) {
            log.error("Failed to fetch borough-payment cross analysis", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getVendorTaxiTypeCross(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching vendor-taxitype cross analysis for {} to {}", startDate, endDate);
        try {
            String start = startDate != null ? startDate.toString() : null;
            String end = endDate != null ? endDate.toString() : null;
            return analysisMapper.selectVendorTaxiTypeCross(start, end);
        } catch (Exception e) {
            log.error("Failed to fetch vendor-taxitype cross analysis", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getAirportBoroughCross(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching airport-borough cross analysis for {} to {}", startDate, endDate);
        try {
            String start = startDate != null ? startDate.toString() : null;
            String end = endDate != null ? endDate.toString() : null;
            return analysisMapper.selectAirportBoroughCross(start, end);
        } catch (Exception e) {
            log.error("Failed to fetch airport-borough cross analysis", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getTimePaymentCross(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching time-payment cross analysis for {} to {}", startDate, endDate);
        try {
            String start = startDate != null ? startDate.toString() : null;
            String end = endDate != null ? endDate.toString() : null;
            return analysisMapper.selectTimePaymentCross(start, end);
        } catch (Exception e) {
            log.error("Failed to fetch time-payment cross analysis", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getDistancePaymentCross(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching distance-payment cross analysis for {} to {}", startDate, endDate);
        try {
            String start = startDate != null ? startDate.toString() : null;
            String end = endDate != null ? endDate.toString() : null;
            return analysisMapper.selectDistancePaymentCross(start, end);
        } catch (Exception e) {
            log.error("Failed to fetch distance-payment cross analysis", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getWeekdayTimeCross(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching weekday-time cross analysis for {} to {}", startDate, endDate);
        try {
            String start = startDate != null ? startDate.toString() : null;
            String end = endDate != null ? endDate.toString() : null;
            return analysisMapper.selectWeekdayTimeCross(start, end);
        } catch (Exception e) {
            log.error("Failed to fetch weekday-time cross analysis", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getTaxiTypeFeeCross(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching taxitype-fee cross analysis for {} to {}", startDate, endDate);
        try {
            String start = startDate != null ? startDate.toString() : null;
            String end = endDate != null ? endDate.toString() : null;
            return analysisMapper.selectTaxiTypeFeeCross(start, end);
        } catch (Exception e) {
            log.error("Failed to fetch taxitype-fee cross analysis", e);
            return Collections.emptyList();
        }
    }
}