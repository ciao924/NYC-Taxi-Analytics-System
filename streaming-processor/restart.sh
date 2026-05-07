#!/bin/bash

# 定义变量
PROJECT_DIR=$(pwd)

# 函数：打印日志
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# 停止现有作业
log "Stopping existing job..."
if [ -f "$PROJECT_DIR/stop.sh" ]; then
    bash "$PROJECT_DIR/stop.sh"
else
    log "Error: stop.sh not found"
    exit 1
fi

# 等待作业停止
log "Waiting for job to stop..."
sleep 5

# 重新部署作业
log "Deploying new job..."
if [ -f "$PROJECT_DIR/deploy.sh" ]; then
    bash "$PROJECT_DIR/deploy.sh"
else
    log "Error: deploy.sh not found"
    exit 1
fi

log "Restart operation completed"
