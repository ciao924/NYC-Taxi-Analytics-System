package com.taxi.analytics.modules.ai.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taxi.analytics.modules.ai.entity.AiChatSession;
import com.taxi.analytics.modules.ai.entity.AiFavoriteQuery;
import com.taxi.analytics.modules.ai.entity.AiMessage;
import com.taxi.analytics.modules.ai.entity.AiScheduledQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@DS("ai")
public interface AiDbMapper {

    AiChatSession selectBySessionId(@Param("sessionId") String sessionId);

    List<AiChatSession> selectAllSessions();

    int insertSession(AiChatSession session);

    int updateSessionName(@Param("sessionId") String sessionId, @Param("sessionName") String sessionName, @Param("updateTime") LocalDateTime updateTime);

    int deleteSession(@Param("sessionId") String sessionId);

    int incrementMessageCount(@Param("sessionId") String sessionId, @Param("lastMessageTime") LocalDateTime lastMessageTime);

    AiMessage selectMessageByMessageId(@Param("messageId") String messageId);

    List<AiMessage> selectMessagesBySessionId(@Param("sessionId") String sessionId);

    int insertMessage(AiMessage message);

    int updateMessageFeedback(@Param("messageId") String messageId, @Param("feedbackScore") Integer feedbackScore, @Param("feedbackComment") String feedbackComment);

    List<AiFavoriteQuery> selectFavoritesByUserId(@Param("userId") String userId);

    int insertFavorite(AiFavoriteQuery favorite);

    int deleteFavorite(@Param("id") Long id);

    int incrementExecutionCount(@Param("id") Long id, @Param("lastExecuteTime") LocalDateTime lastExecuteTime);

    List<AiScheduledQuery> selectScheduledTasksByUserId(@Param("userId") String userId);

    int insertScheduledTask(AiScheduledQuery task);

    int updateScheduledTaskStatus(@Param("id") Long id, @Param("lastRunTime") LocalDateTime lastRunTime, @Param("nextRunTime") LocalDateTime nextRunTime);

    int deleteScheduledTask(@Param("id") Long id);

    @Select("SELECT * FROM ai_message WHERE role = 'user' GROUP BY content ORDER BY COUNT(*) DESC LIMIT #{limit}")
    List<AiMessage> selectPopularQueries(@Param("limit") int limit);

    @Select("SELECT * FROM ai_message WHERE role = 'user' ORDER BY create_time DESC LIMIT #{limit}")
    List<AiMessage> selectRecentQueries(@Param("limit") int limit);
}