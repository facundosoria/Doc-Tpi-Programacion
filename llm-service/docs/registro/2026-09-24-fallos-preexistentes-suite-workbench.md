# Fallos pre-existentes de la suite de `llm-workbench` — 2026-09-24

## Decisión anterior

La US #174 (`FRONTLLM.md`, tareas #742–#747, read-only histórica) se limita al microservicio
`llm-service` y sus cambios se confinan a `llm-workbench/` (pantalla golden-set del docente),
actualmente sobre `integracion/main-a-dev`. Antes de esta verificación, la suite de tests del
workbench figuraba completamente verde sin distinción de origen de los fallos.

## Motivo

Al correr la suite completa de `llm-workbench` (`npm test` → `ng test`, builder
`@angular/build:unit-test`, vitest 4 + jsdom, 2026-09-24) el resultado es **23/26 en verde y 3
test que fallan**. Los 3 fallos **no pertenecen a la US #174** (no tocan la pantalla golden-set —
cuyos 11 tests pasan en verde) sino a dos pantallas ajenas: summary-page y evaluator-shell.
Su causa es **pre-existente** en la rama (confirmado con `git stash` del baseline) y corresponde a
otras entregas, por lo que, según la regla de `AGENTS.md` ("si al implementar la tarea X detectas
que Y está roto, no lo refactorices por iniciativa propia; repórtalo primero"), quedan **reportados
acá y sin tocar** hasta que el equipo dueño los resuelva.

### Fallo 1 y 2 — `summary-page.spec.ts` (spec desactualizada contra el flujo v2)

- Archivo: `llm-workbench/src/app/teacher/summary-page/summary-page.spec.ts`
- Tests: "renders operational state with active calibration, quick access to drafts and no alerts
  (GS-0510, GS-0512)" y "renders actionable alerts when calibration is missing, evaluations are
  pending, and base proposal exists (GS-0511)".
- Causa: la spec sigue esperando el endpoint `GET /api/llm/courses/{id}/base-update-proposals`
  (líneas 33 y 84) y aseriendo `hasBaseProposal() === true` (línea 101), pero el componente ya no
  llama a ese endpoint tras el flujo v2 (post `605f381`): las únicas llamadas reales del summary son
  `rubrics`, `golden-sets`, `calibrations` y `pending-evaluations`, y `hasBaseProposal` quedó
  hardcodeado en `false`. La spec quedó obsoleta contra el componente que hoy existe.
- Impacto real en producción: ninguno — es un desfase spec↔código, no una regresión del código.

### Fallo 3 — `evaluator-shell.spec.ts` (gap de robustez: 404 de `active-calibration`)

- Archivo: `llm-workbench/src/app/teacher/evaluator-shell/evaluator-shell.spec.ts`
- Test: "orients the teacher within a course and exposes all evaluator sections as links".
- Causa: la spec flushea `GET /api/llm/courses/{id}/active-calibration` con **404** (caso legítimo:
  curso sin calibración activa), pero el `computed` `hasActiveCalibration` del shell (línea 40)
  lee `.value()` sobre el `httpResource` en estado de error → `ResourceValueError` que revienta la
  plantilla. Es un **gap real de robustez** del componente: un 404 debería tratarse como "sin
  calibración" (el label de la línea 41 ya contempla el error, pero el `computed` lo pisa antes).
- Impacto real en producción: el shell puede romper el render si el backend responde 404 en ese
  endpoint en vez de `{ ... }`/vacío; amerita fix del componente (tratar `error().status === 404`
  como "sin calibración") junto con su spec en verde.

## Regla vigente

- Los 3 fallos de summary-page/evaluator-shell son **pre-existentes y ajenos a la US #174**; quedan
  reportados y **pendientes del equipo dueño**, sin corrección en esta entrega.
- La US #174 se evalúa por la pantalla golden-set y sus 11 tests (verdes), `npx tsc
  -p tsconfig.app.json --noEmit` y `ng build --configuration=production` (ambos OK).
- Cualquier fix de estos fallos debe ser una tarea aparte con su propia verificación (spec en verde
  + suite completa), no una corrección cosmética en el código de otra pantalla.

## Fuentes

- Corrida `npm test` (ng test, vitest 4.1.11 + jsdom) del 2026-09-24 en
  `llm-service/llm-workbench`: **7 test files / 26 tests — 3 failed, 23 passed**.
  - `summary-page.spec.ts`: 2 failed (GS-0510/GS-0512 y GS-0511) — "Expected one matching request
    for criteria Match URL: .../base-update-proposals, found none".
  - `evaluator-shell.spec.ts`: 1 failed — `ResourceValueError: Resource is currently in an error
    state ... 404 Not Found` en `hasActiveCalibration` (unhandled en la plantilla).
  - `golden-set-page.spec.ts`: **11/11 passed** (US #174).
- `git stash` del baseline en `integracion/main-a-dev`: los mismos 3 fallos existen sin los cambios
  de la US (pre-existentes, no introducidos por esta entrega).
- Archivos leídos: `summary-page.spec.ts` (líneas 33, 84, 101), `evaluator-shell.ts` (líneas 40-41).

## Documentos corregidos

- Creado: `docs/registro/2026-09-24-fallos-preexistentes-suite-workbench.md` (este documento) y su
  fila en `docs/registro/README.md`.
- No se tocó código: `llm-workbench/` solo conserva los 4 archivos de la US #174 ya reportados
  previamente (golden-set-page/…).

## Responsable y evidencia

- Responsable del reporte: agente según US #174 (llm-service / llm-workbench).
- Dueño del fix (candidatos): pantallas summary-page y evaluator-shell, fuera del alcance de la US
  #174.
- Evidencia: salida completa de `ng test` del 2026-09-24 (resumen 3 failed / 23 passed; detalle de
  cada fallo con líneas de spec), disponible en el historial de la sesión.