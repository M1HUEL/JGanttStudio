@echo off
setlocal
cd /d "%~dp0"
java --enable-native-access=ALL-UNNAMED -jar "gantt-bootstrap\target\jganttstudio.jar" %*
if errorlevel 1 pause