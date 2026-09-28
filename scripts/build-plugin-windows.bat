@echo off
cd /d "%~dp0..\plugin"
mvn clean package
copy /Y target\AmigosSMP.jar ..\server\plugins\AmigosSMP.jar
pause
