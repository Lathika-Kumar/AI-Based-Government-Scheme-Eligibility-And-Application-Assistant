@echo off
TITLE SchemeBridge Backend Launcher
echo =========================================================================
echo                 SchemeBridge Backend - Startup Script
echo =========================================================================
echo.

:: 1. Check if MongoDB is running on port 27017
echo [1/3] Checking MongoDB service on port 27017...
netstat -ano | findstr 27017 >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [!] MongoDB is not running on port 27017. Starting local MongoDB instance...
    if exist "C:\Program Files\MongoDB\Server\8.3\bin\mongod.exe" (
        start "MongoDB Server" /min "C:\Program Files\MongoDB\Server\8.3\bin\mongod.exe" --dbpath "%~dp0mongodb_data"
        echo [+] MongoDB started using dbpath: %~dp0mongodb_data
        timeout /t 3 /nobreak >nul
    ) else (
        echo [WARNING] mongod.exe not found at standard path. Please ensure MongoDB is running!
    )
) else (
    echo [+] MongoDB is already running on port 27017.
)

echo.
echo [2/3] Building and Starting Spring Boot Backend (schemebridge-backend)...
echo [+] Target Port: http://localhost:8080
echo [+] Swagger UI will automatically open in your browser at:
echo     http://localhost:8080/swagger-ui/index.html
echo.

cd /d "%~dp0"
call "%~dp0run-all.bat"

pause
