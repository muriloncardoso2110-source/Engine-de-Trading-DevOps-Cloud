@echo off
setlocal
where java >nul 2>nul || (echo Instale JDK 21 e tente novamente. & exit /b 1)
where mvn >nul 2>nul || (echo Instale Apache Maven 3.9+ e tente novamente. & exit /b 1)
cd /d "%~dp0.."
call mvn -B test package || exit /b 1
java -XX:+UseG1GC -XX:MaxGCPauseMillis=10 -Xms256m -Xmx512m -jar target\aurora-exchange-1.0.0.jar
