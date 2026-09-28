@echo off
cd /d "%~dp0..\server"
java -Xms2G -Xmx4G -jar purpur.jar --nogui
pause
