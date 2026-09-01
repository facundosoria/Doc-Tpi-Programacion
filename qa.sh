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

# GITHUB_STEP_SUMMARY es una ruta del HOST, fuera de /work. Sin montarla,
# reportar.py la abre adentro del contenedor, no existe, y la corrida muere con
# FileNotFoundError aunque el gate haya pasado: el verde o rojo terminaba
# dependiendo de un archivo que nadie podia escribir. Montamos su carpeta en la
# misma ruta para que el resumen llegue a la pagina del run.
RESUMEN_CI=()
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  RESUMEN_CI=(-v "$(dirname "$GITHUB_STEP_SUMMARY"):$(dirname "$GITHUB_STEP_SUMMARY")")
fi

# Cada runner self-hosted necesita su PROPIO repositorio Maven. El volumen es
# unico por host, asi que dos corridas concurrentes en el mismo server comparten
# /root/.m2: descargas parciales, .lastUpdated y contencion de locks, que fallan
# de forma intermitente y dificil de atribuir. QA_M2_VOLUME lo separa por runner.
# En tu maquina no cambia nada: sin la variable, el volumen es el de siempre.

# CI se propaga para que el motor sepa que el working tree es efimero: alla un
# nivel `arregla` corregiria archivos que se descartan al terminar la corrida.
# Ver degradacion_ci en tools/qa/config/checks.yml.
exec docker run --rm -i \
  -v "${HOST_ROOT}:/work" \
  -v "${QA_M2_VOLUME:-tpi-qa-m2}:/root/.m2" \
  -w /work \
  "${RESUMEN_CI[@]}" \
  -e QA_BASE="${QA_BASE:-}" \
  -e CI="${CI:-}" \
  -e GITHUB_STEP_SUMMARY="${GITHUB_STEP_SUMMARY:-}" \
  "$IMAGE" bash tools/qa/run.sh "$@"
