package com.taxi.analytics.modules.ai.service.dto;

import java.util.List;

public class AlertNormalizeResponse {
    private String normalizedAlert;
    private String severity;
    private List<String> suggestedActions;

    public String getNormalizedAlert() { return normalizedAlert; }
    public void setNormalizedAlert(String normalizedAlert) { this.normalizedAlert = normalizedAlert; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public List<String> getSuggestedActions() { return suggestedActions; }
    public void setSuggestedActions(List<String> suggestedActions) { this.suggestedActions = suggestedActions; }
}
