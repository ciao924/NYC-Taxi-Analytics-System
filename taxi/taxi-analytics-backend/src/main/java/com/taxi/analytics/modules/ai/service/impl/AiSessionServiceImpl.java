package com.taxi.analytics.modules.ai.service.impl;

import com.taxi.analytics.modules.ai.mapper.AiMapper;
import com.taxi.analytics.modules.ai.service.AiSessionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiSessionServiceImpl implements AiSessionService {

    private final AiMapper aiMapper;

    public AiSessionServiceImpl(AiMapper aiMapper) {
        this.aiMapper = aiMapper;
    }

    @Override
    public String createSession(String sessionName) {
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        aiMapper.createSession(sessionId, null, sessionName != null ? sessionName : "新对话", 0, now, now, now);
        return sessionId;
    }

    @Override
    public String createSession(String sessionId, String sessionName) {
        LocalDateTime now = LocalDateTime.now();
        aiMapper.createSession(sessionId, null, sessionName != null ? sessionName : "新对话", 0, now, now, now);
        return sessionId;
    }

    @Override
    public List<Map<String, Object>> getSessions() {
        return aiMapper.getSessions();
    }

    @Override
    public boolean updateSession(String sessionId, String sessionName) {
        LocalDateTime now = LocalDateTime.now();
        return aiMapper.updateSession(sessionId, sessionName, now, now) > 0;
    }

    @Override
    public boolean deleteSession(String sessionId) {
        return aiMapper.deleteSession(sessionId) > 0;
    }

    @Override
    public List<Map<String, Object>> getSessionMessages(String sessionId) {
        return aiMapper.getSessionMessages(sessionId);
    }

    @Override
    public void saveMessage(String sessionId, String role, String content, String sqlText, String chartData, Integer executionTimeMs, Integer rowCount) {
        String messageId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        aiMapper.saveMessage(sessionId, messageId, role, content, sqlText, chartData, executionTimeMs, rowCount, now);
        aiMapper.incrementMessageCount(sessionId, now);
    }

    @Override
    public List<Map<String, Object>> getSuggestions() {
        return aiMapper.getPopularQueries();
    }

    @Override
    public List<Map<String, Object>> getQueryHistory(int limit) {
        return aiMapper.getQueryHistory(limit);
    }

    @Override
    public boolean saveFeedback(String messageId, Integer feedbackScore, String feedbackComment) {
        return aiMapper.saveFeedback(messageId, feedbackScore, feedbackComment) > 0;
    }

    @Override
    public boolean addFavorite(String userId, String queryText, String queryName) {
        LocalDateTime now = LocalDateTime.now();
        return aiMapper.addFavorite(userId, queryText, queryName, 0, now, now, now) > 0;
    }

    @Override
    public List<Map<String, Object>> getFavorites(String userId) {
        return aiMapper.getFavorites(userId);
    }

    @Override
    public boolean removeFavorite(Long favoriteId) {
        return aiMapper.removeFavorite(favoriteId) > 0;
    }

    @Override
    public String createScheduledTask(String userId, String taskName, String queryText, String scheduleCron, String pushType, String pushTarget) {
        String taskId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        aiMapper.createScheduledTask(userId, taskName, queryText, scheduleCron, pushType, pushTarget, 1, now, now, now, now);
        return taskId;
    }

    @Override
    public List<Map<String, Object>> getScheduledTasks(String userId) {
        return aiMapper.getScheduledTasks(userId);
    }

    @Override
    public boolean deleteScheduledTask(Long taskId) {
        return aiMapper.deleteScheduledTask(taskId) > 0;
    }
}