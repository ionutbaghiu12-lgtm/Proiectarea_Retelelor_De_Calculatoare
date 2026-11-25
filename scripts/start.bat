@echo off
cd /d "%~dp0.."
call mvnw.cmd spring-boot:run
if errorlevel 1 pause
