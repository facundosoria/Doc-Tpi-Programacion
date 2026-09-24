# Tema 02 — Cursos y Matrícula — contratos

> Este documento es el contrato completo y vigente con Tema 02. Reglas generales (canal
> sync/async, errores, autenticación): [00 — Mapa de integración](../00-mapa-de-integracion.md).
> Courses es dueño del inventario curso–cohorte–desafío; Tema 07 es dueño de rúbricas, Golden
> Sets, corridas, activaciones y evaluaciones diferidas. Tema 07 no consulta la base de datos de
> Courses ni infiere el catálogo a partir de intentos de alumnos.

## Qué nos llama (Courses consultando a Tema 07)

| Operación | Respuesta | Estado |
|---|---|---|
| `GET /api/llm/courses/{courseId}/calibrations` | `CalibrationPage` — historial de corridas del curso. | Definido. |
| `GET /api/llm/courses/{courseId}/calibrations/{runId}` | `CalibrationRun` — estado y métricas de una corrida. | Definido. |
| `GET /api/llm/courses/{courseId}/active-calibration` | `ActiveCalibration` o `404`. | Definido. |
| `GET /api/llm/courses/{courseId}/challenge-calibration-assignments` | Asignación efectiva por desafío. | Definido. |
| `GET /api/llm/courses/{courseId}/pending-evaluations` | Evaluaciones aún pendientes del curso. | Definido. |

La creación, el preview y la activación de una corrida **no** son parte de esta integración: los
invoca Backoffice/Admin — ver [`tema-12-backoffice-admin.md`](tema-12-backoffice-admin.md).

## Qué nos da (Tema 07 consultando a Courses)

Tema 07 necesita sincronizar qué cohortes y desafíos son calibrables. El detalle completo de esta
sincronización (endpoints, paginación, semántica de `courseState`/`publicationState`/
`requiresCalibration`) vive en un único lugar para no duplicarlo:
[`requisitos-a-otros-micros.md`, sección B — RQ-CS-03](../requisitos-a-otros-micros.md#rq-cs-03--catálogo-de-cursos-y-desafíos-calibrables).

También necesita:

- **RQ-CS-01 — Pertenencia docente activa de una cohorte**, antes de administrar rúbricas, Golden
  Sets o calibraciones de esa cohorte. Detalle en
  [`requisitos-a-otros-micros.md`, RQ-CS-01](../requisitos-a-otros-micros.md#rq-cs-01--autorizar-a-un-docente-por-cohorte).
- **RQ-CS-02 — Archivo o cierre de cohorte**, para bloquear nuevas calibraciones y resolver
  evaluaciones pendientes de forma auditable. Detalle en
  [`requisitos-a-otros-micros.md`, RQ-CS-02](../requisitos-a-otros-micros.md#rq-cs-02--archivo-o-cierre-de-cohorte).
  En AsyncAPI este evento es `COURSE_ARCHIVED` (canal `course-events`,
  `x-topic-status: pending-assignment`); el schema hoy no declara campos propios — el `payload`
  de la tabla de RQ-CS-02 es la propuesta de Tema 07, no un acuerdo cerrado.

## Qué le damos (Tema 07 publicando hacia Courses)

Canal `calibration-events` (`x-topic-status: pending-assignment`), envelope de 5 campos —
`eventId`, `eventType`, `timestamp`, `producer`, `payload` — por
[`KAFKA_EVENT_STANDARD.md`](../KAFKA_EVENT_STANDARD.md). Message key: `rubricVersionId`.

| `eventType` | Efecto en Courses |
|---|---|
| `CALIBRATION_APPROVED` | Proyecta que una corrida superó PAR-14. No implica que ya esté activada: la activación es una operación separada (ver Tema 12). |
| `CALIBRATION_OUT_OF_TOLERANCE` | Proyecta que esa corrida no habilita la cohorte. |

⚠️ **El schema ejecutable (`CalibrationResultEvent` en `llm-service.asyncapi.yaml`) hoy es
`Envelope` + `eventType` únicamente — sin `payload` definido.** No hay campos acordados todavía
(courseCohortId, calibrationRunId, métricas, etc. son candidatos razonables, no un contrato
cerrado). No implementar un consumidor asumiendo esos campos sin confirmar con Courses primero.

## Bloqueo crítico (no es un pendiente de definición, es una dependencia externa)

El **Golden Set** (muestras del docente para calibrar la rúbrica). Sin esto ningún curso puede
activarse — es la dependencia con el plazo más largo del proyecto, y no es trabajo de desarrollo
de ningún equipo. Ver
[`product-owner/pendientes.md`](../../07-planificacion-y-trabajo-equipo/11-equipos/product-owner/pendientes.md).

## Pendientes de contrato con Tema 02

Registro vivo de lo que hay que acordar. Detalle y checklist en
[`pendientes.md`](../../07-planificacion-y-trabajo-equipo/11-equipos/tema-02-cursos-y-matricula/pendientes.md).

| Tema | Estado |
|---|---|
| Aprobación del catálogo de calibración (RQ-CS-03: paginación, `updatedSince`, versión) | 🔴 propuesto por Tema 07, sin acordar |
| Campos de `COURSE_ARCHIVED` (hoy `Envelope` sin `payload`) | 🔴 propuesta mínima en RQ-CS-02, sin acordar |
| Campos de `CALIBRATION_APPROVED` / `CALIBRATION_OUT_OF_TOLERANCE` (hoy `Envelope` sin `payload`) | 🔴 sin acordar |
| Ruta, scope y semántica final de `GET /api/courses/{courseCohortId}/members/{userId}` (RQ-CS-01) | 🔴 pendiente de aprobación del dueño de Courses |
| Message key de los topics de Courses hacia Tema 07 | 🔴 sin confirmar |
| _(nuevos temas)_ | agregar acá |
