#!/usr/bin/env bash
# Prueba el evaluador de llm-service con el bot (proveedor "fake"), por Kafka.
# Es la misma tanda de casos de la seccion 6 de
# docs/contracts/equipos/llm-service-contrato-para-desafios-practicos.md.
#
# Uso (con el compose de llm-service arriba; usa el broker local `kafka-local` de ese mismo compose):
#   bash scripts/probar-bot-evaluador.sh
#
# Opcional: KAFKA_CONTAINER=<contenedor del broker>  (por defecto, el primero con imagen apache/kafka)
#           PG_CONTAINER=<contenedor de Postgres>    (por defecto, el primero con imagen postgres/pgvector)
#
# Notas:
#  - Publica y lee con los scripts de consola del contenedor del broker; no necesita puertos en el host.
#  - Los mensajes rechazados no van a un topic .dlt (los grupos no pueden crear topics): quedan en la tabla
#    llm.event_dead_letter, que se lee con psql dentro del contenedor de Postgres (PG_CONTAINER).
#  - Cada corrida usa ids nuevos, asi que se puede repetir contra el mismo broker sin contaminarse.
#  - No prueba SCORE_DEFERRED (el bot no falla solo: hay que forzar el fallo del evaluador).
#  - Tarda un par de minutos: lee los topics con timeout y espera para comprobar que NO llegan mensajes.
#  - Sale con codigo distinto de cero si algun caso falla.
set -u

KAFKA_CONTAINER="${KAFKA_CONTAINER:-$(docker ps --format '{{.Names}} {{.Image}}' | awk '$2 ~ /^apache\/kafka/ {print $1; exit}')}"
[ -n "$KAFKA_CONTAINER" ] || { echo "FALLO no encuentro un contenedor de Kafka (definir KAFKA_CONTAINER)"; exit 2; }
BS=localhost:29092
PG_CONTAINER="${PG_CONTAINER:-$(docker ps --format '{{.Names}} {{.Image}}' | awk '$2 ~ /postgres|pgvector/ {print $1; exit}')}"
[ -n "$PG_CONTAINER" ] || { echo "FALLO no encuentro un contenedor de Postgres (definir PG_CONTAINER)"; exit 2; }

fallos=0
ok()   { echo "OK    $1"; }
fail() { echo "FALLO $1"; fallos=$((fallos + 1)); }
uid()  { uuidgen | tr 'A-Z' 'a-z'; }
kafka() { docker exec -i "$KAFKA_CONTAINER" /opt/kafka/bin/"$@"; }
psql_llm() { docker exec -i "$PG_CONTAINER" psql -U llm -d llm -tAc "$1"; }

# Ids nuevos por corrida
ATTEMPT=$(uid); COHORT=$(uid); LEARNER=$(uid)
EV_OK=$(uid); EV_OTRO=$(uid); EV_A=$(uid); EV_B=$(uid); EV_C=$(uid); EV_NUEVO=$(uid)

# publicar <eventId> <eventType> <payload>
# El envelope se arma por concatenacion: el bash 3.2 de macOS rompe las comillas anidadas dentro de $(...).
publicar() {
  local msg='{"eventId":"'$1'","eventType":"'$2'","timestamp":"2026-09-20T15:00:00Z","producer":"practice-service","payload":'$3'}'
  echo "$msg" | kafka kafka-console-producer.sh --bootstrap-server $BS --topic practice-events >/dev/null 2>&1
}

# leer <topic>  (todo el topic, con headers y key)
leer() {
  kafka kafka-console-consumer.sh --bootstrap-server $BS --topic "$1" --from-beginning --timeout-ms 6000 \
    --property print.key=true --property print.headers=true 2>/dev/null
}

# scores de ESTE intento en evaluation-events
scores() { leer evaluation-events | grep 'SCORE_CALCULATED' | grep '"attemptId":"'$ATTEMPT'"'; }

# esperar_scores <cantidad>: reintenta hasta 8 veces (~1 min) y deja el resultado en $encontrados / $n
esperar_scores() {
  local i
  for i in 1 2 3 4 5 6 7 8; do
    encontrados=$(scores); n=$(echo "$encontrados" | grep -c 'SCORE_CALCULATED')
    [ "$n" -ge "$1" ] && return 0
  done
  return 1
}

TRANSCRIPT='[{"role":"student","content":"No entiendo por que mi recursion no corta"},{"role":"tutor","content":"Que pasa con n en cada llamada?"}]'
P_OK='{"attemptId":"'$ATTEMPT'","courseCohortId":"'$COHORT'","learnerId":"'$LEARNER'","transcript":'$TRANSCRIPT'}'
P_SIN_ATTEMPT='{"courseCohortId":"'$COHORT'","learnerId":"'$LEARNER'","transcript":[]}'
P_NO_UUID='{"attemptId":"abc","courseCohortId":"'$COHORT'","learnerId":"'$LEARNER'","transcript":[]}'
P_NO_ARRAY='{"attemptId":"'$ATTEMPT'","courseCohortId":"'$COHORT'","learnerId":"'$LEARNER'","transcript":"hola"}'

echo "Evaluador con el bot, broker en el contenedor $KAFKA_CONTAINER"
echo "intento $ATTEMPT / cohorte $COHORT"
echo

echo "-- 1. ATTEMPT_CLOSED valido"
publicar "$EV_OK" ATTEMPT_CLOSED "$P_OK"
if esperar_scores 1; then
  ok "1  llego un SCORE_CALCULATED"
  linea=$(echo "$encontrados" | head -1)
  [[ "$linea" == *$'\t'"$COHORT"$'\t'* ]] && ok "1  Message Key = courseCohortId" || fail "1  la key no es la cohorte: ${linea:0:200}"
  for h in eventId: eventType:SCORE_CALCULATED; do
    [[ "$linea" == *"$h"* ]] && ok "1  header $h" || fail "1  falta el header $h"
  done
  [[ "$linea" == *'"provider":"fake"'* && "$linea" == *'"model":"fake-evaluator-v1"'* ]] && ok "1  evaluator = fake / fake-evaluator-v1" || fail "1  evaluator inesperado"
  for d in autonomy clarity progression compliance efficiency; do
    [[ "$linea" == *'"'$d'":'* ]] || fail "1  falta la dimension $d"
  done
else
  fail "1  no llego ningun SCORE_CALCULATED en ~1 min"
fi

echo "-- 2. casos que NO deben producir score"
publicar "$EV_OK" ATTEMPT_CLOSED "$P_OK"           # mismo eventId repetido
publicar "$EV_OTRO" SOMETHING_ELSE "$P_OK"         # otro eventType
publicar "$EV_A" ATTEMPT_CLOSED "$P_SIN_ATTEMPT"   # sin attemptId
publicar "$EV_B" ATTEMPT_CLOSED "$P_NO_UUID"       # attemptId que no es UUID
publicar "$EV_C" ATTEMPT_CLOSED "$P_NO_ARRAY"      # transcript que no es array
sleep 15
n=$(scores | grep -c 'SCORE_CALCULATED')
[ "$n" -eq 1 ] && ok "2  sigue habiendo un solo score (eventId repetido, otro eventType e invalidos no lo duplican)" || fail "2  hay $n scores y se esperaba 1"

dlt=$(psql_llm "select raw_value || ' | ' || reason from llm.event_dead_letter where raw_value like '%$COHORT%'")
for par in "$EV_A:sin attemptId" "$EV_B:attemptId no UUID" "$EV_C:transcript no es array"; do
  id=${par%%:*}; nombre=${par#*:}
  echo "$dlt" | grep -q "$id" && ok "2  a event_dead_letter: $nombre" || fail "2  no llego a event_dead_letter: $nombre"
done
echo "$dlt" | grep -q "$EV_OTRO" && fail "2  el otro eventType fue a dead-letter (deberia ignorarse)" || ok "2  el otro eventType se ignora (no va a dead-letter)"
echo "$dlt" | grep -q "$EV_OK" && fail "2  el evento valido repetido fue a dead-letter" || ok "2  el eventId repetido no va a dead-letter"

echo "-- 3. reevaluar el mismo intento con un eventId nuevo"
publicar "$EV_NUEVO" ATTEMPT_CLOSED "$P_OK"
esperar_scores 2 && ok "3  llego un segundo SCORE_CALCULATED" || fail "3  no llego el segundo score"

echo
if [ "$fallos" -eq 0 ]; then echo "Evaluador con el bot: todos los casos OK"; else echo "Evaluador con el bot: $fallos caso(s) fallaron"; exit 1; fi
