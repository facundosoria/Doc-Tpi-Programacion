# Interacciones elegibles para golden set — confirmado EP-03

- **Estado:** 🔴 Placeholder — datos totalmente hardcodeados
- **Épica:** **EP-03** (golden set y referencia humana) — confirmado en esta auditoría
  (2026-09-12), reemplaza la incertidumbre de
  [`pendiente-de-epica/eligible-interactions.md`](../pendiente-de-epica/eligible-interactions.md)
  (archivo retirado, contenido migrado acá).
- **Código:** `EligibleInteractionsController`, `EligibleInteractionsService`
- **Evidencia:** [`verificacion-v2-golden-set-calibracion.md` · §5](../../entregas/verificacion-v2-golden-set-calibracion.md),
  [`CORRECCIONES-SUGERIDAS.md` ítem 5](../../../CORRECCIONES-SUGERIDAS.md)

## Por qué es EP-03 y no EP-04/EP-06

`GET /api/llm/courses/{courseId}/eligible-interactions` y su `anonymize-preview` no evalúan ni
califican nada — **curan candidatas para agregar al golden set** de un curso: es la misma familia
de operaciones que `CourseGoldenSetService.addCase` ([`ep-03/s02-h02.md`](s02-h02.md)), solo que
en vez de que el docente escriba el caso a mano, se lo ofrece a partir de interacciones reales
(anonimizadas con el mismo `RealCaseAnonymizer`). No tiene relación con calibración (EP-04) ni con
evaluar un intento real (EP-06) — es una herramienta de alta, no de evaluación.

## Qué hay en el código

Dos interacciones **fijas** (UUIDs constantes, texto escrito a mano en `EligibleInteractionsService`)
— no hay integración con `practice-service` ni con ningún historial de chat real. El *preview* de
anonimización sí anonimiza de verdad esos datos de muestra.

## Qué falta

Integración real con `practice-service` para traer interacciones de verdad. Hasta entonces, **no
mostrar esto en una demo sin aclarar que son datos de ejemplo** — mismo riesgo que
[`synthetic-golden-set.md`](synthetic-golden-set.md).
