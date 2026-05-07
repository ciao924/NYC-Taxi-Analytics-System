@echo off

echo ========================================
echo 查询 Flink 实时数据消费作业状态
echo ========================================

set JOB_NAME=KafkaToMySQL
echo 正在查询作业 %JOB_NAME% 的状态...

flink list | findstr "%JOB_NAME%"

if %errorlevel% neq 0 (
    echo 未找到作业 %JOB_NAME%
    pause
    exit /b 1
)

echo ========================================
echo 查询完成
echo ========================================
pause
