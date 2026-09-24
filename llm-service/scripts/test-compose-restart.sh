#!/usr/bin/env bash
# ==============================================================================
# test-compose-restart.sh — Prueba automatizada de reinicio de Compose (H06 / T4)
# Traza: CA3 (persistencia tras reinicio) y CA6 (fallo bloqueante ante pérdida)
#        + H07·CA5 (el registro de idempotencia de eventos Kafka persiste tras el reinicio)
# ==============================================================================
set -euo pipefail

# gateway-mock (Nginx, puerto 8080): simula el borde del Gateway, inyecta la identidad docente y enruta /api/courses al courses-mock.
# Desde la integración main→dev, crear un golden set valida la pertenencia al curso contra courses-service: sin courses-mock
# el servicio responde 503 "Courses no está disponible". El frontend Angular del workbench no se levanta.
BASE_URL="${BASE_URL:-http://localhost:8080}"
HEALTH_URL="${HEALTH_URL:-http://localhost:8087}"   # actuator en el puerto de management
PROJECT_NAME="${PROJECT_NAME:-llm-s1-restart-test}"
SKIP_COMPOSE_MANAGE="${SKIP_COMPOSE_MANAGE:-false}"

COMPOSE=(docker compose -f compose.yaml -f compose.workbench.yaml -f compose.debug.yaml -p "$PROJECT_NAME")
SERVICES=(llm-service gateway-mock courses-mock)

# La clave AES de credenciales es obligatoria (compose.yaml); si no viene del entorno ni de .env, se genera una descartable.
export LLM_CREDENTIALS_MASTER_KEY="${LLM_CREDENTIALS_MASTER_KEY:-$(openssl rand -base64 32)}"

COURSE_ID="22222222-2222-2222-2222-222222222222"
TEACHER_ID="11111111-1111-1111-1111-111111111111"

echo "=================================================================="
echo " [H06·T4] Iniciando prueba automatizada de reinicio de Compose"
echo " Traza: CA3 (persistencia de datos) / CA6 (detección de pérdida)"
echo "=================================================================="

wait_for_health() {
  local max_retries=30
  local count=0
  echo -n "Esperando salud del servicio ($HEALTH_URL/actuator/health)... "
  until curl -s -f "$HEALTH_URL/actuator/health" | grep -q '"status":"UP"'; do
    sleep 2
    count=$((count + 1))
    if [ "$count" -ge "$max_retries" ]; then
      echo " ERROR: El servicio no alcanzó estado UP en tiempo y forma."
      return 1
    fi
    echo -n "."
  done
  echo " UP!"
}

cleanup() {
  if [ "$SKIP_COMPOSE_MANAGE" != "true" ]; then
    echo "Limpiando entorno Compose de prueba..."
    "${COMPOSE[@]}" down --volumes --remove-orphans || true
  fi
}
trap cleanup EXIT

if [ "$SKIP_COMPOSE_MANAGE" != "true" ]; then
  echo "1. Levantando stack con Docker Compose (proyecto: $PROJECT_NAME)..."
  "${COMPOSE[@]}" up -d --build --wait "${SERVICES[@]}"
fi

wait_for_health

COMMON_HEADERS=(
  -H "Content-Type: application/json"
  -H "X-Principal-Type: service"
  -H "X-Service-Id: admin-service"
  -H "X-Service-Scopes: llm.golden-set.manage"
  -H "X-Delegated-User: $TEACHER_ID"
  -H "X-Actor-Id: $TEACHER_ID"
  -H "X-User-Roles: TEACHER"
  -H "X-Teacher-Course-Ids: $COURSE_ID"
  -H "X-Request-Id: h06-restart-test-req"
  -H "traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
)

echo "2. Creando Golden Set de referencia para el curso $COURSE_ID..."
CREATE_RESP=$(curl -s -f -X POST "${COMMON_HEADERS[@]}" \
  -d '{"name":"Banco de referencia S1 - Prueba de reinicio"}' \
  "$BASE_URL/api/llm/courses/$COURSE_ID/golden-sets")

VERSION_ID=$(echo "$CREATE_RESP" | grep -o '"id":"[^"]*' | head -n1 | cut -d'"' -f4)
if [ -z "$VERSION_ID" ]; then
  echo "ERROR: No se pudo obtener el versionId del Golden Set creado."
  echo "Respuesta del servicio: $CREATE_RESP"
  exit 1
fi
echo "   Golden Set creado exitosamente. VersionId: $VERSION_ID"

echo "3. Cargando caso de referencia en el Golden Set..."
CASE_PAYLOAD='{
  "transcript": [
    {
      "role": "STUDENT",
      "content": "¿Cómo calculo la complejidad temporal de una búsqueda binaria?"
    }
  ],
  "challengeContext": {
    "statement": "Explicar complejidad logarítmica",
    "expectedKeyPoints": ["división a la mitad", "O(log n)"]
  },
  "author": "Docente Titular S1",
  "referenceScores": {
    "AUTONOMY": 85,
    "CLARITY": 90,
    "PROGRESSION": 88,
    "COMPLIANCE": 92,
    "EFFICIENCY": 85
  },
  "scoreJustifications": {
    "AUTONOMY": "El estudiante resolvió de forma independiente"
  }
}'

curl -s -f -X POST "${COMMON_HEADERS[@]}" \
  -d "$CASE_PAYLOAD" \
  "$BASE_URL/api/llm/courses/$COURSE_ID/golden-sets/$VERSION_ID/cases" > /dev/null
echo "   Caso de referencia insertado correctamente."

echo "4. Consultando el Golden Set antes de reiniciar..."
LIST_BEFORE=$(curl -s -f -X GET "${COMMON_HEADERS[@]}" \
  "$BASE_URL/api/llm/courses/$COURSE_ID/golden-sets")

if ! echo "$LIST_BEFORE" | grep -q "$VERSION_ID"; then
  echo "ERROR: El Golden Set no aparece en la lista antes del reinicio."
  exit 1
fi
echo "   Confirmado: Golden Set presente previo al reinicio."

EVENT_ID="$(cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen | tr 'A-Z' 'a-z')"
psql_llm() {
  "${COMPOSE[@]}" exec -T postgres psql -U llm -d llm -tAc "$1"
}
if [ "$SKIP_COMPOSE_MANAGE" != "true" ]; then
  echo "4b. [H07·CA5] Registrando eventId $EVENT_ID en kafka_consumed_events antes del reinicio..."
  psql_llm "insert into llm.kafka_consumed_events (event_id, topic, event_type, consumer_group) values ('$EVENT_ID', 'practice-events', 'ATTEMPT_CLOSED', 'llm-service')" > /dev/null
fi

if [ "$SKIP_COMPOSE_MANAGE" != "true" ]; then
  echo "5. Ejecutando reinicio de Compose (docker compose restart)..."
  "${COMPOSE[@]}" restart
  echo "   Reinicio completado. Verificando recuperación del servicio..."
  wait_for_health
else
  echo "5. [Modo externo] Omitiendo restart de contenedores por configuración SKIP_COMPOSE_MANAGE."
fi

echo "6. Consultando el Golden Set tras el reinicio del entorno..."
LIST_AFTER=$(curl -s -f -X GET "${COMMON_HEADERS[@]}" \
  "$BASE_URL/api/llm/courses/$COURSE_ID/golden-sets")

# Validación CA3 y CA6 (si falta el dato, falla y bloquea la Review)
if ! echo "$LIST_AFTER" | grep -q "$VERSION_ID"; then
  echo "=================================================================="
  echo " [FALLO CRÍTICO CA6] El Golden Set $VERSION_ID NO persiste tras el reinicio."
  echo " Se detectó pérdida de datos en la base. Bloqueando Review de S1."
  echo "=================================================================="
  exit 1
fi

if [ "$SKIP_COMPOSE_MANAGE" != "true" ]; then
  echo "7. [H07·CA5] Verificando que el registro de idempotencia persiste tras el reinicio..."
  FOUND="$(psql_llm "select count(*) from llm.kafka_consumed_events where event_id = '$EVENT_ID'")"
  if [ "$FOUND" != "1" ]; then
    echo " [FALLO H07·CA5] El eventId $EVENT_ID NO persiste tras el reinicio: se reprocesarían eventos duplicados."
    exit 1
  fi
  echo "   Confirmado: el eventId sigue registrado (dedup intacta)."
fi

echo "=================================================================="
echo " [ÉXITO CA3 & CA6] El banco de casos de referencia persistió intacto."
echo " Prueba automatizada de reinicio superada en verde."
echo "=================================================================="
exit 0
