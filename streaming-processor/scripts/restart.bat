@echo off

echo ========================================
echo 重启 Flink 实时数据消费作业
echo ========================================

rem 停止作业
call "%~dp0stop.bat"

if %errorlevel% neq 0 (
    echo 停止作业失败，无法重启
    pause
    exit /b 1
)

rem 等待一段时间确保作业完全停止
echo 等待作业停止...
timeout /t 5 /nobreak > nul

rem 部署作业
call "%~dp0deploy.bat"

if %errorlevel% neq 0 (
    echo 部署作业失败
    pause
    exit /b 1
)

echo ========================================
        echo 重启完成
echo ========================================
pause
