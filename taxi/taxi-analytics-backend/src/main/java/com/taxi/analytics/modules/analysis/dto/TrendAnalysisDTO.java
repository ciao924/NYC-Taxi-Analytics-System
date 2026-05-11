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
public class TrendAnalysisDTO {

    private LocalDate date;
    private Long tripCount;
    private Double totalAmount;
    private Double avgAmount;
    private Double avgDistance;
    private Long passengerCount;
    private Double growthRate;
    private Double movingAverage;
}

