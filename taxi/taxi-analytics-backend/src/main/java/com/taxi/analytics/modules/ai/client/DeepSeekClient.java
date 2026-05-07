package com.taxi.analytics.modules.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String baseUrl;

    public DeepSeekClient() {
        this(System.getenv("DEEPSEEK_API_KEY"), "https://api.deepseek.com/v1");
    }

    public DeepSeekClient(String apiKey) {
        this(apiKey, "https://api.deepseek.com/v1");
    }

    public DeepSeekClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey != null ? apiKey : "";
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public ChatResponse chat(List<ChatMessage> messages) {
        return chat(messages, "deepseek-chat");
    }

    public ChatResponse chat(List<ChatMessage> messages, String model) {
        try {
            ChatRequest requestBody = new ChatRequest(
                    model,
                    messages,
                    0.7,
                    2000
            );

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), ChatResponse.class);
            } else {
                log.error("DeepSeek API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("API error: " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("Failed to call DeepSeek API", e);
            throw new RuntimeException("Request failed: " + e.getMessage());
        }
    }

    public String chatSync(String prompt) {
        return chatSync(prompt, "deepseek-chat");
    }

    public String chatSync(String prompt, String model) {
        List<ChatMessage> messages = List.of(new ChatMessage("user", prompt));
        ChatResponse response = chat(messages, model);
        if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
            Choice firstChoice = response.getChoices().get(0);
            if (firstChoice != null && firstChoice.getMessage() != null) {
                return firstChoice.getMessage().getContent();
            }
        }
        return "";
    }
}

class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private double temperature;
    private int maxTokens;

    public ChatRequest(String model, List<ChatMessage> messages, double temperature, int maxTokens) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
}
