package com.taxi.analytics.modules.analysis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisMapper {

    List<Map<String, Object>> selectKpiDailyTrend(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectAirportStatistics(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectAirportTrend(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectVendorComparison(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectVendorTrend(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("vendorName") String vendorName);

    List<Map<String, Object>> selectPaymentDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectPaymentTrend(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectDistanceDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectDurationDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectPassengerDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectTipDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectHourlyDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectWeekdayAnalysis(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectMultiDimensionAnalysis(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectMultiDimensionAnalysisAirportPayment(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectMultiDimensionAnalysisVendorAirport(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectMultiDimensionAnalysisVendorHour(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectMultiDimensionAnalysisVendorDistance(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectMultiDimensionAnalysisVendorPassenger(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectMultiDimensionAnalysisBoroughPayment(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectAnomalyData(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectHistoricalTrend(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectCrossTabAnalysis(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectBoroughFlow(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectRevenueContribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectFeeComposition(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectAirportDetailedStatistics(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("airportCode") String airportCode);

    List<Map<String, Object>> selectTrendAnalysis(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectBoroughRevenue(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectPickupHotspots(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectDropoffHotspots(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectTaxiTypeFee(@Param("startDate") String startDate, @Param("endDate") String endDate);

    Map<String, Object> selectKpiSummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectVendorPaymentCross(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectAirportTimeCross(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectBoroughPaymentCross(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectVendorTaxiTypeCross(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectAirportBoroughCross(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectTimePaymentCross(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectDistancePaymentCross(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectWeekdayTimeCross(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectTaxiTypeFeeCross(@Param("startDate") String startDate, @Param("endDate") String endDate);
}