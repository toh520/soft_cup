@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-campus.ps1" %*
set "campusExit=%errorlevel%"
echo.
if not "%campusExit%"=="0" echo Setup failed. Read the error above.
if /I not "%~1"=="-CheckOnly" pause
exit /b %campusExit%
