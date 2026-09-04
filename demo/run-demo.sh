#!/usr/bin/env bash
# ==============================================================================
# Script de inicio para la Demo Integral - Tema 07 (Linux / macOS / Git Bash)
# ==============================================================================

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

echo "=============================================================================="
echo "  Iniciando Suite de Demo - Tema 07: Evaluación LLM & Tutor Socrático"
echo "  UTN FRC - Programación IV"
echo "=============================================================================="
echo ""

echo "[+] Verificando Docker..."
if ! command -v docker &> /dev/null; then
    echo "[ERROR] Docker no está instalado o no se encuentra en el PATH."
    exit 1
fi

echo "[+] Levantando contenedores con Docker Compose..."
docker compose up --build -d

echo ""
echo "=============================================================================="
echo "  > Demo Frontend: http://localhost:3000"
echo "  > Backend REST:  http://localhost:8087"
echo "  > Para ver logs: docker compose logs -f"
echo "  > Para detener:  docker compose down"
echo "=============================================================================="
echo ""
