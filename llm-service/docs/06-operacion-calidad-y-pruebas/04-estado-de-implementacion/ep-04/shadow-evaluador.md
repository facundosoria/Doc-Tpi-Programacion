# Shadow del evaluador (E-31) — sin ficha de historia, EP-04

- **Estado:** 🟢 **Fase 1 construida** (2026-09-19) · ⬜ Fase 2 (tráfico real) bloqueada
- **Épica:** **EP-04** (calibración y gobernanza del modelo): valida una `rubric_version` antes de calibrar.
- **Diseño y decisiones:** [`04-shadow-del-evaluador.md`](../../../03-capacidades-de-ia/golden-set-y-calibracion/04-shadow-del-evaluador.md)
- **Código:** `llm-service/.../llm/shadow/` (`ShadowRunController`, `ShadowRunService`, `ShadowEvaluationRunner`,
  `ShadowRunWorker`, `ShadowMetrics`, `JdbcShadowRunStore`, `GoldenSetShadowSource`, `TutorConversationShadowSource`),
  migración `V37__shadow_evaluation.sql`, y `EvaluatorPrompt` (extraído de `CalibrationEvaluationRunner` para que
  calibración y shadow midan lo mismo).

## Qué hace hoy

Evalúa una rúbrica candidata y la activa del curso sobre las mismas transcripciones **ya guardadas** (casos de un golden
set o conversaciones del tutor), compara los puntajes y guarda solo la comparación. No emite scores, eventos ni toca la
calibración ni las evaluaciones reales (lo hace cumplir `ArchitectureTest`).

## Verificación

- Unitarios: `ShadowMetricsTest` (10), `ShadowEvaluationRunnerTest` (8), `ShadowRunServiceTest` (8),
  `ShadowRunControllerTest` (2), `EvaluatorPromptTest` (3) y la regla nueva de `ArchitectureTest`.
- Integración contra Postgres real: `it/ShadowPersistenceIT` (8): idempotencia y constraints, baseline y visibilidad por
  curso, corrida completa sin tocar `event_outbox`/`calibration_case_results`/`pending_evaluations`, casos fallidos,
  reclamo exclusivo y reintento tras caída, purga, fuente del tutor y endpoint con autorización.
- **No probado** contra un proveedor de LLM real: el evaluador se simuló en todos los tests.

## Huecos

- 🟡 Solo compara `rubric_version` (no hay `prompt_version` versionado).
- 🟡 Umbrales de la recomendación (`mae ≤ 5`, `|bias| ≤ 3`, `≤ 20 %` fallidos) **propuestos**, sin acordar con el equipo.
- ⬜ Fase 2: enganchar la copia en sombra al pipeline de evaluación de intentos reales, que no existe todavía
  (ver [`pending-evaluations-gate.md`](pending-evaluations-gate.md): `markRunning` solo cambia un estado).
