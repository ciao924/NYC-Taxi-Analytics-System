package com.taxi.analytics.modules.ai.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
@DS("ai")
public interface AiMapper {
    
    // 会话相关
    @Insert("INSERT INTO ai_chat_session (session_id, session_name, created_at, updated_at) VALUES (#{sessionId}, #{sessionName}, #{createdAt}, #{updatedAt})")
    int createSession(String sessionId, String sessionName, LocalDateTime createdAt, LocalDateTime updatedAt);
    
    @Select("SELECT * FROM ai_chat_session ORDER BY updated_at DESC")
    List<Map<String, Object>> getSessions();
    
    @Update("UPDATE ai_chat_session SET session_name = #{sessionName}, updated_at = #{updatedAt} WHERE session_id = #{sessionId}")
    int updateSession(String sessionId, String sessionName, LocalDateTime updatedAt);
    
    @Delete("DELETE FROM ai_chat_session WHERE session_id = #{sessionId}")
    int deleteSession(String sessionId);
    
    // 消息相关
    @Insert("INSERT INTO ai_message (message_id, session_id, role, content, type, created_at, execution_time, row_count) VALUES (#{messageId}, #{sessionId}, #{role}, #{content}, #{type}, #{createdAt}, #{executionTime}, #{rowCount})")
    int saveMessage(String messageId, String sessionId, String role, String content, String type, LocalDateTime createdAt, Long executionTime, Integer rowCount);
    
    @Select("SELECT * FROM ai_message WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<Map<String, Object>> getSessionMessages(String sessionId);
    
    @Select("SELECT content FROM ai_message WHERE role = 'user' GROUP BY content ORDER BY COUNT(*) DESC LIMIT 10")
    List<Map<String, Object>> getPopularQueries();
    
    @Select("SELECT * FROM ai_message WHERE role = 'user' ORDER BY created_at DESC LIMIT #{limit}")
    List<Map<String, Object>> getQueryHistory(int limit);
    
    // 收藏相关
    @Insert("INSERT INTO ai_favorite_query (favorite_id, user_id, query_text, created_at) VALUES (#{favoriteId}, #{userId}, #{queryText}, #{createdAt})")
    int addFavorite(String favoriteId, String userId, String queryText, LocalDateTime createdAt);
    
    @Select("SELECT * FROM ai_favorite_query WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Map<String, Object>> getFavorites(String userId);
    
    @Delete("DELETE FROM ai_favorite_query WHERE favorite_id = #{favoriteId}")
    int removeFavorite(String favoriteId);
    
    // 定时任务相关
    @Insert("INSERT INTO ai_scheduled_query (task_id, user_id, query_text, schedule_time, status, created_at) VALUES (#{taskId}, #{userId}, #{queryText}, #{scheduleTime}, #{status}, #{createdAt})")
    int createScheduledTask(String taskId, String userId, String queryText, LocalDateTime scheduleTime, String status, LocalDateTime createdAt);
    
    @Select("SELECT * FROM ai_scheduled_query WHERE user_id = #{userId} ORDER BY schedule_time DESC")
    List<Map<String, Object>> getScheduledTasks(String userId);
    
    @Update("UPDATE ai_scheduled_query SET status = #{status} WHERE task_id = #{taskId}")
    int updateTaskStatus(String taskId, String status);
    
    @Delete("DELETE FROM ai_scheduled_query WHERE task_id = #{taskId}")
    int deleteScheduledTask(String taskId);
    
    // 反馈相关
    @Update("UPDATE ai_message SET feedback = #{feedback} WHERE message_id = #{messageId}")
    int saveFeedback(String messageId, String feedback);
}