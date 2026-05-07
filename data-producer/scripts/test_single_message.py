#!/usr/bin/env python3
import sys
import os
import json
from kafka import KafkaProducer
from config import CONFIG

def test_single_message():
    print("Testing single message send...")
    
    try:
        # 创建生产者
        producer = KafkaProducer(
            bootstrap_servers=CONFIG['kafka']['bootstrap_servers'],
            acks=CONFIG['kafka']['acks'],
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )
        
        # 测试消息
        test_message = {
            'VendorID': 1,
            'lpep_pickup_datetime': '2025-04-01 00:00:00',
            'lpep_dropoff_datetime': '2025-04-01 00:10:00',
            'passenger_count': 2,
            'trip_distance': 5.5,
            'PULocationID': 100,
            'DOLocationID': 200,
            'fare_amount': 20.5,
            'tip_amount': 3.0
        }
        
        # 发送消息
        topic = CONFIG['topics']['green_taxi']
        future = producer.send(topic, test_message)
        # 等待发送完成
        record_metadata = future.get(timeout=10)
        
        print(f"✓ Message sent successfully!")
        print(f"  Topic: {record_metadata.topic}")
        print(f"  Partition: {record_metadata.partition}")
        print(f"  Offset: {record_metadata.offset}")
        print(f"  Message: {json.dumps(test_message, indent=2)}")
        
        producer.close()
        return True
        
    except Exception as e:
        print(f"✗ Error: {str(e)}")
        return False

if __name__ == "__main__":
    success = test_single_message()
    sys.exit(0 if success else 1)
