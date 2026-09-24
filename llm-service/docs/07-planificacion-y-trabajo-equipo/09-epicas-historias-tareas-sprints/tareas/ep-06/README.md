# Tareas SMART — EP-06 · Evaluación, score y auditoría académica

> **Esta carpeta cubre solo H01–H03 (base de Sprint 1).** Son las tres primeras historias de
> [`../../historias/ep-06/`](../../historias/ep-06/README.md), pensadas para entrar en el mismo
> sprint que lo abierto de EP-04. **H04–H06 (impugnar la nota, corrección docente append-only, y
> consultar si un curso puede cerrarse) son de Sprint 2 y quedan explícitamente fuera de este
> corte** — no tienen ficha de tareas todavía; se desglosan cuando se planifique ese sprint.
>
> Formato del [template de Tarea de Taiga](../../../10-plantillas/tarea-taiga.md); método **SMART**
> ([`../README.md`](../README.md)). Cada tarea cuelga en Taiga de su Historia de Usuario padre —
> salvo las de H01, que cuelgan de la tarea de sprint (H01 es un habilitador, no una HU).
>
> Horas orientativas. H01 y H03 suman su referencia de horas; **H02 no tiene referencia de
> historia** ("a estimar — sin paquete propio en ninguna receta") — su suma de abajo es
> informativa, no un compromiso. Los CA y escenarios que cada tarea traza están en
> [`../../historias/ep-06/`](../../historias/ep-06/README.md).

## Índice

| Historia | Archivo | Tipo | Tareas | h |
|---|---|---|---:|---:|
| LLM-EP06-H01 *(LLM-S06-H01)* | [`h01.md`](h01.md) | Tarea de sprint (habilitador) | 7 | 24 |
| LLM-EP06-H02 | [`h02.md`](h02.md) | **HU** de valor | 8 | *(a confirmar en Refinamiento — 26 h informativas)* |
| LLM-EP06-H03 | [`h03.md`](h03.md) | **HU** de valor | 6 | 15 |
| **Total (H01 + H03, comprometido)** | | | **13** | **39** |

## Fuera de alcance de este corte

| Historia | Título | Tipo | Por qué no está acá |
|---|---|---|---|
| LLM-EP06-H04 | Impugnar la nota que me dio la IA en una entrega | **HU** de valor | Sprint 2 |
| LLM-EP06-H05 | Que el docente pueda corregir la nota de la IA sin borrar lo que ya había | **HU** de valor | Sprint 2, depende de H04 |
| LLM-EP06-H06 | Consultar si un curso puede cerrarse o tiene evaluaciones pendientes de resolver | Tarea (habilitador M2M) | Sprint 2, depende de H01 y H04 |

## Cómo se generaron

Con el skill [`generar-tareas`](../../../../../../.agents/skills/generar-tareas/SKILL.md), a partir de
las fichas completas de [`../../historias/ep-06/h01.md`](../../historias/ep-06/h01.md),
[`h02.md`](../../historias/ep-06/h02.md) y [`h03.md`](../../historias/ep-06/h03.md) — sus CA y
sus escenarios BDD son la fuente de la trazabilidad de cada tarea.
