# Tareas del `llm-service` — fichas SMART por historia

> **Qué es esta carpeta.** El desglose en **tareas** de cada Historia de Usuario, una
> ficha por tarea con el formato del [template oficial de Tarea de la Wiki de
> Taiga](../plantillas/tarea-taiga.md). En Taiga cada tarea **cuelga de su HU** (o de la
> tarea de sprint, si el padre es un habilitador).
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
> | Horas por historia y por sprint | [`35`](../35-backlog-ejecutable.md) · «S1» |
> | Criterios de aceptación y escenarios BDD (lo que cada tarea traza) | [`../historias/`](../historias/README.md) |
> | Orden de construcción de las tareas | [playbook · §4](../36-playbook-de-construccion.md) |
> | Vista del sprint (índice, demo) | [`../sprints/s1-historias.md`](../sprints/s1-historias.md) |
>
> Las horas por tarea son **orientativas** y suman la referencia de la historia. Si un
> número de acá no coincide con la fuente, **manda la fuente**.

## Índice

| Archivo | Historias | Épica |
|---|---|---|
| [`ep-01.md`](ep-01.md) | LLM-S01-H01 · H02 · H03 · H04 · H08 · H09 | EP-01 · Plataforma, contratos e integración |
| [`ep-03.md`](ep-03.md) | LLM-S01-H05 · H06 · H07 | EP-03 · Golden set y referencia humana |

**Total S1:** 208 h de tareas (piso de planificación, no tope).

| Historia | Tareas | h |
|---|---:|---:|
| [LLM-S01-H01](ep-01.md#llm-s01-h01--adr-de-arquitectura-y-convenciones-técnicas) | 5 | 16 |
| [LLM-S01-H02](ep-01.md#llm-s01-h02--entorno-reproducible-con-un-comando) | 7 | 30 |
| [LLM-S01-H03](ep-01.md#llm-s01-h03--esqueleto-transversal-del-servicio) | 8 | 34 |
| [LLM-S01-H04](ep-01.md#llm-s01-h04--esquema-inicial-versionado-con-auditoría) | 7 | 38 |
| [LLM-S01-H05](ep-03.md#llm-s01-h05--alta-de-golden-set-y-carga-de-entradas) | 6 | 24 |
| [LLM-S01-H06](ep-03.md#llm-s01-h06--consulta-del-golden-set-que-sobrevive-al-reinicio) | 4 | 14 |
| [LLM-S01-H07](ep-03.md#llm-s01-h07--pantalla-docente-mínima-del-golden-set) | 6 | 24 |
| [LLM-S01-H08](ep-01.md#llm-s01-h08--contrato-openapi-y-mock-del-golden-set-publicados) | 4 | 10 |
| [LLM-S01-H09](ep-01.md#llm-s01-h09--suite-de-pruebas-y-guía-de-demo-de-s1) | 6 | 18 |
| **Total** | **53** | **208** |

## Cómo se generan

Con el skill [`generar-tareas`](../../.agents/skills/generar-tareas/SKILL.md): toma una
HU con sus CA y su BDD y produce sus tareas SMART en el orden de construcción del equipo.
