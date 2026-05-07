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

# 查找作业 ID
log "Searching for job: $JOB_NAME"
JOB_ID=$($FLINK_HOME/bin/flink list -r | grep "$JOB_NAME" | awk '{print $4}')

if [ -z "$JOB_ID" ]; then
    log "Job not found: $JOB_NAME"
    exit 0
fi

log "Found job: $JOB_NAME with ID: $JOB_ID"

# 停止作业
log "Stopping job: $JOB_ID"
$FLINK_HOME/bin/flink cancel $JOB_ID

if [ $? -eq 0 ]; then
    log "Job stopped successfully"
else
    log "Error: Failed to stop job"
    exit 1
fi

log "Stop operation completed"
