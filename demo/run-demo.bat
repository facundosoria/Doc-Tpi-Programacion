@echo off
echo ==============================================================================
echo   Iniciando Suite de Demo - Tema 07: Evaluacion LLM ^& Tutor Socratico
echo   UTN FRC - Programacion IV
echo ==============================================================================
echo.

cd /d "%~dp0"

echo [+] Verificando Docker...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker no esta instalado o no esta en el PATH.
    pause
    exit /b 1
)

echo [+] Levantando contenedores con Docker Compose...
docker compose up --build -d

if %errorlevel% neq 0 (
    echo [ERROR] Ocurrio un error al levantar los contenedores.
    pause
    exit /b 1
)

echo.
echo ==============================================================================
echo   ^> Demo Frontend: http://localhost:3000
echo   ^> Backend REST:  http://localhost:8087
echo   ^> Para ver logs: docker compose logs -f
echo   ^> Para detener:  docker compose down
echo ==============================================================================
echo.

pause
