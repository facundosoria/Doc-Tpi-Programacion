# Mock del contrato llm-service — Golden Set y Calibración (v2)

> **Historia:** `LLM-EP01-H05` (ex-H08) — Contrato OpenAPI y mock del golden set publicados (10 h).  
> **Criterio de Aceptación CA2:** El simulador se levanta con un comando documentado y responde según el contrato.  
> **Contrato de referencia:** [`llm-service.openapi.yaml`](llm-service.openapi.yaml).  
> **Propósito:** Permite a `admin-service` y otros consumidores de la plataforma avanzar en su desarrollo e integración desacoplada sin depender del despliegue real del servicio.

---

## 1. Opción Standalone (Prism — Mock HTTP directo del contrato)

Recomendada para pruebas rápidas de contrato, integración en CI de otros equipos o desarrollo sin base de datos ni Docker.

### Prerrequisito
Disponer de Node.js (npx se incluye por defecto) o Docker.

### Comando para levantar el mock (desde la raíz del repo)

```bash
npx --yes @stoplight/prism-cli mock llm-service/docs/contracts/llm-service.openapi.yaml --port 4010
```

El simulador queda disponible en `http://localhost:4010`, **sin** el prefijo `/api/llm`: Prism ignora el
`servers.url` relativo del contrato, así que `GET /api/llm/admin/rubric-templates` del servicio real es
`GET http://localhost:4010/admin/rubric-templates` en el mock. Verificado el 2026-09-21: devuelve `200` con
cuerpo conforme al esquema y `401` sin `Authorization` (el contrato exige `serviceJwt`).

### Alternativa con Docker (sin requerir Node local)

```bash
docker run --rm -p 4010:4010 -v "${PWD}/llm-service/docs/contracts:/tmp/contracts" stoplight/prism:4 mock /tmp/contracts/llm-service.openapi.yaml --host 0.0.0.0
```

---

## 2. Opción Simulador de Desarrollo Local (Perfil Workbench)

El repositorio incluye un perfil de desarrollo simulado para pruebas locales del backend con datos controlados en memoria.

### Comando

```bash
docker compose -f compose.yaml -f compose.workbench.yaml up
```

### Características del modo Workbench
- Activa `SPRING_PROFILES_ACTIVE=workbench`.
- Los cursos del laboratorio los sirve el `courses-mock` (MockServer, `lab/courses/expectations.json`), no el
  backend: `llm-service` los resuelve con `GatewayCoursesMembershipClient` igual que contra courses-service real.
  El catálogo en memoria `WorkbenchDemoCatalog` se borró en la [integración del 2026-09-21](../registro/2026-09-21-integracion-main-a-dev.md).
- Siembra rúbrica y golden set de prueba con `WorkbenchCalibrationSeed` (perfil `workbench`, idempotente).
- Simula la identidad del docente sin requerir el API Gateway real ni un Identity Provider M2M.
- Levanta además `gateway-mock` (Nginx) en `localhost:8080` como único punto HTTP del laboratorio:
  enruta `/api/llm/**` al `llm-service` real y `/api/courses/**` al `courses-mock` (MockServer, que
  reemplaza a `courses-service`). El navegador usa rutas relativas y no construye headers de
  identidad ni tokens M2M: Nginx descarta los headers sensibles que mande el cliente, inyecta la
  identidad delegada de desarrollo, `traceparent` y `X-Request-Id`, y conserva el path completo.
- El mock de integración **no reemplaza al backend**: `llm-service` y PostgreSQL corren de verdad;
  solo se simulan las fronteras que pertenecen a otros equipos.

---

## 3. Ejemplos de Peticiones contra el Mock Standalone (Puerto 4010)

### A. Listar Cursos Autorizados
```bash
curl -s http://localhost:4010/api/llm/courses \
  -H "Authorization: Bearer mock-token"
```

### B. Listar Golden Sets de un Curso
```bash
COURSE_ID="00000000-0000-0000-0000-000000000010"
curl -s http://localhost:4010/api/llm/courses/${COURSE_ID}/golden-sets \
  -H "Authorization: Bearer mock-token"
```

### C. Crear un Borrador de Golden Set
```bash
COURSE_ID="00000000-0000-0000-0000-000000000010"
curl -s -X POST http://localhost:4010/api/llm/courses/${COURSE_ID}/golden-sets \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer mock-token" \
  -H "Idempotency-Key: 11111111-2222-3333-4444-555555555555" \
  -d '{"name": "Golden Set Inicial 2026"}'
```

### D. Publicar Versión de Golden Set
```bash
COURSE_ID="00000000-0000-0000-0000-000000000010"
VERSION_ID="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
curl -s -X POST http://localhost:4010/api/llm/courses/${COURSE_ID}/golden-sets/${VERSION_ID}/publish \
  -H "Authorization: Bearer mock-token" \
  -H "Idempotency-Key: 22222222-3333-4444-5555-666666666666"
```

### E. Crear Siguiente Versión
```bash
COURSE_ID="00000000-0000-0000-0000-000000000010"
VERSION_ID="aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
curl -s -X POST http://localhost:4010/api/llm/courses/${COURSE_ID}/golden-sets/${VERSION_ID}/next-version \
  -H "Authorization: Bearer mock-token" \
  -H "Idempotency-Key: 33333333-4444-5555-6666-777777777777"
```

---

## 4. Alcance y Límites del Mock

| Aspecto | Comportamiento en el Mock | Comportamiento en Producción |
|---|---|---|
| **Persistencia** | Sin persistencia (stateless: genera respuestas válidas conformes a schema) | PostgreSQL con Flyway |
| **Idempotencia** | No verifica unicidad de `Idempotency-Key` | Valida clave y rechaza duplicados |
| **Autorización** | Acepta cualquier Bearer token sintético | Valida JWT M2M con scopes y tenancy en Gateway |
| **Calibración** | Retorna estado QUEUED/RUNNING de ejemplo | Ejecuta runner asíncrono con métricas PAR-14 |
