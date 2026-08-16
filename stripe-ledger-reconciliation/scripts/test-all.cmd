@echo off
setlocal
cd /d "%~dp0.."
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call npm.cmd --prefix frontend install || exit /b 1
call npm.cmd --prefix frontend run build || exit /b 1
call mvn.cmd clean verify || exit /b 1

