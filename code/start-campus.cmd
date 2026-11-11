@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-campus.ps1" %*
if errorlevel 1 (
  echo.
  echo Startup failed. Read the message above, then press any key to close.
  if /I "%~1"=="-CheckOnly" exit /b 1
  pause >nul
  exit /b 1
)
