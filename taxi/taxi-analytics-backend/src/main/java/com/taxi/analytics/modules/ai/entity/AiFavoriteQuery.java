package com.taxi.analytics.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_favorite_query")
public class AiFavoriteQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String userId;

    private String queryText;

    private String queryName;

    private Integer executionCount;

    private LocalDateTime lastExecuteTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}