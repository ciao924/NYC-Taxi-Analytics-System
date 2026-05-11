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
public class AlertDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String tableName;
    private String columnName;
    private String alertType;
    private String severity;
    private String message;
    private String timestamp;
    private Boolean resolved;
}