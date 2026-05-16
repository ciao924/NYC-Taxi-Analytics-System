package com.taxi.analytics.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_message")
public class AiMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private String messageId;

    private String role;

    private String content;

    private String sqlText;

    private String chartData;

    private Integer executionTimeMs;

    private Integer rowCount;

    private Integer feedbackScore;

    private String feedbackComment;

    private LocalDateTime createTime;
}