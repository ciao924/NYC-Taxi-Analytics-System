package com.taxi.analytics.modules.ai.client;

public class Usage {
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
}
