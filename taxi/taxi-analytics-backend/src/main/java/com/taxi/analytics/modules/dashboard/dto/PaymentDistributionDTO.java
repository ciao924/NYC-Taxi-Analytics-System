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
public class PaymentDistributionDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String paymentType;
    private String paymentTypeName;
    private Long tripCount;
    private Double percentage;
}