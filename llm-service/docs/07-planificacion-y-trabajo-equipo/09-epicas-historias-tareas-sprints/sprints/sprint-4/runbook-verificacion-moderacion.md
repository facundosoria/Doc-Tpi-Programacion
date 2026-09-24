# Runbook de Verificación — Moderación de Chat (LLM-S11-H01)

> **Historia:** [LLM-S11-H01](../../historias/ep-08/h01.md) · **Épica:** EP-08 · Moderación integrada
> **Servicios involucrados:** `chat-service` (consumidor) ↔ `llm-service` (proveedor)
> **Endpoint:** `POST /moderation/v1/decisions` (también disponible en `/api/llm/moderation/v1/decisions`)

Este documento provee la guía de verificación interactiva y los comandos `curl` para que el equipo de `chat-service` pueda probar y validar la integración del canal de moderación síncrona.

---

## 1. Requisitos Previos

- `llm-service` levantado en el puerto local (por defecto `8086` o `8080` según perfil) o accesible a través del API Gateway (`http://localhost:8080`).
- Identidad de servicio con scope `moderation:decide`. `llm-service` **no valida JWT**: confía en los headers que inyecta el API Gateway (DEC-08). Contra el servicio directo (overlay `compose.debug.yaml`, puerto 8086) se envían esos headers a mano; un Bearer sin firma da `401`.

Cabeceras de servicio para las pruebas (reemplazan al JWT de las versiones anteriores de este runbook):
```bash
SVC=(-H 'X-Principal-Type: service' -H 'X-Service-Id: chat-service' -H 'X-Service-Scopes: moderation:decide')
```

> **Ojo v1.1.0:** el body de `/decisions` lleva ahora `"sender_id"` (obligatorio). En los casos de abajo, sustituí `-H "Authorization: Bearer ${MOCK_JWT}"` por `"${SVC[@]}"` y agregá `"sender_id": "alumno-1"`. Contrato: `docs/contracts/llm-service-v1-moderacion.openapi.yaml`.

(Método viejo, ya no funciona salvo `trust-unsigned-bearer=true`:)
```bash
# Header: {"alg":"none","typ":"JWT"}
HEADER="eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0"
# Payload: {"sub":"chat-service","scope":"moderation:decide"}
PAYLOAD="eyJzdWIiOiJjaGF0LXNlcnZpY2UiLCJzY29wZSI6Im1vZGVyYXRpb246ZGVjaWRlIn0"
MOCK_JWT="${HEADER}.${PAYLOAD}.mock-signature"
```

---

## 2. Comandos de Verificación (cURL)

### Caso 1: Decisión Exitosa (ALLOW determinista) — 200 OK
```bash
curl -X POST "http://localhost:8086/moderation/v1/decisions" \
  -H "Authorization: Bearer ${MOCK_JWT}" \
  -H "Content-Type: application/json" \
  -d '{
    "message_id": "msg-demo-001",
    "course_id": "curso-tpi-2026",
    "sender_id": "alumno-1",
    "sender_role": "student",
    "text": "Hola profesor, ¿cuándo es la entrega del sprint 4?",
    "context_flags": {
      "thread_id": "thread-10",
      "is_reply": false
    }
  }'
```
**Respuesta esperada (200 OK):**
```json
{
  "message_id": "msg-demo-001",
  "decision": "ALLOW",
  "reason_code": "CLEAN",
  "classifier_used": "deterministic",
  "latency_ms": 12,
  "incident_id": null
}
```
*Nota: El texto del mensaje NO queda persistido en la tabla de auditoría (minimización de datos).*

---

### Caso 2: Idempotencia (Mismo `message_id`) — 200 OK
Reenviar exactamente la misma petición anterior con `message_id: "msg-demo-001"`:
```bash
curl -X POST "http://localhost:8086/moderation/v1/decisions" \
  -H "Authorization: Bearer ${MOCK_JWT}" \
  -H "Content-Type: application/json" \
  -d '{
    "message_id": "msg-demo-001",
    "course_id": "curso-tpi-2026",
    "sender_id": "alumno-1",
    "sender_role": "student",
    "text": "Hola profesor, ¿cuándo es la entrega del sprint 4?"
  }'
```
**Respuesta esperada (200 OK):**
Devuelve exactamente la misma respuesta original sin ejecutar una nueva evaluación ni duplicar registros en la base de datos de auditoría.

---

### Caso 3: Rechazo por Falta de Token o Scope — 401 Unauthorized
Petición sin cabecera de autorización:
```bash
curl -i -X POST "http://localhost:8086/moderation/v1/decisions" \
  -H "Content-Type: application/json" \
  -d '{
    "message_id": "msg-demo-002",
    "course_id": "curso-tpi-2026",
    "sender_id": "alumno-1",
    "sender_role": "student",
    "text": "Mensaje no autorizado"
  }'
```
**Respuesta esperada (401 Unauthorized):**
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Missing scope: moderation:decide"
}
```

---

### Caso 4: Rechazo por Payload Inválido — 400 Bad Request
Petición con campo obligatorio `text` vacío:
```bash
curl -i -X POST "http://localhost:8086/moderation/v1/decisions" \
  -H "Authorization: Bearer ${MOCK_JWT}" \
  -H "Content-Type: application/json" \
  -d '{
    "message_id": "msg-demo-003",
    "course_id": "curso-tpi-2026",
    "sender_id": "alumno-1",
    "sender_role": "student",
    "text": ""
  }'
```
**Respuesta esperada (400 Bad Request):**
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Los datos enviados no son válidos o contienen campos obligatorios ausentes."
}
```

---

## 3. Script Automatizado de Verificación (`verificar-moderacion.sh`)

```bash
#!/usr/bin/env bash
set -e

BASE_URL="${1:-http://localhost:8086}"
HEADER="eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0"
PAYLOAD="eyJzdWIiOiJjaGF0LXNlcnZpY2UiLCJzY29wZSI6Im1vZGVyYXRpb246ZGVjaWRlIn0"
JWT="${HEADER}.${PAYLOAD}.sig"

echo "=== 1. Probando 200 OK ALLOW ==="
curl -s -f -X POST "${BASE_URL}/moderation/v1/decisions" \
  -H "Authorization: Bearer ${JWT}" \
  -H "Content-Type: application/json" \
  -d '{"message_id":"test-sh-1","course_id":"c1","sender_id":"alumno-1","sender_role":"student","text":"Hola"}' | grep '"decision":"ALLOW"'
echo " [OK]"

echo "=== 2. Probando Idempotencia ==="
curl -s -f -X POST "${BASE_URL}/moderation/v1/decisions" \
  -H "Authorization: Bearer ${JWT}" \
  -H "Content-Type: application/json" \
  -d '{"message_id":"test-sh-1","course_id":"c1","sender_id":"alumno-1","sender_role":"student","text":"Hola"}' | grep '"decision":"ALLOW"'
echo " [OK]"

echo "=== 3. Probando 401 sin token ==="
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/moderation/v1/decisions" \
  -H "Content-Type: application/json" \
  -d '{"message_id":"test-sh-2","course_id":"c1","sender_id":"alumno-1","sender_role":"student","text":"Hola"}')
[ "$STATUS" -eq 401 ]
echo " [OK: Status 401]"

echo "=== Todas las verificaciones pasaron con éxito ==="
```

---

## 4. Flujo completo verificado en vivo (2026-09-18): bloqueo → apelación → docente

```bash
B=http://localhost:8086/moderation/v1
SVC=(-H 'X-Principal-Type: service' -H 'X-Service-Id: chat-service' -H 'X-Service-Scopes: moderation:decide' -H 'Content-Type: application/json')
ALU=(-H 'X-Principal-Type: user' -H 'X-User-Id: alumno-1' -H 'X-User-Roles: STUDENT' -H 'Content-Type: application/json')
DOC=(-H 'X-Principal-Type: user' -H 'X-User-Id: doc-1' -H 'X-User-Roles: TEACHER' -H 'X-Teacher-Course-Ids: c1' -H 'Content-Type: application/json')

# 1) chat pide decisión -> BLOCK + incident_id (guardalo en ID)
curl -s -X POST $B/decisions "${SVC[@]}" -d '{"message_id":"e2e-1","course_id":"c1","sender_id":"alumno-1","sender_role":"student","text":"mira http://a.com http://b.com http://c.com ya"}'
# 2) el alumno apela  -> 201 PENDING_REVIEW (otro usuario: 403; segunda vez: 409)
curl -s -X POST $B/appeals "${ALU[@]}" -d "{\"incident_id\":\"$ID\",\"appeal_reason\":\"Era un mensaje legitimo con enlaces de la catedra.\"}"
# 3) el docente lista su curso (curso ajeno: 403)
curl -s "$B/incidents?course_id=c1" "${DOC[@]}"
# 4) el docente resuelve (motivo < 20 caracteres: 400)
curl -s -X POST $B/incidents/$ID/resolve "${DOC[@]}" -d '{"resolution":"REVERSED","resolution_reason":"Los enlaces eran material de la catedra, falso positivo."}'
```

Mensajes disparadores (motor determinista, sin OpenAI): spam = ≥ 3 URLs; ofuscación = Base64 de código; ofensivo = insultos del diccionario; modo degradado = texto con palabras como `amenaza`/`dudoso` (dispara el clasificador contextual; sin API key responde `PENDING_REVIEW` con `degradation_reason: CONTEXTUAL_UNAVAILABLE`, nunca `ALLOW`).
