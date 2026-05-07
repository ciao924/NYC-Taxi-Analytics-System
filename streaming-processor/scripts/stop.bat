@echo off

echo ========================================
echo 停止 Flink 实时数据消费作业
echo ========================================

rem 获取作业ID
set JOB_NAME=KafkaToMySQL
echo 正在查找作业 %JOB_NAME%...

for /f "tokens=1" %%i in ('flink list ^| findstr "%JOB_NAME%" ^| findstr "RUNNING"') do set JOB_ID=%%i

if "%JOB_ID%" equ "" (
    echo 未找到运行中的 %JOB_NAME% 作业
    pause
    exit /b 1
)

echo 找到作业 ID: %JOB_ID%
echo 正在停止作业...

flink stop %JOB_ID%

if %errorlevel% neq 0 (
    echo 作业停止失败！
    pause
    exit /b 1
)

echo 作业停止成功！
echo ========================================
echo 停止完成
echo ========================================
pause
