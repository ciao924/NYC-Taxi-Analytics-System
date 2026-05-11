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
public class VendorComparisonDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String vendorId;
    private String vendorName;
    private Long tripCount;
    private Double totalRevenue;
    private Double avgFare;
    private Double marketShare;
}