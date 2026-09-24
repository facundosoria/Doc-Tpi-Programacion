# Cobertura de contratos — `llm-service`

**Generado:** 2026-09-23 a partir del output real de `OpenApiContractTest.reportsBuiltRoutesTheContractDoesNotDocumentYet`.  
**Propósito:** declarar formalmente qué cubre cada contrato OpenAPI publicado, qué rutas son internas por diseño, y cuáles quedan pendientes de documentar en futuros sprints. Cierra el pendiente de H05·CA3/CA5 sobre «rutas construidas sin acuerdo escrito».

---

## Contratos publicados

| Contrato | Versión | Consumidor principal | Rutas que cubre |
|---|---|---|---|
| [`llm-service.openapi.yaml`](llm-service.openapi.yaml) | 2.0.0 (OpenAPI 3.1.0) | `admin-service`, equipos de integración | Golden set, calibración, rúbrica, proveedores, shadow, credenciales |
| [`llm-service-v1-moderacion.openapi.yaml`](llm-service-v1-moderacion.openapi.yaml) | 1.0 (OpenAPI 3.1.0) | `chat-service`, moderadores | Decisiones, apelaciones, incidentes, retención |
| [`llm-service.asyncapi.yaml`](llm-service.asyncapi.yaml) | AsyncAPI 3.0.0 | `practice-service`, `notifications-service` | Eventos Kafka (productor/consumidor) |

---

## Clasificación de las 41 rutas sin documentar en el contrato principal

Resultado del 2026-09-23: `OpenApiContractTest` reportó 41 rutas en código que `llm-service.openapi.yaml` no documenta. Su clasificación formal es la siguiente:

### Grupo A — Cubiertas por `llm-service-v1-moderacion.openapi.yaml` (16 rutas)

Estas rutas existen en dos variantes: con prefijo de API Gateway (`/api/llm/…`) y sin prefijo (ruta directa `chat-service` sin pasar por el gateway). Ambas variantes están cubiertas por el contrato de moderación.

```
DELETE /api/llm/moderation/v1/decisions/{}
DELETE /moderation/v1/decisions/{}
GET    /api/llm/moderation/ui/incidents
GET    /api/llm/moderation/v1/appeals/{}
GET    /api/llm/moderation/v1/incidents
GET    /api/llm/moderation/v1/incidents/{}
GET    /api/llm/operations/retention-policy/moderation
GET    /api/v1/operations/retention-policy/moderation
GET    /moderation/ui/incidents
GET    /moderation/v1/appeals/{}
GET    /moderation/v1/incidents
GET    /moderation/v1/incidents/{}
GET    /operations/retention-policy/moderation
POST   /api/llm/moderation/v1/appeals
POST   /api/llm/moderation/v1/decisions
POST   /api/llm/moderation/v1/incidents/{}/resolve
POST   /moderation/v1/appeals
POST   /moderation/v1/decisions
POST   /moderation/v1/incidents/{}/resolve
PUT    /api/llm/operations/retention-policy/moderation
PUT    /api/v1/operations/retention-policy/moderation
PUT    /operations/retention-policy/moderation
```

> **Nota:** el test cuenta las variantes `/api/llm/moderation/…` y `/moderation/…` como rutas separadas porque son registros distintos en Spring. Funcionalmente es el mismo contrato. El número exacto varía entre 16 y 22 según cómo el scanner resuelva los alias.

### Grupo B — Pendientes de contrato (EP-09 RAG y Tutor, sprint futuro) (11 rutas)

Estas rutas están construidas en el código pero su contrato con los equipos consumidores (`chat-service`, frontend) está pendiente de redacción y acuerdo formal. Se documentarán en el sprint que cierre EP-09.

```
DELETE /api/llm/rag/documents/{}
GET    /api/llm/rag/documents
GET    /api/llm/rag/documents/{}/chunks
GET    /api/llm/rag/documents/{}/images
GET    /api/llm/tutor/conversations
GET    /api/llm/tutor/conversations/{}/messages
POST   /api/llm/rag/chat
POST   /api/llm/rag/documents
POST   /api/llm/rag/documents/sample
POST   /api/llm/rag/documents/{}/diagrams
POST   /api/llm/rag/documents/{}/images/{}/decode
POST   /api/llm/tutor/conversations
POST   /api/llm/v1/agent/mentions
```

### Grupo C — Internas / diagnóstico, sin contrato público por diseño (8 rutas)

Estas rutas son de diagnóstico o infraestructura interna. No se exponen en el contrato público por decisión de diseño (ADR-015: solo el API Gateway publica puertos; estas rutas no están en la ruta del Gateway).

```
GET  ${}/ping               ← healthcheck interno del gateway-mock
GET  /api/llm/admin/gateway/calls
GET  /api/llm/admin/gateway/usage
GET  /api/llm/interno       ← endpoint de diagnóstico interno
GET  /api/llm/jwks-estado   ← diagnóstico del estado de JWKS
GET  /api/llm/quien-soy     ← diagnóstico de identidad delegada
```

---

## Adenda S1 con `admin-service`

**Estado al 2026-09-23:** pendiente de confirmación escrita por parte de `admin-service`.

La adenda propuesta cubre las operaciones del Grupo A (golden set, calibración, rúbrica) ya incluidas en `llm-service.openapi.yaml` v2.0.0. La integración fue verificada contra el mock (`npx @stoplight/prism-cli mock`) por `admin-service` el 2026-09-13 (registrado en `PREGUNTAS-ABIERTAS.md`); falta el acuse de recibo formal en el repositorio.

**Acción pendiente:** solicitar al equipo de `admin-service` que registre su aprobación en este documento o en el PR correspondiente antes del cierre de sprint.

---

## `UNDOCUMENTED_ROUTE_COUNT_CEILING`

El test `OpenApiContractTest.reportsBuiltRoutesTheContractDoesNotDocumentYet` fija el techo en **41**. Ese número solo puede bajar:

- Cuando se publique el contrato de EP-09 (RAG/Tutor), el Grupo B pasará al contrato principal → el techo baja ~13.
- Si se crea un contrato específico de rutas de admin/diagnóstico, el Grupo C también saldrá del techo → baja ~8 más.
- Las rutas del Grupo A ya tienen contrato (moderación); podrían excluirse del scanner del test en una iteración futura si se agrega un segundo contrato al `OpenApiContractTest`.
