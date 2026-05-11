package com.taxi.analytics.modules.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiDimensionAnalysisDTO {

    private String dimension1;
    private String dimension1Name;
    private String dimension2;
    private String dimension2Name;
    private Long tripCount;
    private Double totalAmount;
    private Double avgAmount;
    private Double avgDistance;
    private Double percentage;
    private LocalDate statDate;
}

