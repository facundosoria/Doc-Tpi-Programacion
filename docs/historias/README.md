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
| **EP-01** · Plataforma, contratos e integración | [`ep-01/`](ep-01/README.md) | LLM-S01-H01 · H02 · H03 · H04 · H08 · H09 | Tareas de sprint (habilitadores) |
| **EP-02** · AI Gateway, modelos y resiliencia | [`ep-02/`](ep-02/README.md) | LLM-S01-H10 | Tarea de sprint (habilitador) |
| **EP-03** · Golden set y referencia humana | [`ep-03/`](ep-03/README.md) | LLM-S01-H05 · H06 · H07 (histórico) · LLM-S02-H01 · H02 (vigentes) | **HU** de valor (rol: docente) |
| **EP-04** · Calibración y gobernanza del modelo | [`ep-04/`](ep-04/README.md) | LLM-S03-H01 | **HU** de valor (rol: docente) — 🟡 con hueco de implementación |

> **H05–H07** se cargan en Taiga como **HU** (rol real, puntos Fibonacci contra la
> canónica `LLM-S01-H06`, INVEST verificado). **H01–H04, H08, H09, H10** como **tareas**
> bajo su épica: fallan la **V** de INVEST (el `COMO` es «el equipo» o «la plataforma»).
> El formato largo de HU acá es para trazar los escenarios de aceptación, no para
> forzar su carga como HU ([30 · §5](../30-arranque-agil-y-sprint-0.md)).
>
> **H10 es una incorporación de la reprogramación a 8 semanas** (ver
> [`../sprints/README.md`](../sprints/README.md)): adelanta desde S3 el primer paquete de
> EP-02 porque no depende del golden set publicado, solo del ADR (H01).

## Historias por sprint

| Sprint | Épicas activas | Historias | Vista de sprint |
|---|---|---|---|
| **S1** | EP-01 (H01–H04, H08, H09) · EP-02 (H10) · EP-03 (H05–H07, histórico) | 10 | [`../sprints/s1-historias.md`](../sprints/s1-historias.md) · [`../sprints/s1-explicado.md`](../sprints/s1-explicado.md) |
| **S2** | EP-03 (LLM-S02-H01, H02) | 2 — **ya construidas, escritas a posteriori** ([verificación](../entregas/verificacion-v2-golden-set-calibracion.md)) | — |
| **S3** | EP-04 (LLM-S03-H01) | 1 — esqueleto construido, hueco de implementación (falta conectar H10) | — |
| S4–S19 | ver [catálogo](../epicas/README.md) | *(sprint a sprint; EP-02 conserva S8–S9, S3 pierde su primer paquete)* | — |

> **S1–S3 dejaron de ser "a construir" y pasaron a "auditar contra lo ya construido".** El
> commit `605f381` adelantó de facto ese trabajo — ver [decision-605f381.md](../entregas/decision-605f381.md)
> y la propuesta de [Entrega 1](../entregas/entrega-1.md).

## Cómo se generan

Con el skill [`generar-historias-usuario`](../../.agents/skills/generar-historias-usuario/SKILL.md).
Las tareas de cada historia, con [`generar-tareas`](../../.agents/skills/generar-tareas/SKILL.md).
