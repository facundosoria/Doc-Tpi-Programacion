# Tareas del `llm-service` — fichas SMART por historia

> **Qué es esta carpeta.** El desglose en **tareas** de cada Historia de Usuario, en el mismo
> patrón que [`../historias/`](../historias/README.md): **una carpeta por épica, un archivo
> por historia** (`ep-0X/hYY.md` o `ep-0X/sZZ-hYY.md`), con el formato del [template oficial
> de Tarea de la Wiki de Taiga](../plantillas/tarea-taiga.md). En Taiga cada tarea **cuelga de
> su HU** (o de la tarea de sprint, si el padre es un habilitador).
>
> **Método: SMART.** Una tarea está bien escrita si es **S**pecífica (un solo paso
> técnico, verbo + resultado), **M**edible (su criterio de terminado se responde sí/no),
> **A**lcanzable (cabe en ≤ 1 jornada efectiva, una persona o pareja), **R**elevante
> (traza a un CA o escenario BDD de la historia padre) y **A**cotada en el tiempo
> (estimación en horas; si supera la jornada, se parte). Las tareas **no** llevan
> `Como / Quiero / Para` ni puntos Fibonacci: eso es de las historias
> ([29 · §4](../29-guia-catedra-historias-de-usuario.md), [23 · §9.2](../23-plan-construccion-producto-llm.md)).
>
> **Fuente de verdad.**
>
> | Dato | Dónde vive |
> |---|---|
> | Horas por historia y por sprint (S1) | [`35`](../35-backlog-ejecutable.md) · «S1» |
> | Criterios de aceptación y escenarios BDD (lo que cada tarea traza) | [`../historias/`](../historias/README.md) |
> | Orden de construcción de las tareas | [playbook · §4](../36-playbook-de-construccion.md) |
> | Vista del sprint (índice, demo) | [`../sprints/s1-historias.md`](../sprints/s1-historias.md) |
> | Estado real del código (S2/S3, escritas a posteriori) | [`../entregas/verificacion-v2-golden-set-calibracion.md`](../entregas/verificacion-v2-golden-set-calibracion.md) |
>
> Las horas por tarea son **orientativas** y suman la referencia de la historia. Si un
> número de acá no coincide con la fuente, **manda la fuente**. En las historias escritas a
> posteriori (S2, S3), la mayoría de las tareas ya están **hechas** — están marcadas
> explícitamente; no son planificación a futuro.

## Índice por épica

| Épica | Carpeta | Historias | Estado |
|---|---|---|---|
| **EP-01** · Plataforma, contratos e integración | [`ep-01/`](ep-01/README.md) | LLM-S01-H01 · H02 · H03 · H04 · H08 · H09 | Planificadas (S1) |
| **EP-02** · AI Gateway, modelos y resiliencia | [`ep-02/`](ep-02/README.md) | LLM-S01-H10 | Planificada, adelantada a S1 |
| **EP-03** · Golden set y referencia humana | [`ep-03/`](ep-03/README.md) | LLM-S01-H05 · H06 · H07 (histórico) · LLM-S02-H01 · H02 (vigentes) | S1 histórico; S2 ya construida |
| **EP-04** · Calibración y gobernanza del modelo | [`ep-04/`](ep-04/README.md) | LLM-S03-H01 | Esqueleto construido, 1 tarea bloqueante |

## Horas — S1 (planificación original, vigente)

**Total S1:** 240 h de tareas (piso de planificación, no tope).

| Historia | Archivo | Tareas | h |
|---|---|---:|---:|
| LLM-S01-H01 | [`ep-01/h01.md`](ep-01/h01.md) | 5 | 16 |
| LLM-S01-H02 | [`ep-01/h02.md`](ep-01/h02.md) | 7 | 30 |
| LLM-S01-H03 | [`ep-01/h03.md`](ep-01/h03.md) | 8 | 34 |
| LLM-S01-H04 | [`ep-01/h04.md`](ep-01/h04.md) | 7 | 38 |
| LLM-S01-H05 *(histórico)* | [`ep-03/h05.md`](ep-03/h05.md) | 6 | 24 |
| LLM-S01-H06 *(histórico)* | [`ep-03/h06.md`](ep-03/h06.md) | 4 | 14 |
| LLM-S01-H07 *(histórico)* | [`ep-03/h07.md`](ep-03/h07.md) | 6 | 24 |
| LLM-S01-H08 | [`ep-01/h08.md`](ep-01/h08.md) | 4 | 10 |
| LLM-S01-H09 | [`ep-01/h09.md`](ep-01/h09.md) | 6 | 18 |
| LLM-S01-H10 | [`ep-02/h10.md`](ep-02/h10.md) | 6 | 32 |
| **Total** | | **59** | **240** |

## Horas — S2/S3 (escritas a posteriori, código ya construido)

No suman al total de S1: son la documentación retroactiva de `605f381`
([decision-605f381.md](../entregas/decision-605f381.md)). Las horas son de referencia, no un
compromiso de sprint.

| Historia | Archivo | Tareas | Estado |
|---|---|---:|---|
| LLM-S02-H01 | [`ep-03/s02-h01.md`](ep-03/s02-h01.md) | 6 | 5 hechas, 1 pendiente de decisión |
| LLM-S02-H02 | [`ep-03/s02-h02.md`](ep-03/s02-h02.md) | 7 | 6 hechas, 1 pendiente de código |
| LLM-S03-H01 | [`ep-04/s03-h01.md`](ep-04/s03-h01.md) | 7 | 6 hechas, 1 bloqueante (conectar H10) |

## Cómo se generan

Con el skill [`generar-tareas`](../../.agents/skills/generar-tareas/SKILL.md): toma una
HU con sus CA y su BDD y produce sus tareas SMART en el orden de construcción del equipo.
