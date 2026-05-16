package com.taxi.analytics.modules.ai.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
@DS("ai")
public interface AiMapper {

    @Insert("INSERT INTO ai_chat_session (session_id, user_id, session_name, message_count, last_message_time, create_time, update_time) VALUES (#{sessionId}, #{userId}, #{sessionName}, #{messageCount}, #{lastMessageTime}, #{createTime}, #{updateTime})")
    int createSession(@Param("sessionId") String sessionId, @Param("userId") String userId, @Param("sessionName") String sessionName, @Param("messageCount") Integer messageCount, @Param("lastMessageTime") LocalDateTime lastMessageTime, @Param("createTime") LocalDateTime createTime, @Param("updateTime") LocalDateTime updateTime);

    @Select("SELECT * FROM ai_chat_session ORDER BY last_message_time DESC")
    List<Map<String, Object>> getSessions();

    @Update("UPDATE ai_chat_session SET session_name = #{sessionName}, last_message_time = #{lastMessageTime}, update_time = #{updateTime} WHERE session_id = #{sessionId}")
    int updateSession(@Param("sessionId") String sessionId, @Param("sessionName") String sessionName, @Param("lastMessageTime") LocalDateTime lastMessageTime, @Param("updateTime") LocalDateTime updateTime);

    @Delete("DELETE FROM ai_chat_session WHERE session_id = #{sessionId}")
    int deleteSession(@Param("sessionId") String sessionId);

    @Insert("INSERT INTO ai_message (session_id, message_id, role, content, sql_text, chart_data, execution_time_ms, row_count, create_time) VALUES (#{sessionId}, #{messageId}, #{role}, #{content}, #{sqlText}, #{chartData}, #{executionTimeMs}, #{rowCount}, #{createTime})")
    int saveMessage(@Param("sessionId") String sessionId, @Param("messageId") String messageId, @Param("role") String role, @Param("content") String content, @Param("sqlText") String sqlText, @Param("chartData") String chartData, @Param("executionTimeMs") Integer executionTimeMs, @Param("rowCount") Integer rowCount, @Param("createTime") LocalDateTime createTime);

    @Select("<script>SELECT id, session_id, message_id, role, content, sql_text, " +
            "IFNULL(chart_data, JSON_UNQUOTE('null')) AS chart_data, " +
            "execution_time_ms, row_count, create_time " +
            "FROM ai_message WHERE session_id = #{sessionId} ORDER BY create_time ASC</script>")
    List<Map<String, Object>> getSessionMessages(@Param("sessionId") String sessionId);

    @Select("SELECT content FROM ai_message WHERE role = 'user' GROUP BY content ORDER BY COUNT(*) DESC LIMIT 10")
    List<Map<String, Object>> getPopularQueries();

    @Select("SELECT * FROM ai_message WHERE role = 'user' ORDER BY create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> getQueryHistory(@Param("limit") int limit);

    @Insert("INSERT INTO ai_favorite_query (user_id, query_text, query_name, execution_count, last_execute_time, create_time, update_time) VALUES (#{userId}, #{queryText}, #{queryName}, #{executionCount}, #{lastExecuteTime}, #{createTime}, #{updateTime})")
    int addFavorite(@Param("userId") String userId, @Param("queryText") String queryText, @Param("queryName") String queryName, @Param("executionCount") Integer executionCount, @Param("lastExecuteTime") LocalDateTime lastExecuteTime, @Param("createTime") LocalDateTime createTime, @Param("updateTime") LocalDateTime updateTime);

    @Select("SELECT * FROM ai_favorite_query WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Map<String, Object>> getFavorites(@Param("userId") String userId);

    @Delete("DELETE FROM ai_favorite_query WHERE id = #{id}")
    int removeFavorite(@Param("id") Long id);

    @Update("UPDATE ai_favorite_query SET execution_count = execution_count + 1, last_execute_time = #{lastExecuteTime} WHERE id = #{id}")
    int incrementFavoriteExecution(@Param("id") Long id, @Param("lastExecuteTime") LocalDateTime lastExecuteTime);

    @Insert("INSERT INTO ai_scheduled_query (user_id, task_name, query_text, schedule_cron, push_type, push_target, enabled, last_run_time, next_run_time, create_time, update_time) VALUES (#{userId}, #{taskName}, #{queryText}, #{scheduleCron}, #{pushType}, #{pushTarget}, #{enabled}, #{lastRunTime}, #{nextRunTime}, #{createTime}, #{updateTime})")
    int createScheduledTask(@Param("userId") String userId, @Param("taskName") String taskName, @Param("queryText") String queryText, @Param("scheduleCron") String scheduleCron, @Param("pushType") String pushType, @Param("pushTarget") String pushTarget, @Param("enabled") Integer enabled, @Param("lastRunTime") LocalDateTime lastRunTime, @Param("nextRunTime") LocalDateTime nextRunTime, @Param("createTime") LocalDateTime createTime, @Param("updateTime") LocalDateTime updateTime);

    @Select("SELECT * FROM ai_scheduled_query WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Map<String, Object>> getScheduledTasks(@Param("userId") String userId);

    @Update("UPDATE ai_scheduled_query SET last_run_time = #{lastRunTime}, next_run_time = #{nextRunTime} WHERE id = #{id}")
    int updateScheduledTaskStatus(@Param("id") Long id, @Param("lastRunTime") LocalDateTime lastRunTime, @Param("nextRunTime") LocalDateTime nextRunTime);

    @Delete("DELETE FROM ai_scheduled_query WHERE id = #{id}")
    int deleteScheduledTask(@Param("id") Long id);

    @Update("UPDATE ai_message SET feedback_score = #{feedbackScore}, feedback_comment = #{feedbackComment} WHERE message_id = #{messageId}")
    int saveFeedback(@Param("messageId") String messageId, @Param("feedbackScore") Integer feedbackScore, @Param("feedbackComment") String feedbackComment);

    @Update("UPDATE ai_chat_session SET message_count = message_count + 1, last_message_time = #{lastMessageTime}, update_time = #{lastMessageTime} WHERE session_id = #{sessionId}")
    int incrementMessageCount(@Param("sessionId") String sessionId, @Param("lastMessageTime") LocalDateTime lastMessageTime);
}