package com.taxi.analytics.modules.analysis.service;

import com.taxi.analytics.modules.analysis.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AnalysisService {

    List<Map<String, Object>> getAirportStatistics(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getAirportDetailedStatistics(LocalDate startDate, LocalDate endDate, String airportCode);

    List<Map<String, Object>> getVendorComparison(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getVendorTrend(LocalDate startDate, LocalDate endDate, String vendorId);

    List<Map<String, Object>> getPaymentDistribution(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getPaymentTrend(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getDistanceDistribution(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getDurationDistribution(LocalDate startDate, LocalDate endDate);

    List<MultiDimensionAnalysisDTO> getMultiDimensionAnalysis(LocalDate startDate, LocalDate endDate, String dimension1, String dimension2);

    List<AnomalyDetectionDTO> detectAnomalies(LocalDate startDate, LocalDate endDate);

    List<PredictionDTO> getPredictions(LocalDate startDate, LocalDate endDate, int days);

    List<BusinessInsightDTO> generateBusinessInsights(LocalDate startDate, LocalDate endDate);

    List<TrendAnalysisDTO> getTrendAnalysis(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getCrossTabAnalysis(LocalDate startDate, LocalDate endDate, String rowDimension, String colDimension);

    Map<String, Object> getKpiSummary(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getPassengerDistribution(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getTipDistribution(LocalDate startDate, LocalDate endDate);
}
