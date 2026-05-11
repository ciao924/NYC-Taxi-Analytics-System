package com.taxi.analytics.modules.quality.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualitySummaryDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Double overallScore;
    private Double completenessScore;
    private Double accuracyScore;
    private Double uniquenessScore;
    private Double timelinessScore;
}