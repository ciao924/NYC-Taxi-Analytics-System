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
public class BusinessInsightDTO {

    private String insightId;
    private String title;
    private String category;
    private String level;
    private String description;
    private String recommendation;
    private List<String> supportingData;
    private LocalDate discoveryDate;
    private Double impactScore;
}

