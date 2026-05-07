#!/usr/bin/env python3
import sys
import os
from kafka import KafkaProducer, KafkaConsumer
from kafka.admin import KafkaAdminClient, NewTopic
from config.kafka_config import KAFKA_CONFIG, TOPICS

def test_kafka_connection():
    print("Testing Kafka connection...")
    
    try:
        # 测试生产者连接
        producer = KafkaProducer(
            bootstrap_servers=KAFKA_CONFIG['bootstrap_servers'],
            acks=KAFKA_CONFIG['acks'],
            retries=KAFKA_CONFIG['retries']
        )
        print("✓ Kafka producer connected successfully")
        
        # 测试消费者连接
        consumer = KafkaConsumer(
            bootstrap_servers=KAFKA_CONFIG['bootstrap_servers'],
            group_id='test-group',
            auto_offset_reset='earliest'
        )
        print("✓ Kafka consumer connected successfully")
        
        # 测试主题列表
        admin_client = KafkaAdminClient(
            bootstrap_servers=KAFKA_CONFIG['bootstrap_servers']
        )
        topics = admin_client.list_topics()
        print(f"✓ Found {len(topics)} topics: {topics}")
        
        # 检查目标主题是否存在
        target_topic = TOPICS['green_taxi']
        if target_topic in topics:
            print(f"✓ Target topic '{target_topic}' exists")
        else:
            print(f"✗ Target topic '{target_topic}' does not exist")
            # 创建主题
            new_topic = NewTopic(
                name=target_topic,
                num_partitions=6,
                replication_factor=2
            )
            admin_client.create_topics([new_topic])
            print(f"✓ Created topic '{target_topic}'")
        
        # 测试消息发送
        test_message = b"Test message from producer"
        producer.send(target_topic, test_message)
        producer.flush()
        print("✓ Test message sent successfully")
        
        # 关闭连接
        producer.close()
        consumer.close()
        admin_client.close()
        
        print("\n🎉 All Kafka connection tests passed!")
        return True
        
    except Exception as e:
        print(f"✗ Error: {str(e)}")
        return False

if __name__ == "__main__":
    success = test_kafka_connection()
    sys.exit(0 if success else 1)
