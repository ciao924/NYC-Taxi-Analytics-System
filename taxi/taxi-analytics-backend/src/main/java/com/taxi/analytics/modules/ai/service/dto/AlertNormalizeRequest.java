package com.taxi.analytics.modules.ai.service.dto;

public class AlertNormalizeRequest {
    private String alertContent;
    private String alertType;

    public String getAlertContent() { return alertContent; }
    public void setAlertContent(String alertContent) { this.alertContent = alertContent; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
}
