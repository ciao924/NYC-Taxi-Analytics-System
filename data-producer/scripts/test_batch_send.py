#!/usr/bin/env python3
import sys
import os
import time
from config import CONFIG
from scripts.kafka_producer import KafkaTaxiProducer
import argparse

def test_batch_send():
    print("Testing batch message send...")
    
    # 创建命令行参数
    parser = argparse.ArgumentParser()
    parser.add_argument('--file-path', type=str, default=CONFIG['data']['green_tripdata_path'])
    parser.add_argument('--topic', type=str, default=CONFIG['topics']['green_taxi'])
    parser.add_argument('--rate', type=int, default=20)
    parser.add_argument('--concurrency', type=int, default=1)
    parser.add_argument('--start-offset', type=int, default=0)
    
    args = parser.parse_args(['--rate', '20', '--start-offset', '0'])
    
    # 运行生产者，发送100条消息
    producer = KafkaTaxiProducer(args)
    
    # 覆盖process_data方法，只发送100条
    original_process_data = producer.process_data
    
    def limited_process_data():
        # 加载数据
        import pandas as pd
        df = pd.read_parquet(args.file_path)
        producer.stats['total_records'] = 100
        
        # 加载断点
        start_offset = producer.load_checkpoint()
        if start_offset >= 100:
            print('All test records have been sent, exiting...')
            return
        
        print(f'Starting batch test from offset: {start_offset}')
        
        # 处理100条数据
        for i in range(start_offset, min(100, len(df))):
            record = df.iloc[i].to_dict()
            
            if not producer.validate_record(record):
                print(f'Skipping invalid record at offset {i}')
                producer.stats['failed_records'] += 1
                continue
            
            if producer.send_message(record):
                # 速率控制
                if args.rate > 0:
                    elapsed = time.time() - producer.stats['last_sent_time']
                    expected_time = 1.0 / args.rate
                    if elapsed < expected_time:
                        time.sleep(expected_time - elapsed)
                    producer.stats['last_sent_time'] = time.time()
                
                # 每10条记录打印一次状态
                if (i + 1) % 10 == 0:
                    producer.print_stats()
                    producer.save_checkpoint(i + 1)
        
        # 保存最终断点
        producer.save_checkpoint(100)
        print('Batch test completed')
        producer.print_stats()
    
    # 替换方法
    producer.process_data = limited_process_data
    
    try:
        producer.run()
        return True
    except Exception as e:
        print(f"✗ Error: {str(e)}")
        return False

if __name__ == "__main__":
    success = test_batch_send()
    sys.exit(0 if success else 1)
