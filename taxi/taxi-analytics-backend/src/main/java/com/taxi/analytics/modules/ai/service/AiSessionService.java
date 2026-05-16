package com.taxi.analytics.modules.ai.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AiSessionService {

    String createSession(String sessionName);
    
    String createSession(String sessionId, String sessionName);

    List<Map<String, Object>> getSessions();

    boolean updateSession(String sessionId, String sessionName);

    boolean deleteSession(String sessionId);

    List<Map<String, Object>> getSessionMessages(String sessionId);

    void saveMessage(String sessionId, String role, String content, String sqlText, String chartData, Integer executionTimeMs, Integer rowCount);

    List<Map<String, Object>> getSuggestions();

    List<Map<String, Object>> getQueryHistory(int limit);

    boolean saveFeedback(String messageId, Integer feedbackScore, String feedbackComment);

    boolean addFavorite(String userId, String queryText, String queryName);

    List<Map<String, Object>> getFavorites(String userId);

    boolean removeFavorite(Long favoriteId);

    String createScheduledTask(String userId, String taskName, String queryText, String scheduleCron, String pushType, String pushTarget);

    List<Map<String, Object>> getScheduledTasks(String userId);

    boolean deleteScheduledTask(Long taskId);
}