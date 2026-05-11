package com.taxi.analytics.modules.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetectionDTO {

    private String metricName;
    private String metricDisplayName;
    private LocalDate anomalyDate;
    private Double actualValue;
    private Double expectedValue;
    private Double deviationPercent;
    private String anomalyLevel;
    private String anomalyType;
    private String description;
    private List<String> potentialCauses;
    private List<RootCauseAnalysis> rootCauses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RootCauseAnalysis {
        private String dimension;
        private String value;
        private Double contribution;
        private String impactDescription;
    }
}

