package com.taxi.analytics.modules.analysis.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisMapper {

    List<Map<String, Object>> selectAirportStatistics(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectAirportDetailedStatistics(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("airportCode") String airportCode);

    List<Map<String, Object>> selectVendorComparison(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectVendorTrend(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("vendorId") String vendorId);

    List<Map<String, Object>> selectPaymentDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectPaymentTrend(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectDistanceDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectDurationDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectMultiDimensionAnalysis(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("dimension1") String dimension1, @Param("dimension2") String dimension2);
    
    List<Map<String, Object>> selectMultiDimensionAnalysisVendorAirport(@Param("startDate") String startDate, @Param("endDate") String endDate);
    
    List<Map<String, Object>> selectMultiDimensionAnalysisAirportPayment(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectAnomalyData(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectHistoricalTrend(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectTrendAnalysis(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectCrossTabAnalysis(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("rowDimension") String rowDimension, @Param("colDimension") String colDimension);

    Map<String, Object> selectKpiSummary(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectPassengerDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectTipDistribution(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
