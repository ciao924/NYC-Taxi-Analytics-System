package com.taxi.analytics.modules.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirportStatisticsDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String airportCode;
    private String airportName;
    private Long tripCount;
    private Double avgFare;
    private Double avgDistance;
    private Integer peakHour;
}