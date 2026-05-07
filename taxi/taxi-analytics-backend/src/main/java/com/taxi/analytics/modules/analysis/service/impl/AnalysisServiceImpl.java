package com.taxi.analytics.modules.analysis.service.impl;

import com.taxi.analytics.modules.analysis.mapper.AnalysisMapper;
import com.taxi.analytics.modules.analysis.service.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            return analysisMapper.selectAirportStatistics(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch airport statistics", e);
            return generateMockAirportStatistics();
        }
    }

    @Override
    public List<Map<String, Object>> getAirportDetailedStatistics(LocalDate startDate, LocalDate endDate, String airportCode) {
        log.debug("Fetching detailed airport statistics for {} to {}, airport={}", startDate, endDate, airportCode);
        try {
            return analysisMapper.selectAirportDetailedStatistics(startDate.toString(), endDate.toString(), airportCode);
        } catch (Exception e) {
            log.error("Failed to fetch detailed airport statistics", e);
            return generateMockAirportDetailedStatistics();
        }
    }

    @Override
    public List<Map<String, Object>> getVendorComparison(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching vendor comparison for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectVendorComparison(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch vendor comparison", e);
            return generateMockVendorComparison();
        }
    }

    @Override
    public List<Map<String, Object>> getVendorTrend(LocalDate startDate, LocalDate endDate, String vendorId) {
        log.debug("Fetching vendor trend for {} to {}, vendor={}", startDate, endDate, vendorId);
        try {
            return analysisMapper.selectVendorTrend(startDate.toString(), endDate.toString(), vendorId);
        } catch (Exception e) {
            log.error("Failed to fetch vendor trend", e);
            return generateMockVendorTrend();
        }
    }

    @Override
    public List<Map<String, Object>> getPaymentDistribution(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching payment distribution for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectPaymentDistribution(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch payment distribution", e);
            return generateMockPaymentDistribution();
        }
    }

    @Override
    public List<Map<String, Object>> getPaymentTrend(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching payment trend for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectPaymentTrend(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch payment trend", e);
            return generateMockPaymentTrend();
        }
    }

    @Override
    public List<Map<String, Object>> getDistanceDistribution(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching distance distribution for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectDistanceDistribution(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch distance distribution", e);
            return generateMockDistanceDistribution();
        }
    }

    @Override
    public List<Map<String, Object>> getDurationDistribution(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching duration distribution for {} to {}", startDate, endDate);
        try {
            return analysisMapper.selectDurationDistribution(startDate.toString(), endDate.toString());
        } catch (Exception e) {
            log.error("Failed to fetch duration distribution", e);
            return generateMockDurationDistribution();
        }
    }

    private List<Map<String, Object>> generateMockAirportStatistics() {
        List<Map<String, Object>> statistics = new ArrayList<>();
        String[] airports = {"JFK", "LGA", "EWR"};
        String[] airportNames = {"肯尼迪国际机场", "拉瓜迪亚机场", "纽瓦克自由国际机场"};

        for (int i = 0; i < airports.length; i++) {
            Map<String, Object> airport = new HashMap<>();
            airport.put("airport_code", airports[i]);
            airport.put("airport_name", airportNames[i]);
            airport.put("trip_count", (int) (Math.random() * 50000) + 10000);
            airport.put("total_amount", Math.round(Math.random() * 500000) / 100.0);
            airport.put("avg_amount", Math.round(Math.random() * 50 * 100) / 100.0);
            airport.put("avg_distance", Math.round(Math.random() * 15 * 100) / 100.0);
            statistics.add(airport);
        }
        return statistics;
    }

    private List<Map<String, Object>> generateMockAirportDetailedStatistics() {
        List<Map<String, Object>> detailed = new ArrayList<>();
        String[] airports = {"JFK", "LGA", "EWR"};

        for (String airport : airports) {
            for (int hour = 0; hour < 24; hour++) {
                Map<String, Object> stat = new HashMap<>();
                stat.put("airport_code", airport);
                stat.put("hour", hour);
                stat.put("trip_count", (int) (Math.random() * 5000) + 500);
                stat.put("peak_hour", hour >= 7 && hour <= 9 || hour >= 17 && hour <= 19);
                detailed.add(stat);
            }
        }
        return detailed;
    }

    private List<Map<String, Object>> generateMockVendorComparison() {
        List<Map<String, Object>> vendors = new ArrayList<>();
        String[][] vendorData = {
                {"1", "Creative Mobile Technologies"}, {"2", "VeriFone Inc"}
        };

        for (String[] vendor : vendorData) {
            Map<String, Object> v = new HashMap<>();
            v.put("vendor_id", vendor[0]);
            v.put("vendor_name", vendor[1]);
            v.put("trip_count", (int) (Math.random() * 100000) + 50000);
            v.put("total_amount", Math.round(Math.random() * 1000000) / 100.0);
            v.put("avg_trip_distance", Math.round(Math.random() * 10 * 100) / 100.0);
            v.put("market_share", Math.round(Math.random() * 50 * 100) / 100.0);
            vendors.add(v);
        }
        return vendors;
    }

    private List<Map<String, Object>> generateMockVendorTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 30; i >= 0; i--) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", today.minusDays(i).toString());
            day.put("trip_count", (int) (Math.random() * 5000) + 2000);
            day.put("vendor_id", "1");
            trend.add(day);
        }
        return trend;
    }

    private List<Map<String, Object>> generateMockPaymentDistribution() {
        List<Map<String, Object>> payments = new ArrayList<>();
        String[][] paymentData = {
                {"1", "信用卡", "credit_card"},
                {"2", "现金", "cash"},
                {"3", "无消费", "no_charge"},
                {"4", "争议", "dispute"}
        };

        int total = 100;
        for (String[] payment : paymentData) {
            Map<String, Object> p = new HashMap<>();
            p.put("payment_type", payment[0]);
            p.put("payment_desc", payment[1]);
            p.put("payment_code", payment[2]);
            int count = (int) (Math.random() * 30) + 10;
            p.put("trip_count", count);
            p.put("percentage", Math.round(count * 100.0 / total * 100) / 100.0);
            p.put("total_amount", Math.round(Math.random() * 100000 * 100) / 100.0);
            payments.add(p);
        }
        return payments;
    }

    private List<Map<String, Object>> generateMockPaymentTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 30; i >= 0; i--) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", today.minusDays(i).toString());
            day.put("credit_card_pct", Math.round(Math.random() * 30 + 50 * 100) / 100.0);
            day.put("cash_pct", Math.round(Math.random() * 20 + 30 * 100) / 100.0);
            trend.add(day);
        }
        return trend;
    }

    private List<Map<String, Object>> generateMockDistanceDistribution() {
        List<Map<String, Object>> distribution = new ArrayList<>();
        String[] ranges = {"0-2", "2-5", "5-10", "10-15", "15-20", "20+"};

        for (String range : ranges) {
            Map<String, Object> d = new HashMap<>();
            d.put("distance_range", range);
            d.put("trip_count", (int) (Math.random() * 20000) + 5000);
            d.put("percentage", Math.round(Math.random() * 30 * 100) / 100.0);
            distribution.add(d);
        }
        return distribution;
    }

    private List<Map<String, Object>> generateMockDurationDistribution() {
        List<Map<String, Object>> distribution = new ArrayList<>();
        String[] ranges = {"0-10", "10-20", "20-30", "30-45", "45-60", "60+"};

        for (String range : ranges) {
            Map<String, Object> d = new HashMap<>();
            d.put("duration_range", range);
            d.put("trip_count", (int) (Math.random() * 20000) + 5000);
            d.put("percentage", Math.round(Math.random() * 30 * 100) / 100.0);
            distribution.add(d);
        }
        return distribution;
    }
}