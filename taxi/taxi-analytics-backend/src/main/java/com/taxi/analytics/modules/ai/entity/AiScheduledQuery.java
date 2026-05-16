package com.taxi.analytics.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_scheduled_query")
public class AiScheduledQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String taskName;

    private String queryText;

    private String scheduleCron;

    private String pushType;

    private String pushTarget;

    private Integer enabled;

    private LocalDateTime lastRunTime;

    private LocalDateTime nextRunTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}