@echo off

echo ========================================
echo 部署 Flink 实时数据消费作业
echo ========================================

rem 切换到项目根目录
cd /d "%~dp0.."

rem 编译打包
echo 正在编译打包...
mvn clean package

if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)

echo 编译成功！

rem 提交作业
echo 正在提交作业...
flink run -c com.taxi.realtime.KafkaToMySQL target/flink-realtime-consumer-1.0-SNAPSHOT.jar

if %errorlevel% neq 0 (
    echo 作业提交失败！
    pause
    exit /b 1
)

echo 作业提交成功！
echo ========================================
echo 部署完成
echo ========================================
pause
