@echo off
REM 后台运行Kafka生产者脚本

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..\

cd /d %PROJECT_DIR%

REM 启动生产者，速率设置为10条/秒
python scripts\kafka_producer.py --rate 10
