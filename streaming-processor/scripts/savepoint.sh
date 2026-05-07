#!/bin/bash

# Savepoint 管理脚本
# 功能：创建和从 Savepoint 恢复 Flink 作业

# 配置参数
FLINK_HOME="/opt/module/flink-1.17.2"
KAFKA_BROKERS="hadoop102:9092,hadoop103:9092,hadoop104:9092"
SAVEPOINT_DIR="hdfs://hadoop102:8020/flink/savepoints"
JAR_PATH="/opt/flink/jobs/flink-realtime-consumer-1.0-SNAPSHOT.jar"
MAIN_CLASS="com.taxi.realtime.KafkaToMySQL"

# 日志文件
LOG_FILE="/var/log/flink_savepoint.log"

# 时间戳
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")

# 检查 Flink 命令是否存在
if [ ! -f "$FLINK_HOME/bin/flink" ]; then
    echo "[$TIMESTAMP] ERROR: Flink command not found at $FLINK_HOME/bin/flink" >> $LOG_FILE
    exit 1
fi

# 显示帮助信息
show_help() {
    echo "Usage: $0 [create|restore|list] [job_id]"
    echo ""
    echo "Commands:"
    echo "  create <job_id>   - 创建 Savepoint 并停止作业"
    echo "  restore <savepoint_path>  - 从 Savepoint 恢复作业"
    echo "  list             - 列出所有 Savepoint"
    echo ""
    exit 1
}

# 检查参数
if [ $# -lt 1 ]; then
    show_help
fi

# 执行命令
case "$1" in
    create)
        if [ $# -ne 2 ]; then
            echo "[$TIMESTAMP] ERROR: Missing job_id parameter" >> $LOG_FILE
            show_help
        fi
        
        JOB_ID=$2
        echo "[$TIMESTAMP] Creating savepoint for job $JOB_ID..." >> $LOG_FILE
        
        # 创建 Savepoint
        $FLINK_HOME/bin/flink savepoint $JOB_ID $SAVEPOINT_DIR
        
        if [ $? -eq 0 ]; then
            echo "[$TIMESTAMP] Savepoint created successfully for job $JOB_ID" >> $LOG_FILE
        else
            echo "[$TIMESTAMP] Failed to create savepoint for job $JOB_ID" >> $LOG_FILE
        fi
        ;;
        
    restore)
        if [ $# -ne 2 ]; then
            echo "[$TIMESTAMP] ERROR: Missing savepoint_path parameter" >> $LOG_FILE
            show_help
        fi
        
        SAVEPOINT_PATH=$2
        echo "[$TIMESTAMP] Restoring job from savepoint $SAVEPOINT_PATH..." >> $LOG_FILE
        
        # 从 Savepoint 恢复作业
        $FLINK_HOME/bin/flink run -s $SAVEPOINT_PATH -c $MAIN_CLASS $JAR_PATH
        
        if [ $? -eq 0 ]; then
            echo "[$TIMESTAMP] Job restored successfully from savepoint $SAVEPOINT_PATH" >> $LOG_FILE
        else
            echo "[$TIMESTAMP] Failed to restore job from savepoint $SAVEPOINT_PATH" >> $LOG_FILE
        fi
        ;;
        
    list)
        echo "[$TIMESTAMP] Listing savepoints in $SAVEPOINT_DIR..." >> $LOG_FILE
        
        # 列出 Savepoint
        hdfs dfs -ls -R $SAVEPOINT_DIR
        
        if [ $? -eq 0 ]; then
            echo "[$TIMESTAMP] Savepoints listed successfully" >> $LOG_FILE
        else
            echo "[$TIMESTAMP] Failed to list savepoints" >> $LOG_FILE
        fi
        ;;
        
    *)
        echo "[$TIMESTAMP] ERROR: Invalid command: $1" >> $LOG_FILE
        show_help
        ;;
esac

exit 0