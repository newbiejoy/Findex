@echo off
title Findex - Local Document Search
cd /d "%~dp0"

curl -s http://localhost:8080/api/health >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [Findex] Starting Search Backend Engine...
    if exist "backend\target\findex-engine-0.0.1-SNAPSHOT.jar" (
        start /b java -jar backend\target\findex-engine-0.0.1-SNAPSHOT.jar
    ) else (
        start /b cmd /c "cd backend && .\mvnw.cmd spring-boot:run"
    )
    timeout /t 4 /nobreak >nul
)

echo [Findex] Launching Desktop Window...
if exist "frontend\dist-win\Findex-win32-x64\Findex.exe" (
    start "" "frontend\dist-win\Findex-win32-x64\Findex.exe"
) else (
    cd frontend
    npm run desktop
)
