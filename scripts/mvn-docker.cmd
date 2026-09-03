@echo off
setlocal
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0mvn-docker.ps1" %*
exit /b %ERRORLEVEL%
