@echo off
cd /d "%~dp0.."
call mvn -B -q package -DskipTests || exit /b 1
java -XX:+UseG1GC -XX:MaxGCPauseMillis=10 -Xms512m -Xmx1g -jar target\aurora-exchange-1.0.0.jar benchmark 100000 10
