# Reubicación de `Plan_skills_calibracion.md` y su prompt de implementación — 2026-09-22

## Decisión anterior

`llm-service/Plan_skills_calibracion.md` (2432 líneas, fuente de las 166 directivas D-01 a D-166)
y `llm-service/PROMPT_AGENTE_IMPLEMENTACION_SKILLS_CALIBRACION.md` (147 líneas, prompt para lanzar
la implementación en código, todavía sin ejecutar) vivían sueltos en la raíz de `llm-service/`,
fuera de `docs/`.

## Motivo

El usuario pidió sacarlos de la raíz porque ensucian la organización del proyecto, aclarando que
son archivos reales (todavía no usados para implementar código, pero activos como fuente de
decisiones) y no descartables. Ya estaban citados 3 veces desde `docs/registro/` como fuente de
verdad de la especificación de skills/calibración jerárquica ya reflejada en
`03-capacidades-de-ia/golden-set-y-calibracion/05-09`.

## Regla vigente

- `Plan_skills_calibracion.md` → movido a
  [`03-capacidades-de-ia/golden-set-y-calibracion/plan-skills-calibracion.md`](../03-capacidades-de-ia/golden-set-y-calibracion/plan-skills-calibracion.md).
- `PROMPT_AGENTE_IMPLEMENTACION_SKILLS_CALIBRACION.md` → movido a
  [`03-capacidades-de-ia/golden-set-y-calibracion/prompt-agente-implementacion-skills-calibracion.md`](../03-capacidades-de-ia/golden-set-y-calibracion/prompt-agente-implementacion-skills-calibracion.md).
- Mismo criterio que `plan-calibracion-real-y-activacion-par14.md`, que ya vivía en esa carpeta con
  el mismo patrón: plan pendiente de implementación, marcado explícitamente como tal en el README
  de la carpeta (no como regla vigente ni código ya construido).
- El prompt de implementación (referencias a `docsV2` y a la ruta vieja del plan) se actualizó para
  apuntar a las rutas reales de `docs/`.

## Documentos corregidos o agregados

**Movidos:** `Plan_skills_calibracion.md`, `PROMPT_AGENTE_IMPLEMENTACION_SKILLS_CALIBRACION.md`
(ambos con `git mv`, conservan historial).

**Actualizados:** `docs/03-capacidades-de-ia/golden-set-y-calibracion/README.md` (nuevas entradas),
`registro/2026-09-22-skills-y-calibracion-jerarquica.md`,
`registro/coverage-matrix-agente-A-skills-calibracion.md`,
`registro/coverage-matrix-agente-B-cuotas-contratos.md` (rutas actualizadas).

## Evidencia de prueba

2988 enlaces relativos de `docs/` verificados, 0 rotos por este cambio (el único roto es el mismo
preexistente y ajeno de siempre, ya señalado en limpiezas anteriores).
