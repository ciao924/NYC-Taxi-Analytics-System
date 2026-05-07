package com.taxi.analytics.modules.ai.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AiSessionService {
    
    /**
     * 创建新会话
     */
    String createSession(String sessionName);
    
    /**
     * 获取会话列表
     */
    List<Map<String, Object>> getSessions();
    
    /**
     * 更新会话名称
     */
    boolean updateSession(String sessionId, String sessionName);
    
    /**
     * 删除会话
     */
    boolean deleteSession(String sessionId);
    
    /**
     * 获取会话消息历史
     */
    List<Map<String, Object>> getSessionMessages(String sessionId);
    
    /**
     * 保存消息
     */
    void saveMessage(String sessionId, String role, String content, String type, Long executionTime, Integer rowCount);
    
    /**
     * 获取查询建议
     */
    List<Map<String, Object>> getSuggestions();
    
    /**
     * 获取查询历史
     */
    List<Map<String, Object>> getQueryHistory(int limit);
    
    /**
     * 保存反馈
     */
    boolean saveFeedback(String messageId, String feedback);
    
    /**
     * 添加收藏
     */
    boolean addFavorite(String userId, String queryText);
    
    /**
     * 获取收藏列表
     */
    List<Map<String, Object>> getFavorites(String userId);
    
    /**
     * 取消收藏
     */
    boolean removeFavorite(String favoriteId);
    
    /**
     * 创建定时查询任务
     */
    String createScheduledTask(String userId, String queryText, LocalDateTime scheduleTime);
    
    /**
     * 获取定时任务列表
     */
    List<Map<String, Object>> getScheduledTasks(String userId);
    
    /**
     * 删除定时任务
     */
    boolean deleteScheduledTask(String taskId);
}