package com.taxi.analytics.modules.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendDataDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String statDate;
    private Long totalTrips;
    private Double totalRevenue;
    private Double avgFare;
}