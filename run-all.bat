@echo off
TITLE SchemeBridge Microservices Master Launcher
echo =========================================================================
echo             SchemeBridge Microservices - Master Launcher
echo =========================================================================
echo.

:: 1. Check if MongoDB is running on port 27017
echo [1/4] Checking MongoDB service on port 27017...
netstat -ano | findstr 27017 >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [!] Starting local MongoDB instance...
    if exist "C:\Program Files\MongoDB\Server\8.3\bin\mongod.exe" (
        start /B "" "C:\Program Files\MongoDB\Server\8.3\bin\mongod.exe" --dbpath "%~dp0mongodb_data" >nul 2>&1
        echo [+] MongoDB started.
        ping -n 4 127.0.0.1 >nul
    ) else (
        echo [WARNING] mongod.exe not found at standard path. Please ensure MongoDB is running!
    )
) else (
    echo [+] MongoDB is already running on port 27017.
)

echo.
echo [2/4] Starting Config Server (Port 8888)...
start /B "" /d "%~dp0schemebridge-microservices" cmd /c "mvn spring-boot:run -pl config-server"
ping -n 12 127.0.0.1 >nul

echo [3/4] Starting Service Registry (Port 8761)...
start /B "" /d "%~dp0schemebridge-microservices" cmd /c "mvn spring-boot:run -pl service-registry"
ping -n 10 127.0.0.1 >nul

echo [4/4] Starting API Gateway, Business Microservices & React Frontend...
start /B "" /d "%~dp0schemebridge-microservices" cmd /c "mvn spring-boot:run -pl api-gateway"
ping -n 4 127.0.0.1 >nul

start /B "" /d "%~dp0schemebridge-microservices" cmd /c "mvn spring-boot:run -pl auth-service"
start /B "" /d "%~dp0schemebridge-microservices" cmd /c "mvn spring-boot:run -pl core-service"
start /B "" /d "%~dp0schemebridge-microservices" cmd /c "mvn spring-boot:run -pl notification-service"
start /B "" /d "%~dp0schemebridge-microservices" cmd /c "mvn spring-boot:run -pl admin-service"

start /B "" /d "%~dp0schemeBridge-frontend" cmd /c "npm run dev"

echo.
echo [+] All microservices and frontend started cleanly in background!
echo [+] Service Registry: http://localhost:8761
echo [+] API Gateway:       http://localhost:8080
echo [+] Core Service:      http://localhost:8082
echo [+] React Frontend:   http://localhost:5173
echo.
