#!/usr/bin/env python3
import sys
import os
import pandas as pd
from config.kafka_config import DATA_CONFIG

def check_parquet_file():
    print("Checking parquet data file...")
    
    file_path = DATA_CONFIG['green_tripdata_path']
    absolute_path = os.path.join(os.getcwd(), file_path)
    
    print(f"File path: {absolute_path}")
    
    # 检查文件是否存在
    if not os.path.exists(absolute_path):
        print(f"✗ File does not exist: {absolute_path}")
        return False
    
    print("✓ File exists")
    
    try:
        # 读取前10行数据进行测试
        df = pd.read_parquet(absolute_path, nrows=10)
        print(f"✓ Successfully read parquet file")
        print(f"✓ Total columns: {len(df.columns)}")
        print(f"✓ Sample rows: {len(df)}")
        
        # 显示列名和数据类型
        print("\nColumns and data types:")
        for col, dtype in df.dtypes.items():
            print(f"  {col}: {dtype}")
        
        # 显示前5行数据
        print("\nSample data:")
        print(df.head())
        
        # 检查数据完整性
        print("\nData integrity check:")
        print(f"  Total null values: {df.isnull().sum().sum()}")
        print(f"  Null values per column:")
        null_counts = df.isnull().sum()
        for col, count in null_counts.items():
            if count > 0:
                print(f"    {col}: {count}")
        
        print("\n🎉 All parquet file checks passed!")
        return True
        
    except Exception as e:
        print(f"✗ Error reading parquet file: {str(e)}")
        return False

if __name__ == "__main__":
    success = check_parquet_file()
    sys.exit(0 if success else 1)
