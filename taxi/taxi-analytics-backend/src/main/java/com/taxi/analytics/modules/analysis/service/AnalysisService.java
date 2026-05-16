package com.taxi.analytics.modules.analysis.service;

import com.taxi.analytics.modules.analysis.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AnalysisService {

    List<Map<String, Object>> getKpiDailyTrend(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getAirportStatistics(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getAirportDetailedStatistics(LocalDate startDate, LocalDate endDate, String airportCode);

    List<Map<String, Object>> getAirportTrend(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getVendorComparison(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getVendorTrend(LocalDate startDate, LocalDate endDate, String vendorName);

    List<Map<String, Object>> getPaymentDistribution(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getPaymentTrend(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getDistanceDistribution(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getDurationDistribution(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getPassengerDistribution(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getTipDistribution(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getHourlyDistribution(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getWeekdayAnalysis(LocalDate startDate, LocalDate endDate);

    List<MultiDimensionAnalysisDTO> getMultiDimensionAnalysis(LocalDate startDate, LocalDate endDate, String dimension1, String dimension2);

    List<AnomalyDetectionDTO> detectAnomalies(LocalDate startDate, LocalDate endDate);

    List<PredictionDTO> getPredictions(LocalDate startDate, LocalDate endDate, int days);

    List<BusinessInsightDTO> generateBusinessInsights(LocalDate startDate, LocalDate endDate);

    List<TrendAnalysisDTO> getTrendAnalysis(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getCrossTabAnalysis(LocalDate startDate, LocalDate endDate, String rowDimension, String colDimension);

    List<Map<String, Object>> getBoroughFlow(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getRevenueContribution(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getFeeComposition(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getBoroughRevenue(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getPickupHotspots(LocalDate startDate, LocalDate endDate, int limit);

    List<Map<String, Object>> getDropoffHotspots(LocalDate startDate, LocalDate endDate, int limit);

    List<Map<String, Object>> getTaxiTypeFee(LocalDate startDate, LocalDate endDate);

    Map<String, Object> getKpiSummary(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getVendorPaymentCross(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getAirportTimeCross(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getBoroughPaymentCross(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getVendorTaxiTypeCross(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getAirportBoroughCross(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getTimePaymentCross(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getDistancePaymentCross(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getWeekdayTimeCross(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> getTaxiTypeFeeCross(LocalDate startDate, LocalDate endDate);
}