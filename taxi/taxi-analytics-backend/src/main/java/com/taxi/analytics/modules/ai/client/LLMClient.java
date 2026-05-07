package com.taxi.analytics.modules.ai.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LLMClient {

    private static final Logger log = LoggerFactory.getLogger(LLMClient.class);

    private DeepSeekClient primaryClient;
    private DeepSeekClient fallbackClient;

    public void initialize(String primaryApiKey, String fallbackApiKey) {
        primaryClient = new DeepSeekClient(primaryApiKey);
        if (fallbackApiKey != null && !fallbackApiKey.isEmpty()) {
            fallbackClient = new DeepSeekClient(fallbackApiKey);
        }
    }

    public ChatResponse chat(List<ChatMessage> messages) {
        try {
            ChatResponse response = primaryClient.chat(messages);
            return response;
        } catch (Exception e) {
            log.warn("Primary LLM failed, trying fallback: {}", e.getMessage());
            if (fallbackClient != null) {
                try {
                    return fallbackClient.chat(messages);
                } catch (Exception fallbackException) {
                    log.error("Fallback LLM also failed", fallbackException);
                    throw new RuntimeException("Both LLM clients failed: " + fallbackException.getMessage());
                }
            } else {
                throw new RuntimeException("LLM client failed: " + e.getMessage());
            }
        }
    }

    public String chatSync(String prompt) {
        List<ChatMessage> messages = List.of(new ChatMessage("user", prompt));
        try {
            ChatResponse response = chat(messages);
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                Choice firstChoice = response.getChoices().get(0);
                if (firstChoice != null && firstChoice.getMessage() != null) {
                    return firstChoice.getMessage().getContent();
                }
            }
        } catch (Exception e) {
            log.error("Chat sync failed", e);
        }
        return "";
    }
}
