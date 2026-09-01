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

# El nombre de usuario se usa en dos lados --la ruta del directorio remoto y el
# registro que queda en el buzon-- asi que se resuelve una sola vez. En Git Bash
# puede venir como DOMINIO+usuario, y en el runner el que importa es quien pusheo,
# no el usuario del servicio.
#
# Si ya viene definido, se respeta: es el caso de `--remoto`, donde el que importa
# es quien lo lanzo desde su maquina y no la cuenta con la que entro por SSH --que
# es compartida, asi que sin esto todas las corridas del equipo se verian como la
# misma persona.
QA_USUARIO="${QA_USUARIO:-$(printf '%s' "${GITHUB_ACTOR:-$(id -un)}" | sed 's/[^A-Za-z0-9_.-]/-/g')}"

# --remoto: el gate no corre aca, corre en el server del equipo.
#
# Existe porque el runner self-hosted solo sirve mientras el repo sea nuestro:
# darlo de alta necesita permisos de administrador sobre el repositorio, y un
# runner en un repo compartido con otros equipos le daria ejecucion en nuestro
# server a cualquiera que pueda abrir un pull request. Esto no necesita permiso
# de nadie en GitHub, y el server queda expuesto solo a quien tenga SSH.
#
# NO toca el push ni git: manda lo que tenes en el working tree --incluido lo que
# todavia no commiteaste-- y te devuelve el mismo resumen que verias corriendolo
# local. Por eso va antes de cualquier llamada a Docker: el sentido es que
# funcione en una maquina que no puede correr el contenedor.
REMOTO=0
ARGUMENTOS=()
for argumento in "$@"; do
  if [ "$argumento" = "--remoto" ]; then
    REMOTO=1
  else
    ARGUMENTOS+=("$argumento")
  fi
done
set -- ${ARGUMENTOS[@]+"${ARGUMENTOS[@]}"}

if [ "$REMOTO" = "1" ]; then
  if [ -z "${QA_REMOTO:-}" ]; then
    echo "qa: --remoto necesita saber a donde conectarse." >&2
    echo "    export QA_REMOTO=usuario@servidor" >&2
    echo "    export QA_REMOTO_PUERTO=2222    # opcional, default 22" >&2
    exit 2
  fi

  # El nombre de usuario entra en una ruta del server, asi que no puede venir vacio.
  if [ -z "$QA_USUARIO" ]; then
    echo "qa: no pude resolver tu nombre de usuario para armar la ruta remota." >&2
    exit 2
  fi

  QA_PUERTO="${QA_REMOTO_PUERTO:-22}"
  QA_BASE_REMOTA="${QA_REMOTO_DIR:-/opt/TP-Pipelines/remoto}"
  QA_DESTINO="$QA_BASE_REMOTA/$QA_USUARIO"

  # El buzon de corridas del server, hermano de remoto/. Va como default y no como
  # variable obligatoria: si el directorio no existe alla, el registro no se
  # escribe y la corrida sigue igual.
  QA_BUZON_REMOTO="${QA_SPOOL_REMOTO:-/opt/TP-Pipelines/corridas}"
  export MSYS_NO_PATHCONV=1

  # Un directorio y un volumen de Maven por persona: seis personas corriendo a la
  # vez no se pisan, por el mismo motivo que cada runner tiene el suyo.
  QA_ARGS=""
  for argumento in "$@"; do QA_ARGS="$QA_ARGS $(printf '%q' "$argumento")"; done

  echo "qa: mandando el working tree a $QA_REMOTO ($QA_DESTINO)" >&2
  tar czf - --exclude=./.qa . | ssh -p "$QA_PUERTO" "$QA_REMOTO" "rm -rf '$QA_DESTINO' && mkdir -p '$QA_DESTINO' && tar xzf - -C '$QA_DESTINO'"

  # El codigo de salida es el del gate, no el de ssh: se captura a mano porque
  # con `set -e` un rojo del gate abortaria el script antes de traer el resumen.
  # CI=1 a proposito, aunque esto no sea el CI: la copia del server es tan efimera
  # como el working tree de un runner. Sin esto, un chequeo en `arregla` correria
  # spotless:apply sobre archivos que se descartan al terminar, y la corrida
  # quedaria en verde sin que el arreglo llegue nunca a tu maquina: ni protesta ni
  # arregla. Con CI=1, degradacion_ci lo convierte en `bloquea` y te lo reporta.
  QA_CODIGO=0
  ssh -p "$QA_PUERTO" "$QA_REMOTO" "cd '$QA_DESTINO' && CI=1 QA_BASE='${QA_BASE:-}' QA_M2_VOLUME='tpi-qa-m2-remoto-$QA_USUARIO' QA_SPOOL='$QA_BUZON_REMOTO' QA_ORIGEN=remoto QA_USUARIO='$QA_USUARIO' bash qa.sh$QA_ARGS" || QA_CODIGO=$?

  # El resumen vuelve, para que --remoto y local se lean exactamente igual.
  ssh -p "$QA_PUERTO" "$QA_REMOTO" "cd '$QA_DESTINO' && tar czf - .qa 2>/dev/null" | tar xzf - 2>/dev/null || true

  exit "$QA_CODIGO"
fi

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

# El buzon de corridas es una ruta del HOST, igual que el step summary, y se monta
# por el mismo motivo. Con dos diferencias:
#
# Adentro entra siempre en /qa-buzon. La ruta del host la elige quien administra el
# server y no tiene por que existir dentro del contenedor; el step summary se monta
# en su ruta exacta porque ahi la eligio el runner y reportar.py tiene que abrir
# ESA. Aca la elegimos nosotros, asi que conviene fija: tambien la hace montable
# desde Windows, donde la ruta del host ni siquiera es de Unix.
#
# Y se exige que el directorio YA EXISTA. Si no, Docker lo crearia con el montaje y
# como root, y el contenedor --que corre con tu UID-- no podria escribir adentro:
# quedaria un directorio inutil en el server que nadie pidio.
#
# Si no esta, no pasa nada: en tu maquina la variable no esta definida y `./qa.sh`
# se comporta igual que siempre.
BUZON=()
if [ -n "${QA_SPOOL:-}" ] && [ -d "${QA_SPOOL}" ]; then
  if command -v cygpath >/dev/null 2>&1; then
    BUZON_HOST="$(cygpath -w "$QA_SPOOL")"
  else
    BUZON_HOST="$QA_SPOOL"
  fi
  BUZON=(-v "${BUZON_HOST}:/qa-buzon" -e QA_SPOOL=/qa-buzon)
fi

# En Linux el contenedor corre como root, y todo lo que escribe en el working
# tree --.qa/, __pycache__, y los target/ que deja Maven-- queda con dueno root.
# En Windows y macOS no importa, porque el montaje traduce los dueños. En el
# runner si: el checkout de la corrida siguiente corre como el usuario del runner,
# no puede borrar nada de eso, y el job muere en dos segundos sin llegar a
# ejecutar el gate. El CI se rompia solo cada dos corridas.
#
# Por eso en Linux corremos con tu UID. Como /root/.m2 deja de ser escribible, el
# repositorio Maven se mueve a /qa-m2 por variable de entorno: la imagen no se
# toca, asi que el tag --que es el hash del Dockerfile-- no cambia y nadie tiene
# que reconstruir nada.
VOLUMEN_M2="${QA_M2_VOLUME:-tpi-qa-m2}"
IDENTIDAD=()
DESTINO_M2=/root/.m2
if [ "$(uname -s)" = "Linux" ]; then
  UID_GID="$(id -u):$(id -g)"
  DESTINO_M2=/qa-m2
  # MAVEN_CONFIG tambien se mueve: el entrypoint de la imagen de Maven tiene
  # /root/.m2 fijo y, sin usuario root, protesta con "Wrong volume permissions?"
  # en cada corrida. No rompe nada, pero es ruido en todos los logs del CI.
  IDENTIDAD=(
    --user "$UID_GID"
    -e HOME=/tmp
    -e MAVEN_CONFIG=/qa-m2
    -e MAVEN_OPTS="-Dmaven.repo.local=/qa-m2/repository"
  )
  # Un volumen nuevo nace con dueno root: sin esto el usuario no podria escribirlo.
  # Va con --entrypoint para saltear el de la imagen de Maven y no depender de
  # como un shell interprete las comillas.
  docker volume create "$VOLUMEN_M2" >/dev/null
  docker run --rm -v "$VOLUMEN_M2:/qa-m2" --entrypoint chown "$IMAGE" -R "$UID_GID" /qa-m2
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
  -v "${VOLUMEN_M2}:${DESTINO_M2}" \
  -w /work \
  "${RESUMEN_CI[@]}" \
  "${IDENTIDAD[@]}" \
  "${BUZON[@]}" \
  -e QA_BASE="${QA_BASE:-}" \
  -e CI="${CI:-}" \
  -e QA_ORIGEN="${QA_ORIGEN:-}" \
  -e QA_USUARIO="${QA_USUARIO}" \
  -e GITHUB_STEP_SUMMARY="${GITHUB_STEP_SUMMARY:-}" \
  "$IMAGE" bash tools/qa/run.sh "$@"
