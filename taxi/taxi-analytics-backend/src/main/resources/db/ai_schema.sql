-- nyc_taxi_ai 数据库建表语句
-- 根据 docs/yc_taxi_ai.csv 表结构定义创建

-- 创建 ai_chat_session 表（聊天会话表）
CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    session_id VARCHAR(64) NOT NULL COMMENT '会话唯一标识',
    user_id VARCHAR(64) DEFAULT NULL COMMENT '用户ID',
    session_name VARCHAR(255) DEFAULT '新对话' COMMENT '会话名称',
    message_count INT DEFAULT 0 COMMENT '消息数量',
    last_message_time DATETIME DEFAULT NULL COMMENT '最后消息时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_last_message_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天会话表';

-- 创建 ai_favorite_query 表（收藏查询表）
CREATE TABLE IF NOT EXISTS ai_favorite_query (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    query_text VARCHAR(1000) NOT NULL COMMENT '查询文本',
    query_name VARCHAR(255) DEFAULT NULL COMMENT '查询名称',
    execution_count INT DEFAULT 0 COMMENT '执行次数',
    last_execute_time DATETIME DEFAULT NULL COMMENT '最后执行时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_last_execute_time (last_execute_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI收藏查询表';

-- 创建 ai_message 表（消息表）
CREATE TABLE IF NOT EXISTS ai_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    message_id VARCHAR(64) NOT NULL COMMENT '消息唯一标识',
    role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant/system',
    content TEXT DEFAULT NULL COMMENT '消息内容',
    sql_text TEXT DEFAULT NULL COMMENT 'SQL文本',
    chart_data JSON DEFAULT NULL COMMENT '图表配置JSON',
    execution_time_ms INT DEFAULT NULL COMMENT '执行时间（毫秒）',
    row_count INT DEFAULT NULL COMMENT '返回行数',
    feedback_score TINYINT DEFAULT NULL COMMENT '反馈评分（1-5）',
    feedback_comment VARCHAR(500) DEFAULT NULL COMMENT '反馈评论',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_id (message_id),
    INDEX idx_session_id (session_id),
    INDEX idx_role (role),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI消息表';

-- 创建 ai_scheduled_query 表（定时查询表）
CREATE TABLE IF NOT EXISTS ai_scheduled_query (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    task_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    query_text VARCHAR(1000) NOT NULL COMMENT '查询文本',
    schedule_cron VARCHAR(64) NOT NULL COMMENT 'Cron表达式',
    push_type VARCHAR(32) NOT NULL COMMENT '推送类型：email/sms/webhook',
    push_target VARCHAR(255) DEFAULT NULL COMMENT '推送目标',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    last_run_time DATETIME DEFAULT NULL COMMENT '最后运行时间',
    next_run_time DATETIME DEFAULT NULL COMMENT '下次运行时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_enabled (enabled),
    INDEX idx_next_run_time (next_run_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI定时查询表';