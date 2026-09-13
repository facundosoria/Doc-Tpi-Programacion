# Compuerta de evaluaciones pendientes por falta de calibración — confirmado EP-04

- **Estado:** 🟢 Construida (para lo que hace) — **no es evaluación real, es una compuerta**
- **Épica:** **EP-04** (calibración) — confirmado en esta auditoría (2026-09-12), reemplaza la
  duda de [`pendiente-de-epica/course-evaluation-status.md`](../pendiente-de-epica/course-evaluation-status.md)
  (archivo retirado, contenido migrado acá).
- **Código:** `CourseEvaluationStatusController`, `CourseEvaluationStatusRepository`,
  `EvaluationAvailabilityService`, `PendingEvaluationWorker`, `CalibrationWorkflowService`
  (`queue`/`resumeNext`).

## Por qué es EP-04 y no EP-06

El propio código lo dice: el Javadoc de `CourseEvaluationStatusController` es explícito — *"Status
for AI-usage evaluations only; this controller never exposes academic grading."* Lo que expone son
tres lecturas: la calibración activa de un curso, los desafíos con calibración asignada/bloqueada,
y las evaluaciones que quedaron en cola. **Ninguna calcula ni devuelve un puntaje.**

`EvaluationAvailabilityService.queueWhenUnavailable` y `CalibrationWorkflowService.queue`/
`resumeNext` implementan **una sola compuerta**: si el desafío de un intento no tiene calibración
activa válida (`hasValidCalibration`), el intento se encola (`pending_evaluations`) en vez de
intentar evaluarse; `PendingEvaluationWorker` (cada 5 s) reclama la cola y, apenas el desafío tiene
calibración válida, marca la fila `RUNNING` con `markRunning`.

**Y ahí termina.** `markRunning` solo cambia un estado en la base — no invoca ningún modelo, no
calcula ninguna dimensión, no persiste ningún puntaje. Es la mitad de un mecanismo: la parte que
"no evalúa hasta que hay con qué" está lista; la parte que "evalúa de verdad" no existe en ningún
lugar del código (ver [`../ep-06/evaluacion-y-apelacion.md`](../ep-06/evaluacion-y-apelacion.md)).

## Veredicto

| Pieza | Qué hace | Estado |
|---|---|---|
| Lectura de calibración activa / asignaciones / pendientes por curso | Tres `GET` de solo lectura, autorizados por curso | 🟢 |
| Compuerta "no evaluar sin calibración válida" | `queueWhenUnavailable` + `hasValidCalibration` | 🟢 (para lo que cubre — ver hueco abajo) |
| Reanudación de pendientes cuando aparece calibración válida | `PendingEvaluationWorker` + `resumeNext` | 🟡 solo cambia el estado a `RUNNING`; no dispara ninguna evaluación real después |
| Evaluación real del intento (dimensiones, puntaje, justificación) | — | 🔴 no existe en ningún lugar del código |

**Hueco no cubierto por esta compuerta:** el requisito real de EP-06 es "si el **evaluador**
(modelo/proveedor) está caído, la entrega se acepta igual y el puntaje llega después" — un fallo
de disponibilidad del modelo, no de calibración. Esta pieza solo cubre el caso "el desafío todavía
no tiene calibración asignada", que es un caso distinto (y más fácil) del que pide la épica.
