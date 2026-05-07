#!/bin/bash

# 定义变量
PROJECT_DIR=$(pwd)
JAR_NAME="flink_consumer-1.0-SNAPSHOT.jar"
FLINK_HOME="/opt/flink"
JOB_NAME="KafkaToMySQL"

# 函数：打印日志
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# 检查 Maven 是否安装
if ! command -v mvn &> /dev/null; then
    log "Error: Maven is not installed"
    exit 1
fi

# 检查 Flink 是否安装
if [ ! -d "$FLINK_HOME" ]; then
    log "Warning: Flink home directory not found at $FLINK_HOME"
    log "Please set FLINK_HOME environment variable to your Flink installation directory"
    # 继续执行，因为可能在集群环境中通过其他方式提交
fi

# 编译和打包
log "Building project..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    log "Error: Build failed"
    exit 1
fi

# 检查 JAR 文件是否生成
JAR_PATH="$PROJECT_DIR/target/$JAR_NAME"
if [ ! -f "$JAR_PATH" ]; then
    log "Error: JAR file not found at $JAR_PATH"
    exit 1
fi

log "Build successful. JAR file: $JAR_PATH"

# 提交作业到 Flink 集群
if [ -d "$FLINK_HOME" ]; then
    log "Submitting job to Flink cluster..."
    $FLINK_HOME/bin/flink run -d -c com.taxi.realtime.KafkaToMySQL $JAR_PATH
    
    if [ $? -eq 0 ]; then
        log "Job submitted successfully"
    else
        log "Error: Failed to submit job"
        exit 1
    fi
else
    log "Flink not found, skipping job submission"
    log "You can submit the JAR file manually using: flink run -d -c com.taxi.realtime.KafkaToMySQL $JAR_PATH"
fi

log "Deployment completed"
