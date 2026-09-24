# Tareas SMART — EP-07 · Operación, cuotas y observabilidad

> **Alcance de esta carpeta: solo H01–H03 (primera mitad de la épica, Sprint 1).** H04–H06
> (segunda mitad — recuperación de trabajos, salud del servicio, prueba de carga/backup) son
> de **Sprint 2** y **no tienen ficha de tareas todavía**; quedan fuera de este desglose a
> propósito.
>
> Una ficha por historia, igual que
> [`../../historias/ep-07/`](../../historias/ep-07/README.md). Formato del
> [template de Tarea de Taiga](../../../10-plantillas/tarea-taiga.md); método **SMART**
> ([`../README.md`](../README.md)). Cada tarea cuelga en Taiga de su Historia de Usuario (o
> de la tarea de sprint, si el padre es un habilitador — caso de H03).
>
> Horas orientativas: suman la referencia de cada historia, tal como está fijada en la ficha
> de la historia padre (columna «Estimación (plan)»). Los CA y escenarios BDD que cada tarea
> traza están en [`../../historias/ep-07/`](../../historias/ep-07/README.md).

## Índice — H01 a H03 (Sprint 1)

| Historia | Archivo | Tipo | Tareas | h |
|---|---|---|---:|---:|
| LLM-EP07-H01 *(LLM-S09-H01)* — Consultar el panel de costos, cuotas y fallas del servicio | [`h01.md`](h01.md) | HU de valor | 6 | 36 |
| LLM-EP07-H02 *(LLM-S09-H02)* — Configurar un límite de cuota versionado y auditado | [`h02.md`](h02.md) | HU de valor | 7 | 28 |
| LLM-EP07-H03 *(LLM-S09-H03)* — Que el sistema aplique 429 y Retry-After en todas las rutas con límite | [`h03.md`](h03.md) | Tarea habilitadora | 5 | 22 |
| **Total (H01–H03)** | | | **18** | **86** |

## Fuera de alcance (Sprint 2 — sin ficha de tareas todavía)

| Historia | Título | Tipo |
|---|---|---|
| LLM-S10-H01 (H04 de la épica) | Ver, reintentar y recuperar trabajos detenidos sin duplicar resultados | HU de valor |
| LLM-S10-H02 (H05 de la épica) | Que el estado de salud del servicio sea útil aunque el proveedor esté caído | Tarea habilitadora |
| LLM-S10-H03 (H06 de la épica) | Prueba de carga y backup/restore probados con evidencia real | Tarea habilitadora |

Estas tres historias se desglosarán en tareas SMART cuando el equipo entre a Sprint 2; sus
fichas de historia ya existen en
[`../../historias/ep-07/`](../../historias/ep-07/README.md) (`h04.md`, `h05.md`, `h06.md`)
pero no tienen carpeta de tareas equivalente en este momento.

## Cómo se generan

Con el skill [`generar-tareas`](../../../../../../.agents/skills/generar-tareas/SKILL.md): toma una HU
con sus CA y su BDD y produce sus tareas SMART en el orden de construcción del equipo.
