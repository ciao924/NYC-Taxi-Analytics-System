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
public class TableHealthStatusDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String tableName;
    private String status;
    private Double score;
    private Long recordCount;
    private String lastUpdated;
}