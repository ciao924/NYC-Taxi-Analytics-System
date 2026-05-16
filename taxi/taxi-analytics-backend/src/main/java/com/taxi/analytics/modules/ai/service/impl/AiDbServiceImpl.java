package com.taxi.analytics.modules.ai.service.impl;

import com.taxi.analytics.modules.ai.entity.AiChatSession;
import com.taxi.analytics.modules.ai.entity.AiFavoriteQuery;
import com.taxi.analytics.modules.ai.entity.AiMessage;
import com.taxi.analytics.modules.ai.entity.AiScheduledQuery;
import com.taxi.analytics.modules.ai.mapper.AiDbMapper;
import com.taxi.analytics.modules.ai.service.AiDbService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AiDbServiceImpl implements AiDbService {

    private final AiDbMapper aiDbMapper;

    public AiDbServiceImpl(AiDbMapper aiDbMapper) {
        this.aiDbMapper = aiDbMapper;
    }

    @Override
    public AiChatSession getSessionById(String sessionId) {
        return aiDbMapper.selectBySessionId(sessionId);
    }

    @Override
    public List<AiChatSession> getAllSessions() {
        return aiDbMapper.selectAllSessions();
    }

    @Override
    public String createSession(String userId, String sessionName) {
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        AiChatSession session = new AiChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setSessionName(sessionName != null ? sessionName : "新对话");
        session.setMessageCount(0);
        session.setLastMessageTime(now);
        session.setCreateTime(now);
        session.setUpdateTime(now);

        aiDbMapper.insertSession(session);
        return sessionId;
    }

    @Override
    public boolean updateSessionName(String sessionId, String sessionName) {
        return aiDbMapper.updateSessionName(sessionId, sessionName, LocalDateTime.now()) > 0;
    }

    @Override
    public boolean deleteSession(String sessionId) {
        return aiDbMapper.deleteSession(sessionId) > 0;
    }

    @Override
    public void incrementMessageCount(String sessionId) {
        aiDbMapper.incrementMessageCount(sessionId, LocalDateTime.now());
    }

    @Override
    public AiMessage getMessageById(String messageId) {
        return aiDbMapper.selectMessageByMessageId(messageId);
    }

    @Override
    public List<AiMessage> getSessionMessages(String sessionId) {
        return aiDbMapper.selectMessagesBySessionId(sessionId);
    }

    @Override
    public String saveMessage(String sessionId, String role, String content, String sqlText, String chartData, Integer executionTimeMs, Integer rowCount) {
        String messageId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        AiMessage message = new AiMessage();
        message.setSessionId(sessionId);
        message.setMessageId(messageId);
        message.setRole(role);
        message.setContent(content);
        message.setSqlText(sqlText);
        message.setChartData(chartData);
        message.setExecutionTimeMs(executionTimeMs);
        message.setRowCount(rowCount);
        message.setCreateTime(now);

        aiDbMapper.insertMessage(message);

        incrementMessageCount(sessionId);

        return messageId;
    }

    @Override
    public boolean saveMessageFeedback(String messageId, Integer feedbackScore, String feedbackComment) {
        return aiDbMapper.updateMessageFeedback(messageId, feedbackScore, feedbackComment) > 0;
    }

    @Override
    public List<AiFavoriteQuery> getFavorites(String userId) {
        return aiDbMapper.selectFavoritesByUserId(userId);
    }

    @Override
    public Long addFavorite(String userId, String queryText, String queryName) {
        LocalDateTime now = LocalDateTime.now();

        AiFavoriteQuery favorite = new AiFavoriteQuery();
        favorite.setUserId(userId);
        favorite.setQueryText(queryText);
        favorite.setQueryName(queryName);
        favorite.setExecutionCount(0);
        favorite.setLastExecuteTime(now);
        favorite.setCreateTime(now);
        favorite.setUpdateTime(now);

        aiDbMapper.insertFavorite(favorite);
        return favorite.getId();
    }

    @Override
    public boolean removeFavorite(Long favoriteId) {
        return aiDbMapper.deleteFavorite(favoriteId) > 0;
    }

    @Override
    public void incrementFavoriteExecution(Long favoriteId) {
        aiDbMapper.incrementExecutionCount(favoriteId, LocalDateTime.now());
    }

    @Override
    public List<AiScheduledQuery> getScheduledTasks(String userId) {
        return aiDbMapper.selectScheduledTasksByUserId(userId);
    }

    @Override
    public Long createScheduledTask(String userId, String taskName, String queryText, String scheduleCron, String pushType, String pushTarget) {
        LocalDateTime now = LocalDateTime.now();

        AiScheduledQuery task = new AiScheduledQuery();
        task.setUserId(userId);
        task.setTaskName(taskName);
        task.setQueryText(queryText);
        task.setScheduleCron(scheduleCron);
        task.setPushType(pushType);
        task.setPushTarget(pushTarget);
        task.setEnabled(1);
        task.setCreateTime(now);
        task.setUpdateTime(now);

        aiDbMapper.insertScheduledTask(task);
        return task.getId();
    }

    @Override
    public boolean updateScheduledTaskStatus(Long taskId, LocalDateTime lastRunTime, LocalDateTime nextRunTime) {
        return aiDbMapper.updateScheduledTaskStatus(taskId, lastRunTime, nextRunTime) > 0;
    }

    @Override
    public boolean deleteScheduledTask(Long taskId) {
        return aiDbMapper.deleteScheduledTask(taskId) > 0;
    }

    @Override
    public List<AiMessage> getPopularQueries(int limit) {
        return aiDbMapper.selectPopularQueries(limit);
    }

    @Override
    public List<AiMessage> getRecentQueries(int limit) {
        return aiDbMapper.selectRecentQueries(limit);
    }
}