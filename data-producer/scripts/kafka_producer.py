#!/usr/bin/env python3
import sys
import os
# 添加当前目录的父目录到Python路径，以便能够导入config模块
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import time
import json
import argparse
import logging
from logging.handlers import RotatingFileHandler
from kafka import KafkaProducer
import pandas as pd
from config import CONFIG

# 配置日志
def setup_logging():
    log_dir = 'logs'
    os.makedirs(log_dir, exist_ok=True)
    
    logger = logging.getLogger('kafka_producer')
    logger.setLevel(logging.INFO)
    
    # 控制台日志
    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    console_formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
    console_handler.setFormatter(console_formatter)
    
    # 文件日志（滚动）
    file_handler = RotatingFileHandler(
        os.path.join(log_dir, 'kafka_producer.log'),
        maxBytes=10485760,  # 10MB
        backupCount=5
    )
    file_handler.setLevel(logging.INFO)
    file_formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
    file_handler.setFormatter(file_formatter)
    
    logger.addHandler(console_handler)
    logger.addHandler(file_handler)
    
    return logger

logger = setup_logging()

def serialize_message(message):
    """序列化消息，处理 Timestamp 类型"""
    def convert_timestamp(obj):
        if pd.api.types.is_datetime64_any_dtype(type(obj)) or hasattr(obj, 'strftime'):
            return obj.strftime('%Y-%m-%d %H:%M:%S')
        raise TypeError(f"Object of type {type(obj).__name__} is not JSON serializable")
    
    return json.dumps(message, default=convert_timestamp).encode('utf-8')

class KafkaTaxiProducer:
    def __init__(self, args):
        self.args = args
        self.producer = None
        self.checkpoint_file = os.path.join('checkpoint', f'{args.topic}_checkpoint.json')
        self.stats = {
            'total_records': 0,
            'sent_records': 0,
            'failed_records': 0,
            'start_time': time.time(),
            'last_sent_time': time.time()
        }
    
    def create_producer(self):
        """创建Kafka生产者"""
        try:
            self.producer = KafkaProducer(
                bootstrap_servers=CONFIG['kafka']['bootstrap_servers'],
                acks=CONFIG['kafka']['acks'],
                retries=CONFIG['kafka']['retries'],
                retry_backoff_ms=CONFIG['kafka']['retry_backoff_ms'],
                request_timeout_ms=CONFIG['kafka']['request_timeout_ms'],
                batch_size=CONFIG['kafka']['batch_size'],
                linger_ms=CONFIG['kafka']['linger_ms'],
                buffer_memory=CONFIG['kafka']['buffer_memory'],
                value_serializer=serialize_message
            )
            logger.info('Kafka producer created successfully')
        except Exception as e:
            logger.error(f'Failed to create Kafka producer: {str(e)}')
            raise
    
    def load_checkpoint(self):
        """加载断点续传信息"""
        if os.path.exists(self.checkpoint_file):
            try:
                with open(self.checkpoint_file, 'r') as f:
                    checkpoint = json.load(f)
                logger.info(f'Loaded checkpoint: {checkpoint}')
                return checkpoint.get('offset', 0)
            except Exception as e:
                logger.error(f'Failed to load checkpoint: {str(e)}')
                return 0
        return 0
    
    def save_checkpoint(self, offset):
        """保存断点续传信息"""
        try:
            checkpoint = {'offset': offset, 'timestamp': time.time()}
            with open(self.checkpoint_file, 'w') as f:
                json.dump(checkpoint, f)
            logger.info(f'Saved checkpoint at offset: {offset}')
        except Exception as e:
            logger.error(f'Failed to save checkpoint: {str(e)}')
    
    def send_message(self, message):
        """发送消息到Kafka"""
        max_retries = 3
        retry_delay = 1
        
        for attempt in range(max_retries):
            try:
                future = self.producer.send(self.args.topic, message)
                # 等待发送完成
                future.get(timeout=CONFIG['kafka']['request_timeout_ms'] / 1000)
                self.stats['sent_records'] += 1
                return True
            except Exception as e:
                logger.warning(f'Failed to send message (attempt {attempt + 1}/{max_retries}): {str(e)}')
                if attempt < max_retries - 1:
                    time.sleep(retry_delay)
                    retry_delay *= 2  # 指数退避
                else:
                    self.stats['failed_records'] += 1
                    return False
    
    def process_data(self):
        """处理数据并发送到Kafka"""
        # 加载数据
        logger.info(f'Reading data from: {self.args.file_path}')
        try:
            df = pd.read_parquet(self.args.file_path)
            # 按时间顺序排序
            pickup_col = 'lpep_pickup_datetime' if 'lpep_pickup_datetime' in df.columns else 'tpep_pickup_datetime'
            if pickup_col in df.columns:
                df = df.sort_values(pickup_col)
                logger.info(f'Data sorted by {pickup_col}')
            self.stats['total_records'] = len(df)
            logger.info(f'Total records in file: {self.stats["total_records"]}')
        except Exception as e:
            logger.error(f'Failed to read parquet file: {str(e)}')
            return
        
        # 加载断点
        start_offset = self.load_checkpoint()
        if start_offset >= len(df):
            logger.info('All records have been sent, exiting...')
            return
        
        logger.info(f'Starting from offset: {start_offset}, total records: {self.stats["total_records"]}')
        
        # 处理数据
        for i in range(start_offset, len(df)):
            # 检查是否需要优雅退出
            if hasattr(self, 'stop_flag') and self.stop_flag:
                logger.info(f'Received stop signal, exiting at offset {i}...')
                self.save_checkpoint(i)
                break
            
            # 转换数据为字典
            record = df.iloc[i].to_dict()
            
            # 数据校验
            if not self.validate_record(record):
                logger.warning(f'Skipping invalid record at offset {i}')
                self.stats['failed_records'] += 1
                continue
            
            # 记录发送前的时间戳
            send_time = time.time()
            
            # 发送消息
            if self.send_message(record):
                # 记录发送成功的详细信息
                pickup_col = 'lpep_pickup_datetime' if 'lpep_pickup_datetime' in record else 'tpep_pickup_datetime'
                dropoff_col = 'lpep_dropoff_datetime' if 'lpep_dropoff_datetime' in record else 'tpep_dropoff_datetime'
                pickup_time = record.get(pickup_col, 'N/A')
                dropoff_time = record.get(dropoff_col, 'N/A')
                logger.debug(f'Sent record {i} at {time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(send_time))}, ' 
                           f'pickup: {pickup_time}, dropoff: {dropoff_time}')
                
                # 速率控制
                if self.args.rate > 0:
                    elapsed = time.time() - self.stats['last_sent_time']
                    expected_time = 1.0 / self.args.rate
                    if elapsed < expected_time:
                        time.sleep(expected_time - elapsed)
                    self.stats['last_sent_time'] = time.time()
                
                # 每100条记录打印一次状态
                if (i + 1) % 100 == 0:
                    self.print_stats()
                    # 保存断点
                    self.save_checkpoint(i + 1)
                    logger.info(f'Checkpoint saved at offset {i + 1}')
        
        # 保存最终断点
        self.save_checkpoint(len(df))
        logger.info('All records processed')
        self.print_stats()
    
    def validate_record(self, record):
        """验证记录是否有效"""
        # 基本验证：确保关键字段存在（支持绿色和黄色出租车）
        # 绿色出租车使用 lpep_pickup_datetime，黄色出租车使用 tpep_pickup_datetime
        pickup_col = 'lpep_pickup_datetime' if 'lpep_pickup_datetime' in record else 'tpep_pickup_datetime'
        dropoff_col = 'lpep_dropoff_datetime' if 'lpep_dropoff_datetime' in record else 'tpep_dropoff_datetime'
        
        required_fields = ['VendorID', pickup_col, dropoff_col, 
                          'passenger_count', 'trip_distance', 'PULocationID', 'DOLocationID']
        
        for field in required_fields:
            if field not in record or pd.isna(record[field]):
                return False
        
        return True
    
    def print_stats(self):
        """打印统计信息"""
        elapsed = time.time() - self.stats['start_time']
        if elapsed > 0:
            current_rate = self.stats['sent_records'] / elapsed
            # 计算进度百分比
            progress = (self.stats['sent_records'] / self.stats['total_records']) * 100 if self.stats['total_records'] > 0 else 0
            # 计算预计剩余时间
            if current_rate > 0:
                remaining_records = self.stats['total_records'] - self.stats['sent_records']
                remaining_time = remaining_records / current_rate
            else:
                remaining_time = 0
        else:
            current_rate = 0
            progress = 0
            remaining_time = 0
        
        logger.info(f"Stats: Total={self.stats['total_records']}, Sent={self.stats['sent_records']}, "
                   f"Failed={self.stats['failed_records']}, Rate={current_rate:.2f} records/sec, "
                   f"Elapsed={elapsed:.2f} sec, Progress={progress:.2f}%, "
                   f"Remaining={remaining_time:.2f} sec")
    
    def run(self):
        """运行生产者"""
        # 检查是否存在checkpoint文件
        if os.path.exists(self.checkpoint_file):
            logger.info(f"Checkpoint file found. Will resume from last position.")
            logger.info(f"To start from the beginning, delete: {self.checkpoint_file}")
        else:
            logger.info("No checkpoint file found. Will start from the beginning.")
        
        try:
            self.create_producer()
            self.process_data()
        except KeyboardInterrupt:
            logger.info('Received KeyboardInterrupt, stopping...')
            self.stop_flag = True
        finally:
            if self.producer:
                self.producer.close()
                logger.info('Kafka producer closed')

def main():
    parser = argparse.ArgumentParser(description='Kafka Taxi Trip Producer')
    parser.add_argument('--taxi-type', type=str, choices=['green', 'yellow'], default='green',
                        help='Taxi type: green or yellow (default: green)')
    parser.add_argument('--file-path', type=str, default=None,
                        help='Path to parquet data file (optional, will use default based on taxi-type)')
    parser.add_argument('--topic', type=str, default=None,
                        help='Kafka topic to send messages to (optional, will use default based on taxi-type)')
    parser.add_argument('--rate', type=int, default=10, help='Message send rate (records/second)')
    parser.add_argument('--concurrency', type=int, default=1, help='Number of concurrent producers')
    parser.add_argument('--start-offset', type=int, default=0, help='Starting offset in data file')
    
    args = parser.parse_args()
    
    # 根据出租车类型设置默认值
    if args.taxi_type == 'green':
        if args.file_path is None:
            args.file_path = CONFIG['data']['green_tripdata_path']
        if args.topic is None:
            args.topic = CONFIG['topics']['green_taxi']
    elif args.taxi_type == 'yellow':
        if args.file_path is None:
            args.file_path = CONFIG['data']['yellow_tripdata_path']
        if args.topic is None:
            args.topic = CONFIG['topics']['yellow_taxi']
    
    logger.info(f'Starting Kafka producer for {args.taxi_type} taxi with args: {args}')
    
    producer = KafkaTaxiProducer(args)
    producer.run()

if __name__ == '__main__':
    try:
        main()
    except Exception as e:
        logger.error(f'Fatal error: {str(e)}', exc_info=True)
        print(f'Error: {str(e)}')
        import traceback
        traceback.print_exc()
        sys.exit(1)
