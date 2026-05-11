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
public class HotspotDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String zoneId;
    private String zoneName;
    private Long tripCount;
    private Integer rank;
    private String type;
    private Double latitude;
    private Double longitude;
}