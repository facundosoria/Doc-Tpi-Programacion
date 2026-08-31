#!/usr/bin/env bash
# Punto de entrada del gate de calidad.
# Wrapper delgado: resuelve la raiz del repo, garantiza la imagen y delega en run.sh.
# Toda la logica vive en tools/qa/ para que extraer el gate sea mover una carpeta.
#
# Verifica y reporta. NO toca git: el push lo hacen las personas.
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

QA_DIR="tools/qa"

# El tag es el hash del Dockerfile: si cambia, se reconstruye sola para todos.
TAG="$(git hash-object "$QA_DIR/Dockerfile" | cut -c1-12)"
IMAGE="tpi-qa:$TAG"

if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
  echo "Construyendo $IMAGE (primera vez, puede tardar unos minutos)..." >&2
  docker build -t "$IMAGE" -f "$QA_DIR/Dockerfile" "$QA_DIR" >&2
fi

# En Git Bash hay que pasarle a Docker una ruta de Windows y frenar el
# mangling de MSYS sobre las rutas del contenedor.
if command -v cygpath >/dev/null 2>&1; then
  HOST_ROOT="$(cygpath -w "$ROOT")"
  export MSYS_NO_PATHCONV=1
else
  HOST_ROOT="$ROOT"
fi

exec docker run --rm -i \
  -v "${HOST_ROOT}:/work" \
  -v tpi-qa-m2:/root/.m2 \
  -w /work \
  -e QA_BASE="${QA_BASE:-}" \
  -e GITHUB_STEP_SUMMARY="${GITHUB_STEP_SUMMARY:-}" \
  "$IMAGE" bash tools/qa/run.sh "$@"
