@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0run-rankga.ps1" %*
exit /b %ERRORLEVEL%
