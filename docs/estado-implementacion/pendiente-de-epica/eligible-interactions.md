# Interacciones elegibles para golden set — sin ficha de historia

- **Estado:** 🔴 Placeholder — datos totalmente hardcodeados
- **Épica tentativa:** ninguna con evidencia clara (candidato natural: EP-03, como insumo para
  cargar un golden set desde interacciones reales de `practice-service`)
- **Código:** `EligibleInteractionsController` + `EligibleInteractionsService`
- **Evidencia:** [`verificacion-v2-golden-set-calibracion.md` · §5](../../entregas/verificacion-v2-golden-set-calibracion.md),
  [`CORRECCIONES-SUGERIDAS.md` ítem 5](../../../llm-service/CORRECCIONES-SUGERIDAS.md)

## Qué hay en el código

Dos interacciones **fijas** (UUIDs constantes, texto escrito a mano) — no hay integración con
`practice-service` ni con ningún historial de chat real. El *preview* de anonimización sí
anonimiza de verdad esos datos de muestra (usa el mismo `RealCaseAnonymizer` que
[`ep-03/s02-h02.md`](../ep-03/s02-h02.md)).

## Qué falta

Integración real con `practice-service` para traer interacciones de verdad. Hasta entonces,
**no mostrar esto en una demo sin aclarar que son datos de ejemplo** — mismo riesgo que
[`synthetic-golden-set.md`](../ep-03/synthetic-golden-set.md).
