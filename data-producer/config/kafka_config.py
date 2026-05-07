# Kafka 配置文件

KAFKA_CONFIG = {
    'bootstrap_servers': ['hadoop102:9092', 'hadoop103:9092', 'hadoop104:9092'],
    'acks': 'all',
    'retries': 5,
    'retry_backoff_ms': 1000,
    'request_timeout_ms': 30000,
    'batch_size': 16384,
    'linger_ms': 100,
    'buffer_memory': 33554432
}

# 主题配置
TOPICS = {
    'green_taxi': 'taxi_trip_green',
    'yellow_taxi': 'taxi_trip_yellow'
}

# 数据文件配置
DATA_CONFIG = {
    'green_tripdata_path': 'data/green_tripdata_2025-04.parquet',
    'yellow_tripdata_path': 'data/yellow_tripdata_2025-04.parquet'
}
