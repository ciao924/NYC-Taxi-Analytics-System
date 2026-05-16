package com.taxi.analytics.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_chat_session")
public class AiChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private String userId;

    private String sessionName;

    private Integer messageCount;

    private LocalDateTime lastMessageTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}