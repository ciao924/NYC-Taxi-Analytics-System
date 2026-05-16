package com.taxi.analytics.modules.ai.service;

import com.taxi.analytics.modules.ai.entity.AiChatSession;
import com.taxi.analytics.modules.ai.entity.AiFavoriteQuery;
import com.taxi.analytics.modules.ai.entity.AiMessage;
import com.taxi.analytics.modules.ai.entity.AiScheduledQuery;

import java.time.LocalDateTime;
import java.util.List;

public interface AiDbService {

    AiChatSession getSessionById(String sessionId);

    List<AiChatSession> getAllSessions();

    String createSession(String userId, String sessionName);

    boolean updateSessionName(String sessionId, String sessionName);

    boolean deleteSession(String sessionId);

    void incrementMessageCount(String sessionId);

    AiMessage getMessageById(String messageId);

    List<AiMessage> getSessionMessages(String sessionId);

    String saveMessage(String sessionId, String role, String content, String sqlText, String chartData, Integer executionTimeMs, Integer rowCount);

    boolean saveMessageFeedback(String messageId, Integer feedbackScore, String feedbackComment);

    List<AiFavoriteQuery> getFavorites(String userId);

    Long addFavorite(String userId, String queryText, String queryName);

    boolean removeFavorite(Long favoriteId);

    void incrementFavoriteExecution(Long favoriteId);

    List<AiScheduledQuery> getScheduledTasks(String userId);

    Long createScheduledTask(String userId, String taskName, String queryText, String scheduleCron, String pushType, String pushTarget);

    boolean updateScheduledTaskStatus(Long taskId, LocalDateTime lastRunTime, LocalDateTime nextRunTime);

    boolean deleteScheduledTask(Long taskId);

    List<AiMessage> getPopularQueries(int limit);

    List<AiMessage> getRecentQueries(int limit);
}