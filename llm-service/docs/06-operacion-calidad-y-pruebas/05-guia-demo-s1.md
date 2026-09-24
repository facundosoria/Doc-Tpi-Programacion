# 05 — Guía de Demostración Reproducible — Sprint 1 (LLM-EP01-H06 / ex-H09)

> **Historia de Usuario / Tarea Técnica:** [`historias/ep-01/h06.md`](../07-planificacion-y-trabajo-equipo/09-epicas-historias-tareas-sprints/historias/ep-01/h06.md)  
> **Traza de Criterios de Aceptación:** CA3 (persistencia tras reinicio), CA4 (guía paso a paso ejecutable), CA6 (bloqueo ante pérdida de datos).  
> **Audiencia:** Equipo de desarrollo, Product Owner, Comité evaluador de Sprint Review.  
> **Objetivo:** Demostrar con evidencia reproducible —y no solo con un relato— el recorrido canónico de S1: **acceso autorizado → alta de rúbrica y golden set → carga de casos de referencia → reinicio de contenedores → consulta y verificación de persistencia**.  
> **Verificada el 2026-09-21** ejecutando cada comando de esta guía contra el stack real (macOS, Docker 28.5.1).

---

## 1. Prerrequisitos

Todos los comandos se corren desde la raíz de `llm-service/`. Una sola vez por máquina:

```bash
cp .env.example .env                 # y completar LLM_CREDENTIALS_MASTER_KEY en .env
openssl rand -base64 32              # genera la clave AES-256 que pide LLM_CREDENTIALS_MASTER_KEY
docker network create tpi-platform   # red externa de la plataforma; no hace falta si ya existe
```

Identificadores fijos de la demostración (los únicos que el `courses-mock` autoriza):

- **`courseId`:** `22222222-2222-2222-2222-222222222222`
- **Docente (`X-Delegated-User`):** `11111111-1111-1111-1111-111111111111`
- **Plantilla institucional de rúbrica:** `10000000-0000-0000-0000-000000000002` (sembrada por Flyway)

## 2. Cómo se entra al servicio (identidad)

Desde la integración `main` → `dev`, el servicio valida la pertenencia del docente al curso contra
courses-service. En la demo eso lo simulan dos contenedores del overlay `compose.workbench.yaml`:

- **`gateway-mock`** (Nginx, `localhost:8080`): hace de API Gateway. **Inyecta** `X-Principal-Type`,
  `X-Service-Id: admin-service`, los scopes y `X-Delegated-User` del docente, y descarta cualquier
  identidad que mande el cliente. Por eso **los `curl` de esta guía no llevan headers de identidad**.
- **`courses-mock`** (MockServer): responde por courses-service. Autoriza el curso `2222…` y rechaza otros
  (por ejemplo `4444…` → `403`).

Sin `courses-mock` crear un golden set devuelve `503 "Courses no está disponible"`. El frontend Angular
del workbench **no** se levanta para esta demo.

## 3. Recorrido paso a paso

```bash
C=22222222-2222-2222-2222-222222222222
B=http://localhost:8080/api/llm/courses/$C
dc() { docker compose -f compose.yaml -f compose.workbench.yaml -f compose.debug.yaml "$@"; }   # sirve en bash y zsh
```

### Paso 1 — Arranque y salud

```bash
dc up -d --build --wait llm-service gateway-mock courses-mock
curl -s http://localhost:8087/actuator/health
```

Esperado: `{"status":"UP","groups":["liveness","readiness"]}`. (El actuator está en el puerto de
management `8087`, publicado por `compose.debug.yaml`; el `gateway-mock` no lo expone.)

### Paso 2 — Alta y publicación de la rúbrica del curso

La rúbrica del curso nace de la plantilla institucional (cinco dimensiones, pesos 30/25/20/15/10):

```bash
curl -s -X POST $B/rubrics -H "Content-Type: application/json" -H "Idempotency-Key: $(uuidgen)" \
  -d '{"templateVersionId":"10000000-0000-0000-0000-000000000002","name":"Rúbrica Algoritmos S1"}'
```

Esperado: `201` con `{"id":"<RUBRIC_ID>", … "state":"DRAFT", …}`. Publicarla:

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST $B/rubrics/<RUBRIC_ID>/publish \
  -H "Idempotency-Key: $(uuidgen)" -H "Content-Type: application/json"
```

Esperado: `204`.

### Paso 3 — Alta del golden set

```bash
curl -s -X POST $B/golden-sets -H "Content-Type: application/json" -d '{"name":"Banco de Casos Oficial S1"}'
```

Esperado: `201` con `{"id":"<VERSION_ID>","familyId":"…","version":1,"state":"DRAFT"}`.

### Paso 4 — Carga del caso de referencia

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X POST $B/golden-sets/<VERSION_ID>/cases \
  -H "Content-Type: application/json" -d '{
    "transcript": [{"role": "STUDENT", "content": "¿Cuál es el peor caso del algoritmo Quicksort y por qué?"}],
    "challengeContext": {"statement": "Explicar particionamiento y complejidad cuadrática",
                         "expectedKeyPoints": ["pivote desbalanceado", "O(n^2)"]},
    "author": "Prof. S1 Titular",
    "referenceScores": {"AUTONOMY": 80, "CLARITY": 85, "PROGRESSION": 90, "COMPLIANCE": 88, "EFFICIENCY": 90},
    "scoreJustifications": {"AUTONOMY": "El estudiante identifica de manera independiente el caso del pivote"}
  }'
```

Esperado: `201`.

### Paso 5 — Consulta previa al reinicio

```bash
curl -s $B/golden-sets
```

Esperado: `{"items":[{"id":"<VERSION_ID>","name":"Banco de Casos Oficial S1","state":"DRAFT","cases":[…]}]}`.

### Paso 6 — Reinicio del entorno

```bash
dc restart
until curl -s http://localhost:8087/actuator/health | grep -q '"status":"UP"'; do sleep 1; done
```

### Paso 7 — Consulta posterior (verificación de persistencia, CA3)

```bash
curl -s $B/golden-sets
```

Evidencia: el golden set sigue con el mismo `id` y `name`; los datos viven en el volumen
`llm-postgres-data`.

### Paso 8 — Verificación automatizada

Todo el recorrido anterior, más la persistencia del registro de deduplicación de eventos Kafka
(`kafka_consumed_events`, H07·CA5), en un solo comando; levanta y destruye su propio stack:

- Linux / macOS / Git Bash: `bash scripts/test-compose-restart.sh`
- Windows PowerShell: `powershell -File scripts/test-compose-restart.ps1`

Sale con código `!= 0` si el dato no persiste (CA6). Para apagar la demo manual:

```bash
dc down --volumes --remove-orphans
```

## 4. Pruebas negativas

1. **Sin identidad, directo al servicio** (saltándose el Gateway; puerto `8086` publicado solo por el overlay de debug):
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8086/api/llm/courses/$C/golden-sets
   ```
   Esperado: `401`.
2. **Curso que el docente no integra** (el `courses-mock` no lo autoriza):
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/llm/courses/44444444-4444-4444-4444-444444444444/golden-sets
   ```
   Esperado: `403`.
3. **Golden set inexistente** (id bien formado que no existe):
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" $B/golden-sets/00000000-0000-0000-0000-000000000000
   ```
   Esperado: `404` (Problem Details).

## 5. Matriz de trazabilidad de criterios de aceptación

| Criterio de Aceptación | Cómo se valida | Estado (2026-09-21) |
|---|---|---|
| **CA1:** suite completa en verde | `mvn -o verify -Dintegration=true`: 688 unitarios (incluye 3 de esquema) + 99 de integración, 0 fallas | 🟡 en verde local; sin CI |
| **CA2:** cobertura ≥ 90 % (Doc 24) | `jacoco:check` del `pom.xml` de `app`: 92,9 % instrucciones, 93,7 % líneas; `domain` 99,1 % | ✅ |
| **CA3:** prueba automática de reinicio | `scripts/test-compose-restart.sh` / `.ps1` | ✅ ejecutado |
| **CA4:** guía paso a paso | Este documento, verificado comando por comando | ✅ |
| **CA5 (neg.):** cobertura baja → se rechaza el cambio | `jacoco:check` rompe `mvn verify` (ocurrió el 2026-09-21: `domain` 89,6 %) | 🟡 sin CI que lo imponga en cada PR |
| **CA6 (neg.):** pérdida de datos al reiniciar → falla | El script sale con código 1 si falta el dato | ✅ |
