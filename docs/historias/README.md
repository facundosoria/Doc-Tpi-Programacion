# Historias de usuario del `llm-service` — fichas por épica

> **Qué es esta carpeta.** Las Historias de Usuario del producto, **agrupadas por
> épica** (no por sprint), cada archivo con las fichas en el formato del [template
> oficial de Historia de Usuario de la Wiki de Taiga](../plantillas/historia-de-usuario-taiga.md):
> Como / Quiero / Para, Notas, Criterios de Aceptación con negativos, BDD (≥ 3
> escenarios), Prototipo, Estimación y Dependencias.
>
> La **épica** dice *para qué sirve* la historia en el producto; el **sprint** dice
> *cuándo* se construye. El desglose de cada historia en **tareas SMART** vive en
> [`../tareas/`](../tareas/README.md); la vista de un sprint concreto (índice, demo,
> receta) en [`../sprints/`](../sprints/README.md).
>
> **Qué NO es.** No es fuente de verdad de planificación. Si un dato de una ficha no
> coincide:
>
> | Dato | Fuente única |
> |---|---|
> | ID, épica, pareja, dependencias, horas | [`35`](../35-backlog-ejecutable.md) |
> | Catálogo de épicas (pareja líder, fase, sprints) | [`../epicas/README.md`](../epicas/README.md) |
> | Tipo (HU de valor / habilitador) y demo del sprint | [30 · §5](../30-arranque-agil-y-sprint-0.md) |
> | DoR / DoD | [23 · §9.2](../23-plan-construccion-producto-llm.md) |
> | Método para redactar y estimar | [29 · Guía de cátedra: Historias de Usuario](../29-guia-catedra-historias-de-usuario.md) |
> | Contratos HTTP y adendas | [`../contracts/`](../contracts/) |
> | Desglose en tareas | [`../tareas/`](../tareas/README.md) |
>
> **Título en Taiga.** Cada ficha se carga con el título `GXX — TÍTULO` (`GXX` =
> número de grupo, aún sin asignar). El ID interno `LLM-Sxx-Hyy` es el del equipo.

## Índice por épica

| Épica | Archivo | Historias | Tipo en Taiga |
|---|---|---|---|
| **EP-01** · Plataforma, contratos e integración | [`ep-01.md`](ep-01.md) | LLM-S01-H01 · H02 · H03 · H04 · H08 · H09 | Tareas de sprint (habilitadores) |
| **EP-03** · Golden set y referencia humana | [`ep-03.md`](ep-03.md) | LLM-S01-H05 · H06 · H07 | **HU** de valor (rol: docente) |

> **H05–H07** se cargan en Taiga como **HU** (rol real, puntos Fibonacci contra la
> canónica `LLM-S01-H06`, INVEST verificado). **H01–H04, H08, H09** como **tareas**
> bajo EP-01: fallan la **V** de INVEST (el `COMO` es «el equipo» o «la plataforma»).
> El formato largo de HU acá es para trazar los escenarios de aceptación, no para
> forzar su carga como HU ([30 · §5](../30-arranque-agil-y-sprint-0.md)).

## Historias por sprint

| Sprint | Épicas activas | Historias | Vista de sprint |
|---|---|---|---|
| **S1** | EP-01 (H01–H04, H08, H09) · EP-03 (H05–H07) | 9 | [`../sprints/s1-historias.md`](../sprints/s1-historias.md) · [`../sprints/s1-explicado.md`](../sprints/s1-explicado.md) |
| **S2** | EP-03 | *(a desglosar en el Refinamiento previo a S2)* | — |
| S3–S19 | ver [catálogo](../epicas/README.md) | *(sprint a sprint)* | — |

## Cómo se generan

Con el skill [`generar-historias-usuario`](../../.agents/skills/generar-historias-usuario/SKILL.md).
Las tareas de cada historia, con [`generar-tareas`](../../.agents/skills/generar-tareas/SKILL.md).
