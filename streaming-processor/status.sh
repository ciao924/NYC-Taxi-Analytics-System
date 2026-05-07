#!/bin/bash

# 定义变量
FLINK_HOME="/opt/flink"
JOB_NAME="KafkaToMySQL"

# 函数：打印日志
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# 检查 Flink 是否安装
if [ ! -d "$FLINK_HOME" ]; then
    log "Error: Flink home directory not found at $FLINK_HOME"
    log "Please set FLINK_HOME environment variable to your Flink installation directory"
    exit 1
fi

# 列出所有作业
log "Listing all Flink jobs..."
$FLINK_HOME/bin/flink list

# 查找特定作业
log "\nChecking status of job: $JOB_NAME"
JOB_STATUS=$($FLINK_HOME/bin/flink list -r | grep "$JOB_NAME")

if [ -z "$JOB_STATUS" ]; then
    log "Job not found: $JOB_NAME"
else
    log "Job status: $JOB_STATUS"
    
    # 提取作业 ID
    JOB_ID=$(echo "$JOB_STATUS" | awk '{print $4}')
    if [ ! -z "$JOB_ID" ]; then
        log "\nJob details:"
        # 尝试获取作业详情（如果 Flink 版本支持）
        $FLINK_HOME/bin/flink info $JOB_ID 2>/dev/null || log "Detailed info not available (requires Flink 1.12+)"
    fi
fi

log "Status check completed"
