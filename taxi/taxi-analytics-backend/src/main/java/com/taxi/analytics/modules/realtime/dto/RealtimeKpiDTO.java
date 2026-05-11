package com.taxi.analytics.modules.realtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeKpiDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long orderCount;
    private Double totalFare;
    private Double avgFare;
    private Long windowStart;
    private Long windowEnd;
}