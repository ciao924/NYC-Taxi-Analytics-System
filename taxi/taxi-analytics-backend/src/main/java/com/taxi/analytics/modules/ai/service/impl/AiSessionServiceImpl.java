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
        aiMapper.createSession(sessionId, sessionName, now, now);
        return sessionId;
    }

    @Override
    public List<Map<String, Object>> getSessions() {
        return aiMapper.getSessions();
    }

    @Override
    public boolean updateSession(String sessionId, String sessionName) {
        return aiMapper.updateSession(sessionId, sessionName, LocalDateTime.now()) > 0;
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
    public void saveMessage(String sessionId, String role, String content, String type, Long executionTime, Integer rowCount) {
        String messageId = UUID.randomUUID().toString();
        aiMapper.saveMessage(messageId, sessionId, role, content, type, LocalDateTime.now(), executionTime, rowCount);
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
    public boolean saveFeedback(String messageId, String feedback) {
        return aiMapper.saveFeedback(messageId, feedback) > 0;
    }

    @Override
    public boolean addFavorite(String userId, String queryText) {
        String favoriteId = UUID.randomUUID().toString();
        return aiMapper.addFavorite(favoriteId, userId, queryText, LocalDateTime.now()) > 0;
    }

    @Override
    public List<Map<String, Object>> getFavorites(String userId) {
        return aiMapper.getFavorites(userId);
    }

    @Override
    public boolean removeFavorite(String favoriteId) {
        return aiMapper.removeFavorite(favoriteId) > 0;
    }

    @Override
    public String createScheduledTask(String userId, String queryText, LocalDateTime scheduleTime) {
        String taskId = UUID.randomUUID().toString();
        aiMapper.createScheduledTask(taskId, userId, queryText, scheduleTime, "PENDING", LocalDateTime.now());
        return taskId;
    }

    @Override
    public List<Map<String, Object>> getScheduledTasks(String userId) {
        return aiMapper.getScheduledTasks(userId);
    }

    @Override
    public boolean deleteScheduledTask(String taskId) {
        return aiMapper.deleteScheduledTask(taskId) > 0;
    }
}