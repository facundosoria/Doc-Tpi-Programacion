# Historias de usuario — EP-01 · Plataforma, contratos e integración

> Fichas en el formato del [template oficial de Historia de Usuario de la Wiki de
> Taiga](../../plantillas/historia-de-usuario-taiga.md). Estas seis historias (**H01–H04,
> H08, H09**) son **habilitadores técnicos** de S1: fallan la **V** de INVEST (el `COMO`
> es «el equipo» o «la plataforma»), así que en Taiga se cargan como **tareas** bajo
> EP-01, sin puntos de valor. El formato largo acá es para trazar los escenarios de
> aceptación.
>
> **Fuente de verdad.** ID, épica, pareja, dependencias y horas mandan desde
> [`35` · «S1»](../../35-backlog-ejecutable.md).
> Tipo (HU / habilitador) y demo, desde [30 · §5](../../30-arranque-agil-y-sprint-0.md).
> DoR/DoD, desde [23 · §9.2](../../23-plan-construccion-producto-llm.md). Método, desde
> [29](../../29-guia-catedra-historias-de-usuario.md). El desglose en **tareas SMART** de
> cada historia está en [`../../tareas/ep-01/`](../../tareas/ep-01/README.md).
>
> **Estimación en puntos.** Ninguna ficha trae puntos Fibonacci: se asignan en el
> **Sprint 0** con Planning Poker contra la historia canónica (`LLM-S01-H06`). La columna
> *h* del plan es la referencia de planificación y **no se convierte** a puntos.

## Índice

| ID | Título | Tipo | Pareja | Dep. | h |
|---|---|---|---|---|--:|
| [LLM-S01-H01](h01.md) | ADR de arquitectura y convenciones técnicas | Tarea (habilitador) | P1 | — | 16 |
| [LLM-S01-H02](h02.md) | Entorno reproducible con un comando | Tarea (habilitador) | P1 | H01 | 30 |
| [LLM-S01-H03](h03.md) | Esqueleto transversal del servicio | Tarea (habilitador) | P1 | H02 | 34 |
| [LLM-S01-H04](h04.md) | Esquema inicial versionado con auditoría | Tarea (habilitador) | P1 | H03 | 38 |
| [LLM-S01-H08](h08.md) | Contrato OpenAPI y mock del golden set publicados | Tarea (habilitador) | P1 | H03 | 10 |
| [LLM-S01-H09](h09.md) | Suite de pruebas y guía de demo de S1 | Tarea (habilitador) | todos | H04–H07 | 18 |

> Las HU de valor de S1 (**H05–H07**, EP-03) están en [`../ep-03.md`](../ep-03.md). La vista
> completa del sprint (índice de las 9, demo) está en
> [`../../sprints/s1-historias.md`](../../sprints/s1-historias.md).
