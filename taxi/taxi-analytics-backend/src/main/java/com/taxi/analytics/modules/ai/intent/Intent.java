package com.taxi.analytics.modules.ai.intent;

import java.util.Map;

public class Intent {
    private final IntentType intentType;
    private final Map<String, String> entities;
    private final double confidence;

    public Intent(IntentType intentType, Map<String, String> entities, double confidence) {
        this.intentType = intentType;
        this.entities = entities;
        this.confidence = confidence;
    }

    public IntentType getIntentType() {
        return intentType;
    }

    public Map<String, String> getEntities() {
        return entities;
    }

    public double getConfidence() {
        return confidence;
    }
}
