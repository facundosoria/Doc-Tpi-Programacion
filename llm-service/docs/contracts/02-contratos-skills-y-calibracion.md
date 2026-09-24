# Contratos de skills y calibración

> **Estado:** referencia de consolidación. Distingue contrato canónico,
> requisito interservicio y propuesta pendiente de aprobación. No reemplaza
> `llm-service.openapi.yaml` ni `llm-service.asyncapi.yaml`.
>
> **Alcance:** los puntos 2 (cuota), 4 (sanitización) y 9 (observabilidad y pruebas) están fuera del
> sprint: este documento no habilita su implementación.
>
> **Relación con la documentación decisión por decisión (2026-09-22):** este documento resume la
> superficie de API propuesta. El detalle íntegro de cada decisión de Producto (D-01 a D-166),
> con su implicancia de diseño completa, vive en:
> [03 — Sincronización de calibración y avisos de operación](03-sincronizacion-calibracion-y-notificaciones.md)
> (D-21 a D-26, D-78 a D-85, D-95 a D-99, D-110 a D-112, D-115 a D-119, D-164, D-165),
> [08 — Límites, seguridad de skills y evaluador](../02-arquitectura-y-plataforma/08-limites-seguridad-de-skills-y-evaluador.md)
> (D-27 a D-38, D-88 a D-91),
> [06 — Evaluación diferida, verificación y degradación](../06-operacion-calidad-y-pruebas/06-evaluacion-diferida-verificacion-y-degradacion.md)
> (D-86 a D-94, D-100 a D-119, D-162, D-163, D-166), y en
> [03 — Golden Set y calibración: 05 a 09](../03-capacidades-de-ia/golden-set-y-calibracion/README.md)
> (skills, subcalibración jerárquica, rúbrica ponderada, máquina de estados y flujos de UI). Ante
> cualquier diferencia de detalle entre este resumen de endpoints y esos documentos, prevalece el
> documento decisión por decisión, no este resumen.

## 1. Convenciones aplicables

### Sincrónico

- Todo endpoint se expone exclusivamente bajo `/api/llm/**` mediante API
  Gateway; no se habilitan puertos directos entre microservicios.
- Las operaciones requieren JWT M2M con `aud=llm-service` y scope mínimo. La
  identidad delegada, curso y tenancy se validan desde los headers/token del
  Gateway; no se confía en valores de identidad enviados en el body.
- `traceparent` y `X-Request-Id` son headers obligatorios. La respuesta replica
  `X-Request-Id`.
- Toda escritura requiere `Idempotency-Key` UUID. La misma clave, operación,
  actor y payload devuelve el resultado original; reutilizarla con otro payload
  produce `409` con código tipado.
- Los errores usan RFC 7807 y `codigo` estable. `409` se usa para estado,
  revisión, baseline o preview obsoleto; `422` para entrada inválida.

### Asincrónico

- Todo topic usa `<evento>.v<major>` y envelope con `eventId`, `version`,
  `occurredAt`, `producer` y `data`.
- `traceparent` y `X-Request-Id` viajan sólo en headers Kafka.
- La entrega es *at-least-once*: todo consumidor deduplica por `eventId`.
- La publicación se escribe en outbox en la misma transacción que el hecho de
  dominio; un relay publica posteriormente. No se publica desde un controller
  o worker sin outbox.

## 2. Contratos sincrónicos

### 2.1 Contrato HTTP canónico existente

El schema ejecutable vigente es [llm-service.openapi.yaml](llm-service.openapi.yaml).
Su superficie actual se conserva hasta que una propuesta se apruebe y se fusione
explícitamente.

| Grupo de recursos | Entrada a LLM | Salida de LLM |
|---|---|---|
| Tutor | `POST /tutor/interactions` desde `practice-service`: contexto del intento y mensaje. | Respuesta pedagógica; nunca corrección académica ni solución esperada. |
| Rúbricas | Plantillas institucionales y rúbricas versionadas por curso. | Borradores/versiones, dimensiones, pesos y estado. |
| Golden Set | Versiones, casos, importaciones, anonimización y casos sintéticos. | Golden Sets, lotes, resultados de validación y propuestas. |
| Calibración actual | Crear/listar/consultar corridas, preview y activación. | Corrida, métricas PAR-14, preview, calibración activa y asignaciones. |
| Evaluaciones pendientes | Consulta por curso. | Página de pendientes en el modelo histórico actual. |
| Modelos y proveedores | Administración de credenciales, deployments y modelo evaluador. | Recursos administrativos y modelos disponibles. |

El OpenAPI actual usa rutas históricas como
`/courses/{courseId}/calibrations/{runId}/activate`. Las rutas de la siguiente
sección son adiciones o reemplazos propuestos; no deben coexistir en producción
sin una decisión explícita de migración y compatibilidad.

### 2.2 Endpoints faltantes de skills — PROPUESTOS

No existe todavía una ruta `/skills` en el OpenAPI canónico. Se propone que el
catálogo sea global dentro de LLM, con autorización por actor y curso, y que las
asociaciones de curso sólo clasifiquen/filtren, sin otorgar permisos.

| Método y ruta propuesta | Entrada mínima | Salida mínima | Nota |
|---|---|---|---|
| `GET /api/llm/skills` | Query opcional: `courseId`, `q`, `tag`, `visibility`, `scope`, `sort`, `page`, `size`. | `SkillPage` con nombre, función, autor, versión visible, estado, visibilidad, etiquetas y cursos asociados autorizados. | No expone skills privadas de terceros. |
| `POST /api/llm/skills` | `multipart/form-data`: archivo Markdown, nombre, descripción funcional, etiquetas, `associatedCourseIds`, visibilidad. | `201 SkillVersion`. | La validación/sanitización previa a disponibilidad depende del punto 4, hoy fuera de alcance. |
| `GET /api/llm/skills/{skillId}` | Path. | Metadatos de familia y versión vigente visible. | `404` si no existe o no está autorizada. |
| `PATCH /api/llm/skills/{skillId}` | `If-Match`; nombre, descripción, etiquetas, visibilidad y cursos asociados. | Metadatos actualizados. | No altera Markdown ni crea versión de contenido. |
| `GET /api/llm/skills/{skillId}/versions` | Paginación. | `SkillVersionPage`. | Incluye hash, estado y procedencia; no contenido si el actor no puede leerlo. |
| `POST /api/llm/skills/{skillId}/versions` | `multipart/form-data`: nuevo Markdown y metadatos de versión. | `201 SkillVersion`. | Crea una versión inmutable; no modifica calibraciones históricas. |
| `GET /api/llm/skills/{skillId}/versions/{skillVersionId}` | Path. | Metadatos y Markdown si la autorización permite verlo. | El Markdown nunca se registra en logs. |
| `POST /api/llm/skills/{skillId}/clones` | `name`, descripción/etiquetas/cursos opcionales. | `201 SkillVersion` privada. | Mantiene `originSkillVersionId`; la copia es independiente. |
| `GET /api/llm/skills/favorites` | Paginación. | Favoritos del actor, fijados a `skillVersionId`. | No actualiza favoritos automáticamente. |
| `PUT /api/llm/skills/favorites/{skillVersionId}` | Sin body. | `204`. | Idempotente; crea favorito del actor. |
| `DELETE /api/llm/skills/favorites/{skillVersionId}` | Sin body. | `204`. | Idempotente; sólo afecta favorito propio. |
| `GET /api/llm/skills/{skillId}/versions/{skillVersionId}/usages` | `courseId` opcional autorizado, paginación. | Usos actuales e históricos por curso/desafío/calibración. | No revela recursos de cursos no autorizados. |

Las acciones de archivar, deshabilitar, marcar revisión y cerrar una revisión
administrativa se reservan para el punto 4. No se implementan ni publican en
esta iteración aunque el plan registre su comportamiento futuro.

#### Esquemas mínimos propuestos de Skills

```json
{
  "skillId": "uuid",
  "versionId": "uuid",
  "version": 3,
  "name": "Guía de feedback",
  "description": "Instrucciones pedagógicas para feedback formativo.",
  "visibility": "PUBLIC",
  "state": "AVAILABLE",
  "contentHash": "sha256-hex",
  "tags": ["feedback"],
  "associatedCourseIds": ["uuid"],
  "authorId": "uuid",
  "createdAt": "date-time"
}
```

`state=AVAILABLE` es sólo un nombre de propuesta para la versión seleccionable.
Los estados de seguridad definitivos no se congelan hasta retomar el punto 4.

### 2.3 Endpoints faltantes de calibración — PROPUESTOS

Los siguientes recursos materializan D-161 a D-164. El borrador es privado de
su creador salvo lectura administrativa autorizada; una corrida y activación
entran al historial compartido del curso conforme a permisos vigentes.

| Método y ruta propuesta | Entrada mínima | Salida mínima | Regla |
|---|---|---|---|
| `GET /api/llm/courses/{courseId}/calibration-drafts` | `scope`, `challengeId` opcional, paginación. | `CalibrationDraftPage`. | Sólo borradores del actor; Admin sólo lectura según la política vigente. |
| `POST /api/llm/courses/{courseId}/calibration-drafts` | `scope=COURSE|CHALLENGE`, `challengeId` cuando aplique y selección parcial de recursos. | `201 CalibrationDraft`. | No invoca proveedor ni exige configuración completa. |
| `GET /api/llm/courses/{courseId}/calibration-drafts/{draftId}` | Path. | `CalibrationDraft`. | `404` fuera de actor/curso. |
| `PATCH /api/llm/courses/{courseId}/calibration-drafts/{draftId}` | `If-Match`; cambios parciales de recursos/overlay. | Borrador actualizado. | Si hay conflictos de rebase, devuelve el borrador `CONFLICTED`. |
| `DELETE /api/llm/courses/{courseId}/calibration-drafts/{draftId}` | Path. | `204`. | Sólo borra un borrador no iniciado; la semántica física queda sujeta a retención. |
| `POST /api/llm/courses/{courseId}/calibration-drafts/{draftId}/runs` | Sin body. | `202 CalibrationRun` y `Location`. | Materializa snapshot; valida recursos, baseline y reglas de inicio. |
| `GET /api/llm/courses/{courseId}/calibration-status` | Path. | `CalibrationStatus`. | Consulta agnóstica para el curso registrado. |
| `GET /api/llm/courses/{courseId}/challenges/{challengeId}/calibration-status` | Path. | `CalibrationStatus`. | Consulta agnóstica para el desafío registrado. |
| `GET /api/llm/courses/{courseId}/deferred-evaluation-summary` | Path. | `DeferredEvaluationSummary`. | Sólo indicadores, nunca datos de alumnos/entregas. |
| `POST /api/llm/courses/{courseId}/calibration-runs/{runId}/activation-previews` | Sin body. | `201 ActivationPreview`. | Fija revisiones y asociaciones migrables/bloqueadas. |
| `POST /api/llm/courses/{courseId}/calibration-activations` | `calibrationRunId`, `previewId`, `migrateChallengeIds`, `reason` sólo para reactivación histórica. | `201 CalibrationActivation`. | Rechaza preview vencido/obsoleto o desafío bloqueado. |
| `POST /api/llm/admin/calibration-activations/{activationId}/verifications` | `reason` obligatorio. | `202 CalibrationVerification` y `Location`. | Ejecución manual; no usa fallback. |
| `POST /api/llm/admin/deferred-evaluations/{evaluationId}/resumptions` | `reason` obligatorio. | `202 DeferredEvaluation`. | Sólo desde `RETRY_EXHAUSTED`; no crea otro score. |

#### Esquemas mínimos propuestos

```json
// GET calibration-status
{
  "registered": true,
  "calibrationValidity": "VALID",
  "evaluatorAvailability": "AVAILABLE",
  "effectiveCalibrationRunId": "uuid | null"
}
```

`calibrationValidity` propone: `NO_ACTIVE`, `VALID`,
`VERIFICATION_PENDING`, `RECALIBRATION_REQUIRED`, `SECURITY_SUSPENDED`.
`evaluatorAvailability` propone `AVAILABLE|UNAVAILABLE`. Esta separación
reemplazaría el `calibrationState` único de D-81, sólo si se aprueba antes de
editar OpenAPI.

```json
// GET deferred-evaluation-summary
{
  "pendingCount": 12,
  "oldestPendingAt": "date-time | null",
  "retryExhaustedCount": 1,
  "hasRetryExhausted": true,
  "updatedAt": "date-time"
}
```

`pendingCount` incluye `DEFERRED`, `QUEUED`, `RUNNING`, `RETRY_WAIT` y
`RETRY_EXHAUSTED`; sólo `COMPLETED` resuelve el pendiente.

### 2.4 Salidas HTTP que LLM requiere de otros servicios — PROPUESTAS

| Consumidor/servidor | Llamada que realiza LLM | Datos mínimos requeridos | Estado |
|---|---|---|---|
| `courses-service` | `GET /api/courses/{courseCohortId}/members/{userId}` por Gateway. | Cohorte, usuario, rol y estado de membresía. | Pendiente de aprobación de Courses. |

LLM no consulta ni escribe directamente en `challenges-service`. La selección y
estado de desafíos se obtienen mediante los contratos que acuerde el dueño del
catálogo, sin acceder a su base de datos.

### 2.5 Contrato interno con AI Gateway — PROPUESTO

No es un endpoint público. Todo controller y worker usa el AI Gateway interno;
ningún componente llama directamente al proveedor.

Entrada mínima al Gateway: `invocationId`, `profileHash`, snapshot del perfil
efectivo, deployment, esquema JSON estricto y evidencia permitida. Salida
esperada del modelo:

```json
{
  "schemaVersion": "1.0",
  "profileHash": "sha256-hex",
  "scores": [{ "unitId": "AUTONOMY", "score": 74 }]
}
```

El validador exige unidades activas exactas, sin faltantes/duplicados, y scores
enteros de 0 a 100. LLM calcula pesos, final y PAR-14; el modelo no los decide.

## 3. Contratos asíncronos

### 3.1 Eventos publicados actualmente por LLM

El [AsyncAPI actual](llm-service.asyncapi.yaml) declara estado `PROPOSED` e
implementación `PARTIAL`. Los siguientes topics están definidos en ese schema,
pero requieren confirmación de consumidores antes de considerarse acordados.

| Topic | `data` mínima | Consumidor previsto |
|---|---|---|
| `score_de_ia_calculado.v1` | `evaluationId`, `attemptId`, `challengeId`, `courseCohortId`, `learnerId`, estado, score agregado/dimensiones, rúbrica, corrida y fecha. | `practice-service`. |
| `score_pendiente_diferido.v1` | Intento, desafío, cohorte, alumno, causa, fecha y próximo reintento opcional. | Practice y servicio que controla cierre. |
| `calibracion_aprobada.v1` | Cohorte, corrida, rúbrica, Golden Set, métricas y fecha. | Courses y Admin. |
| `calibracion_fuera_de_tolerancia.v1` | Mismos IDs, código de fallo, métricas y fecha. | Courses y Admin. |
| `incidente_de_jailbreak.v1` | Incidente, intento, desafío, cohorte, alumno, función y severidad. | Admin y Seguridad. |

### 3.2 Eventos que LLM consume — REQUISITOS PENDIENTES

| Topic solicitado | Productor dueño | `data` mínima requerida por LLM | Efecto idempotente |
|---|---|---|---|
| `practica_publicada.v1` | `practice-service` | `challengeId`, `courseCohortId`, `publishedAt`, `publicationVersion`. | Crea/recupera asignación de calibración. |
| `intento_iniciado.v1` | `practice-service` | `attemptId`, `challengeId`, `courseCohortId`, `learnerId`, `startedAt`. | Bloquea la asignación del desafío. |
| `intento_cerrado.v1` | `practice-service` | `attemptId`, `challengeId`, `courseCohortId`, `learnerId`, `closedAt`, transcript ordenada y contexto mínimo. | Crea/recupera una única `DeferredEvaluation`. |
| Evento de curso/requisito de calibración | `courses-service` | Tipo/ID de recurso, `courseId`, activo, `requiresCalibration`, nombre snapshot y versión. | Actualiza el registro acotado de LLM. |
| Evento de archivo/cierre de curso | `courses-service` | `courseId`, instante y estado final. | Aún pendiente de política y nombre final. |

Los eventos de entrada deben acordarse con sus dueños y añadirse al AsyncAPI
como operaciones `receive`. LLM rechaza eventos inválidos sin efecto parcial y
aplica la política de DLQ de plataforma.

### 3.3 Eventos nuevos propuestos por Skills y calibración

| Topic propuesto | Hecho | `data` mínima propuesta | Consumidores a acordar |
|---|---|---|---|
| `calibracion_activada.v1` | Se crea o reemplaza una activación. | `activationId`, alcance, `courseId`, `challengeId` opcional, `calibrationRunId`. | Servicios que proyectan disponibilidad. |
| `calibracion_suspendida.v1` | Una activación deja de ser apta. | `activationId`, alcance, recurso, `reasonCode`. | Consumidores de estado y Notificaciones. |
| `score_diferido_reintentos_agotados.v1` | Un pendiente llega al límite. | `evaluationId`, intento, curso, desafío, contador. | Notificaciones y operación. |
| `evaluador_indisponible.v1` | Se abre indisponibilidad total del evaluador. | `incidentId`, deployment, `reasonCode`, recursos afectados sólo si se acuerda. | Notificaciones. |
| `evaluador_recuperado.v1` | Se recupera una indisponibilidad. | `incidentId`, deployment, fecha. | Notificaciones. |

`calibracion_aprobada.v1` no equivale a `calibracion_activada.v1`: la primera
confirma PAR-14 de una corrida; la segunda cambia el puntero efectivo. Tampoco
una suspensión equivale a una falla de ejecución. Los topics se mantienen
separados para no mezclar hechos de dominio.

### 3.4 Solicitud hacia Notificaciones — PROPUESTA

Se propone `notificacion_solicitada.v1`. LLM publica la solicitud por outbox;
`notification-service` persiste, entrega y administra leído/no leído. No se
incluyen Markdown, prompts, transcripciones, secretos ni resultados de alumnos.

```json
{
  "notificationRequestId": "uuid",
  "incidentId": "uuid | null",
  "type": "CALIBRATION_SUSPENDED",
  "severity": "HIGH",
  "recipientScope": {
    "kind": "ADMIN_ROLE | COURSE_AUTHORIZED_TEACHERS | USER",
    "courseId": "uuid | null",
    "userId": "uuid | null"
  },
  "resource": {
    "courseId": "uuid | null",
    "challengeId": "uuid | null"
  },
  "reasonCode": "string",
  "occurredAt": "date-time"
}
```

La deduplicación técnica es por `eventId`. Para evitar avisos de negocio
repetidos, LLM propone unicidad de outbox por
`(incidentId, type, recipientScope, phase)`, donde `phase` puede ser `OPENED`,
`RETRY_EXHAUSTED` o `RECOVERED`. Topic, destinatarios, severidades y DLQ deben
ser aprobados por `notification-service`.

## 4. Bloqueos antes de publicar schemas

1. Aprobar D-162 a D-166; hoy son propuestas, no decisiones implementables.
2. Congelar nombres de rutas, scopes y campos con cada consumidor HTTP.
3. Resolver la equivalencia o mapeo entre `courseId` de HTTP y
   `courseCohortId` de eventos existentes.
4. Decidir la evolución de cinco dimensiones fijas hacia dimensiones/subcriterios
   extensibles antes de alterar schemas de scores o Golden Set.
5. Acordar los eventos recibidos con `practice-service` y `courses-service`, y
   el contrato de solicitudes con `notification-service`.
6. Al aprobar, actualizar el YAML correspondiente, el documento por equipo y
   las pruebas de contrato en el mismo cambio. No editar destructivamente `.v1`.
