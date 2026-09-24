# Tema 12 — Backoffice / ADMIN — contratos

> Este documento es el contrato completo y vigente con Tema 12. Reglas generales (canal
> sync/async, errores, autenticación): [00 — Mapa de integración](../00-mapa-de-integracion.md).
> Todas las rutas pasan por `/api/llm`, requieren JWT M2M con scope administrativo, `traceparent`
> y `X-Request-Id`; toda escritura agrega `Idempotency-Key` cuando el OpenAPI la declara.

## Qué nos llama (Admin operando la API de Tema 07)

### Rúbricas y Golden Sets

| Grupo | Endpoints |
|---|---|
| Plantillas | `GET/POST /api/llm/admin/rubric-templates`; `GET/PATCH .../{rubricVersionId}`; `POST .../publish`; `POST .../next-version`. |
| Rúbricas de curso | `GET/POST/PATCH /api/llm/courses/{courseId}/rubrics`; `POST .../{rubricVersionId}/publish`; `POST .../next-version`. |
| Golden Sets | `GET/POST /api/llm/courses/{courseId}/golden-sets`; `POST .../copy-from-base/{baseVersionId}`; `GET/POST/PATCH/DELETE .../cases`; `POST .../publish`; `POST .../next-version`. |
| Importaciones | `POST .../golden-set-imports`; `POST .../{batchId}/validate`; `GET .../{batchId}/rows/{rowNumber}`; `POST .../{batchId}/commit`. |
| Evidencia | `GET .../eligible-interactions`; `POST .../{interactionId}/anonymize-preview`; `POST .../synthetic-golden-set-cases`. |

### Crear y activar una calibración

| Operación | Request mínimo | Response |
|---|---|---|
| `POST /api/llm/courses/{courseId}/calibrations` | `rubricVersionId`, `goldenSetVersionId`. | `202 CalibrationRun`. |
| `GET /api/llm/courses/{courseId}/calibrations/{runId}` | Path. | `CalibrationRun` con estado y métricas. |
| `GET /api/llm/courses/{courseId}/calibrations/stability-groups` | Path. | Grupos de estabilidad. |
| `POST /api/llm/courses/{courseId}/calibrations/{runId}/activate-preview` | Path, `Idempotency-Key`. | `200 ActivationPreview` o `409`. |
| `POST /api/llm/courses/{courseId}/calibrations/{runId}/activate` | Path, `Idempotency-Key`. | `200 ActiveCalibration` o `409`. |
| `GET /api/llm/courses/{courseId}/active-calibration` | Path. | `ActiveCalibration` o `404`. |
| `GET /api/llm/courses/{courseId}/challenge-calibration-assignments` | Path. | `AssignmentPage`. |
| `GET /api/llm/courses/{courseId}/pending-evaluations` | Path. | `PendingEvaluationPage`. |

Una corrida `PASSED` no pasa a `ACTIVE` automáticamente: la activación es una operación separada y
explícita, mediada por `activate-preview` → `activate`. Un preview vencido, obsoleto o
incompatible devuelve `409`.

### Proveedores y modelos

| Grupo | Endpoints |
|---|---|
| Adapters | `GET/POST /api/llm/admin/model-adapters`. |
| Credenciales | `POST /api/llm/admin/provider-credentials/{credentialId}/test-model` y `/deployments`. |
| Calibración institucional | `GET/POST /api/llm/admin/institutional-calibration/profile`; `GET/POST /api/llm/admin/institutional-calibration/runs`. |
| Evaluadores | `GET /api/llm/admin/evaluator-models`; `GET .../active`; `GET .../calibration-target`; `GET .../events`. |
| Candidato | `POST .../{deploymentId}/chat`; `POST .../chat/stream`; `POST .../activate`; `POST .../select-for-calibration`; `GET .../usage`; `GET/DELETE .../{deploymentId}`. |
| Asignación | `GET/PUT /api/llm/model-assignments/{function}`. |

Tema 07 devuelve secretos como `writeOnly`/enmascarados y nunca publica credenciales, prompts
administrativos completos o secretos en eventos.

### Skills y calibración jerárquica (superficie propuesta, no ejecutable todavía)

Las rutas de Skills, borradores de calibración jerárquica y verificación/reanudación
administrativa están **propuestas**, no en el OpenAPI vigente. El detalle completo está en
[`../02-contratos-skills-y-calibracion.md`](../02-contratos-skills-y-calibracion.md) — no se
duplica acá para no desincronizarlo.

## Qué nos da (eventos que Tema 07 consume desde Admin)

Canal `llm-config-events` (`x-topic-status: pending-assignment`), envelope de 5 campos —
`eventId`, `eventType`, `timestamp`, `producer`, `payload` — por
[`KAFKA_EVENT_STANDARD.md`](../KAFKA_EVENT_STANDARD.md).

| `eventType` | Efecto en Tema 07 |
|---|---|
| `MODEL_CHANGED` | Dispara recalibración automática (RF-IA-32). |

⚠️ **El schema ejecutable (`ModelChangedEvent`) hoy es `Envelope` + `eventType` únicamente — sin
`payload` definido.** Si el cambio de modelo dispara recalibración a nivel plataforma o por curso
es una decisión pendiente de Admin, no una suposición que ya esté en el schema.

## Qué le damos (eventos que Tema 07 publica hacia Admin)

Canales `calibration-events` y `moderation-events` (`x-topic-status: pending-assignment`), mismo
envelope. Message key de `calibration-events`: `rubricVersionId`.

| `eventType` | Canal | Efecto en Admin |
|---|---|---|
| `CALIBRATION_APPROVED` | `calibration-events` | Proyectar una corrida aprobada. |
| `CALIBRATION_OUT_OF_TOLERANCE` | `calibration-events` | Proyectar que no habilita la cohorte. |
| `JAILBREAK_INCIDENT_DETECTED` | `moderation-events` | Abrir incidente administrativo (consumidores: Tema 12 + equipo de seguridad). |

⚠️ `CalibrationResultEvent` y `JailbreakIncidentEvent` hoy son `Envelope` + `eventType`
únicamente — ninguno de los dos tiene `payload` definido en `llm-service.asyncapi.yaml` todavía.
Cuando se defina, ese payload no debe contener soluciones esperadas, transcripciones, Markdown ni
secretos.

Sin estos endpoints/eventos, Tema 12 no tiene forma de demostrar una calibración.

## Qué pasa si esto falla

Técnica común en
[transversales del README de equipos](README.md#agenda-de-la-sesión-de-integración) y en
[06 — Operación e ingeniería](../../06-operacion-calidad-y-pruebas/01-operacion-e-ingenieria.md).
Una corrida de calibración llama al proveedor una vez por caso del Golden Set: Circuit Breaker por
proveedor, backoff con tope, y el job queda `fallido` si se agota el margen — nunca "degradación
funcional": una calibración con modelo local o degradado no sirve para aprobar RF-IA-36.

Si Tema 12 dispara `POST /calibrations` y el job termina `fallido`, hoy no hay un evento
específico de "calibración no se pudo correr" — solo `CALIBRATION_OUT_OF_TOLERANCE` (que es un
resultado, no un error de infraestructura). Confirmar con Tema 12 si hace falta distinguir "no
corrió" de "corrió y no pasó PAR-14".

## Acordado

- Tema 12 es dueño de la pantalla de configuración del proveedor LLM.
- 🟡 Propuesto — Tema 12 es dueño de la pantalla de límites de uso de IA por alumno (cantidad de
  usos y tokens por día). Ver
  [`pendientes.md`](../../07-planificacion-y-trabajo-equipo/11-equipos/tema-12-backoffice-admin/pendientes.md)
  por la tensión con la privacidad del panel agregado de costos.

## Pendientes de contrato con Tema 12

| Tema | Estado |
|---|---|
| Payload de `MODEL_CHANGED` (alcance plataforma vs. curso) | 🔴 sin acordar |
| Payload de `CALIBRATION_APPROVED` / `CALIBRATION_OUT_OF_TOLERANCE` | 🔴 sin acordar |
| Evento distinto para "corrida no pudo ejecutarse" vs. "corrida no pasó PAR-14" | 🟡 a confirmar |
| Endpoints de Skills y calibración jerárquica (ver `02-contratos-skills-y-calibracion.md`) | 🟡 propuestos, no en OpenAPI |
| _(nuevos temas)_ | agregar acá |
