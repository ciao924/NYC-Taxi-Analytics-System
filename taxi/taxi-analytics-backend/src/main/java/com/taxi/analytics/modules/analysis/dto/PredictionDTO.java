package com.taxi.analytics.modules.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionDTO {

    private LocalDate date;
    private String metricName;
    private Double predictedValue;
    private Double lowerBound;
    private Double upperBound;
    private Double confidence;
    private Double trend;
    private String trendDirection;
}

