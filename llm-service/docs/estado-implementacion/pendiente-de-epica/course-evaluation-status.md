# Estado de evaluación por curso — sin ficha de historia

- **Estado:** ⚪ No auditado en profundidad
- **Épica tentativa:** EP-04 (calibración activa) y/o EP-06/S4 según
  [`verificacion-v2-golden-set-calibracion.md` · §1](../../entregas/verificacion-v2-golden-set-calibracion.md) —
  sin confirmar
- **Código:** `CourseEvaluationStatusController` + `CourseEvaluationStatusRepository`

## Qué se sabe

Expone tres endpoints coherentes con el esquema (`active_calibrations`,
`challenge_calibration_assignments`, `pending_evaluations`) — pensados para que
`courses-service` consulte la calibración activa de un curso antes de activarlo
([08 · B-4](../../08-decisiones-y-pendientes.md)) y para evaluaciones diferidas por falta de
calibración vigente. `verificacion-v2-golden-set-calibracion.md` (§7) marca explícitamente que
**no revisó las queries línea por línea** — la forma de los endpoints es coherente, pero no hay
auditoría de fondo.

## Por qué la épica es incierta

El endpoint vive junto al código de calibración (EP-04), pero conceptualmente "evaluación
pendiente" es el dominio de EP-06 (Evaluación, score y auditoría académica). Ninguna auditoría
previa decidió cuál de las dos manda. Confirmar con quien mantiene el módulo antes de escribir
una ficha.
