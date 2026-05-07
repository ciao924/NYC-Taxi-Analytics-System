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
}