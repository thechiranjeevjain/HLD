@echo off
setlocal
cd /d "%~dp0.."
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
if not exist "frontend\node_modules" call npm.cmd --prefix frontend install || exit /b 1
call npm.cmd --prefix frontend run build || exit /b 1
call mvn.cmd -q package -DskipTests || exit /b 1
echo Starting at http://localhost:8095
java -jar target\stripe-ledger-reconciliation-1.0.0.jar

