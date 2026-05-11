package com.taxi.analytics.modules.dashboard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardQueryRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @NotBlank(message = "开始日期不能为空")
    private String startDate;
    
    @NotBlank(message = "结束日期不能为空")
    private String endDate;
    
    private String vendorId;
    
    private String borough;
    
    private String taxiType;
}