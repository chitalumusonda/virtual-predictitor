@echo off
title VirtualBet AI - WebApp
echo ========================================================
echo        VirtualBet AI - Windows WebApp Launcher
echo ========================================================
echo Starting local webapp server at http://127.0.0.1:8766 ...
start "" "http://127.0.0.1:8766"
py -3 app.py || python3 app.py || python app.py
if errorlevel 1 (
    echo.
    echo [ERROR] Python 3 was not found on your system.
    echo Please install Python 3.9+ from https://www.python.org/downloads/
    echo Make sure to check "Add Python to PATH" during installation.
    pause
)
