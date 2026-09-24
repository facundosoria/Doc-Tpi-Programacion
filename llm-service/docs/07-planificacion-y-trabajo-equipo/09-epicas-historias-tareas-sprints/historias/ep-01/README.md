# Historias de usuario — EP-01 · Plataforma, contratos e integración

> Fichas en el formato del [template oficial de Historia de Usuario de la Wiki de
> Taiga](../../../10-plantillas/historia-de-usuario-taiga.md). Estas seis historias (**H01–H06**,
> anteriormente H01–H04, H08, H09) son **habilitadores técnicos** de S1: fallan la **V** de INVEST (el `COMO`
> es «el equipo» o «la plataforma»), así que en Taiga se cargan como **tareas** bajo
> EP-01, sin puntos de valor. El formato largo acá es para trazar los escenarios de
> aceptación.
>
> **Fuente de verdad.** ID, épica, pareja, dependencias y horas mandan desde
> [`35` · «S1»](../../../04-backlog-ejecutable.md).
> Tipo (HU / habilitador) y demo, desde [30 · §5](../../../02-arranque-agil-y-sprint-0.md).
> DoR/DoD, desde [23 · §9.2](../../../03-plan-de-construccion-del-producto.md). Método, desde
> [29](../../../08-guia-de-historias-de-usuario.md). El desglose en **tareas SMART** de
> cada historia está en [`../../tareas/ep-01/`](../../tareas/ep-01/README.md).
>
> **Estimación en puntos.** Ninguna ficha trae puntos Fibonacci: se asignan en el
> **Sprint 0** con Planning Poker contra la historia canónica (`LLM-EP03-H02`, ex `LLM-S01-H06`). La columna
> *h* del plan es la referencia de planificación y **no se convierte** a puntos.

## Índice

| ID | Título | Tipo | Pareja | Dep. | h |
|---|---|---|---|---|--:|
| [LLM-EP01-H01](h01.md) | ADR de arquitectura y convenciones técnicas | Tarea (habilitador) | P1 | — | 16 |
| [LLM-EP01-H02](h02.md) | Entorno reproducible con un comando | Tarea (habilitador) | P1 | H01 | 30 |
| [LLM-EP01-H03](h03.md) | Esqueleto transversal del servicio | Tarea (habilitador) | P1 | H02 | 34 |
| [LLM-EP01-H04](h04.md) | Esquema inicial versionado con auditoría | Tarea (habilitador) | P1 | H03 | 38 |
| [LLM-EP01-H05](h05.md) | Contrato OpenAPI y mock del golden set publicados *(ex-H08)* | Tarea (habilitador) | P1 | H03 | 10 |
| [LLM-EP01-H06](h06.md) | Suite de pruebas y guía de demo de S1 *(ex-H09)* | Tarea (habilitador) | todos | H04, EP-03 | 18 |
| [LLM-EP01-H07](h07.md) *(propuesta)* | Esqueleto de mensajería Kafka con deduplicación de eventos | Tarea (habilitador) | P1 (sugerido) | H03, H04 | *(a fijar)* |

> **H07 es una propuesta, no backlog confirmado.** Cierra el CA de épica "publica/consume eventos
> sin duplicarlos" ([`epicas/ep-01.md`](../../epicas/ep-01.md)), que hoy ninguna ficha cubre. Entra
> a `35` recién cuando el equipo la refine y le asigne sprint y horas.
>
> Las HU de valor de S1 (**EP-03**) están en [`../ep-03/`](../ep-03/README.md). La vista
> completa del sprint (índice, demo) está en
> [`../../sprints/sprint-1/s1-historias.md`](../../sprints/sprint-1/s1-historias.md).
