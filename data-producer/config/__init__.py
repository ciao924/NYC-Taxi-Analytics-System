# 配置管理模块
import os
from dotenv import load_dotenv
from .kafka_config import KAFKA_CONFIG, TOPICS, DATA_CONFIG

# 加载环境变量
load_dotenv()

# 从环境变量覆盖配置
def get_config():
    config = {
        'kafka': KAFKA_CONFIG.copy(),
        'topics': TOPICS.copy(),
        'data': DATA_CONFIG.copy()
    }
    
    # 从环境变量读取Kafka配置
    kafka_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS')
    if kafka_servers:
        config['kafka']['bootstrap_servers'] = kafka_servers.split(',')
    
    # 从环境变量读取主题配置
    green_topic = os.getenv('KAFKA_TOPIC_GREEN')
    if green_topic:
        config['topics']['green_taxi'] = green_topic
    
    yellow_topic = os.getenv('KAFKA_TOPIC_YELLOW')
    if yellow_topic:
        config['topics']['yellow_taxi'] = yellow_topic
    
    # 从环境变量读取数据文件路径
    data_path = os.getenv('GREEN_TRIPDATA_PATH')
    if data_path:
        config['data']['green_tripdata_path'] = data_path
    
    yellow_data_path = os.getenv('YELLOW_TRIPDATA_PATH')
    if yellow_data_path:
        config['data']['yellow_tripdata_path'] = yellow_data_path
    
    return config

# 导出配置
CONFIG = get_config()
