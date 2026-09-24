#!/usr/bin/env bash
set -euo pipefail

project_name="llm-s1-smoke"
mode="${1:-}"

# compose.yaml fija container_name y nombra las redes: sin este override el smoke choca con cualquier
# contenedor/red de otro proyecto (p. ej. `llm-service`) y exige que exista la red externa `tpi-platform`.
# Todo lo que crea el smoke lleva el prefijo del proyecto y se borra al salir.
override="$(mktemp)"
cat > "$override" <<EOF
services:
  llm-service:
    container_name: ${project_name}-app
networks:
  default:
    name: ${project_name}-net
  tpi-platform:
    name: ${project_name}-platform
    external: false
EOF

# La clave AES de credenciales es obligatoria (compose.yaml); si no viene del entorno o de .env, se genera una descartable.
export LLM_CREDENTIALS_MASTER_KEY="${LLM_CREDENTIALS_MASTER_KEY:-$(openssl rand -base64 32)}"

dc() { docker compose -p "$project_name" -f compose.yaml -f "$override" "$@"; }

cleanup() { dc down --volumes --remove-orphans; rm -f "$override"; }
trap cleanup EXIT

if [[ "$mode" == "--cold" ]]; then
  echo "[T7] Modo --cold: eliminando imágenes/volúmenes previos de '$project_name' antes de medir."
  dc down --rmi all --volumes --remove-orphans || true
fi

mvn -q -DskipTests package

start_ts=$(date +%s)
dc up --build --wait
end_ts=$(date +%s)
elapsed=$((end_ts - start_ts))

dc exec -T llm-service wget -qO- http://localhost:8087/actuator/health | grep -q '"status":"UP"'
echo "Docker Compose S1 smoke test: OK"
if [[ "$mode" == "--cold" ]]; then
  echo "[T7] Tiempo de arranque en frío (docker compose up --build --wait): ${elapsed}s"
else
  echo "[T7] Tiempo de arranque (docker compose up --build --wait): ${elapsed}s (usar --cold para medir en frío)"
fi
