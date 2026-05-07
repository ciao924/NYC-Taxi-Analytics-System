#!/bin/bash

# Kafka Lag 监控脚本
# 功能：监控 Kafka 消费组的 Lag 情况，超过阈值时发送钉钉告警

# 配置参数
KAFKA_HOME="/opt/module/kafka_2.13"
KAFKA_BROKERS="hadoop102:9092,hadoop103:9092,hadoop104:9092"
CONSUMER_GROUP="flink-green-trip-group"
TOPIC="taxi_trip_green"
LAG_THRESHOLD=10000  # Lag 阈值
DINGTALK_WEBHOOK="https://oapi.dingtalk.com/robot/send?access_token=YOUR_ACCESS_TOKEN"

# 日志文件
LOG_FILE="/var/log/kafka_lag_monitor.log"

# 时间戳
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")

# 检查 Kafka 命令是否存在
if [ ! -f "$KAFKA_HOME/bin/kafka-consumer-groups.sh" ]; then
    echo "[$TIMESTAMP] ERROR: Kafka consumer groups script not found at $KAFKA_HOME/bin/kafka-consumer-groups.sh" >> $LOG_FILE
    exit 1
fi

# 获取 Lag 信息
echo "[$TIMESTAMP] Checking Kafka Lag for group $CONSUMER_GROUP..." >> $LOG_FILE

LAG_INFO=$($KAFKA_HOME/bin/kafka-consumer-groups.sh --bootstrap-server $KAFKA_BROKERS --group $CONSUMER_GROUP --describe | grep -E "LAG|TOPIC")

# 解析 Lag 信息
total_lag=0
while IFS= read -r line; do
    if [[ $line == *"LAG"* ]]; then
        # 提取 Lag 值
        lag=$(echo $line | awk '{print $NF}')
        total_lag=$((total_lag + lag))
    fi
done <<< "$LAG_INFO"

# 输出 Lag 信息
echo "[$TIMESTAMP] Total Lag for $CONSUMER_GROUP: $total_lag" >> $LOG_FILE

# 检查是否超过阈值
if [ $total_lag -gt $LAG_THRESHOLD ]; then
    echo "[$TIMESTAMP] WARNING: Lag $total_lag exceeds threshold $LAG_THRESHOLD" >> $LOG_FILE
    
    # 发送钉钉告警
    message="【Kafka Lag 告警】\n消费组：$CONSUMER_GROUP\n主题：$TOPIC\n当前 Lag：$total_lag\n阈值：$LAG_THRESHOLD\n时间：$TIMESTAMP"
    
    # 构建钉钉消息体
    payload='{"msgtype": "text", "text": {"content": "'"$message"'"}}'
    
    # 发送告警
    curl -s -X POST -H "Content-Type: application/json" -d "$payload" "$DINGTALK_WEBHOOK"
    
    if [ $? -eq 0 ]; then
        echo "[$TIMESTAMP] DingTalk alert sent successfully" >> $LOG_FILE
    else
        echo "[$TIMESTAMP] Failed to send DingTalk alert" >> $LOG_FILE
    fi
else
    echo "[$TIMESTAMP] Lag $total_lag is within threshold $LAG_THRESHOLD" >> $LOG_FILE
fi

# 清理日志（保留最近7天）
find /var/log -name "kafka_lag_monitor.log*" -mtime +7 -delete

exit 0