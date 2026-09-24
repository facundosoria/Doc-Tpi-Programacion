# Historias de usuario del `llm-service` — fichas por épica

> **Qué es esta carpeta.** Las Historias de Usuario del producto, **agrupadas por
> épica** (no por sprint), cada archivo con las fichas en el formato del [template
> oficial de Historia de Usuario de la Wiki de Taiga](../../10-plantillas/historia-de-usuario-taiga.md):
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
> | ID, épica, pareja, dependencias, horas | [`35`](../../04-backlog-ejecutable.md) |
> | Catálogo de épicas (pareja líder, fase, sprints) | [`../epicas/README.md`](../epicas/README.md) |
> | Tipo (HU de valor / habilitador) y demo del sprint | [30 · §5](../../02-arranque-agil-y-sprint-0.md) |
> | DoR / DoD | [23 · §9.2](../../03-plan-de-construccion-del-producto.md) |
> | Método para redactar y estimar | [29 · Guía de cátedra: Historias de Usuario](../../08-guia-de-historias-de-usuario.md) |
> | Contratos HTTP vigentes | [`../contracts/`](../../../contracts) |
> | Desglose en tareas | [`../tareas/`](../tareas/README.md) |
> | **Estado de implementación real** (qué de esto ya está construido, contra qué código) | [`../estado-implementacion/`](../../../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/README.md) |
>
> **Título en Taiga.** Cada ficha se carga con el título `GXX — TÍTULO` (`GXX` =
> número de grupo, aún sin asignar). El ID interno sigue el formato por épica `LLM-EPxx-Hyy`
> (con trazabilidad al sprint de ejecución y código histórico `LLM-Sxx-Hyy`).

## Índice por épica

| Épica | Archivo | Historias | Tipo en Taiga |
|---|---|---|---|
| **EP-01** · Plataforma, contratos e integración | [`ep-01/`](ep-01/README.md) | LLM-EP01-H01 · H02 · H03 · H04 · H05 · H06 | Tareas de sprint (habilitadores) |
| **EP-02** · AI Gateway, modelos y resiliencia | [`ep-02/`](ep-02/README.md) | LLM-EP02-H01 · H02 | Tareas de sprint (habilitadores) |
| **EP-03** · Golden set y referencia humana | [`ep-03/`](ep-03/README.md) | LLM-EP03-H01 · H02 · H03 (histórico S1) · LLM-EP03-H04 · H05 (vigentes S2) | **HU** de valor (rol: docente) |
| **EP-04** · Calibración y gobernanza del modelo | [`ep-04/`](ep-04/README.md) | LLM-EP04-H01 · H02 · H03 · H04 | **HU** de valor (rol: docente/ADMIN) — H01 🟢 completa; H02–H04 ⚪ borradores |
| **EP-05** · Tutor seguro y guardarraíles | [`ep-05/`](ep-05/README.md) | LLM-EP05-H01 · H02 · H03 | **HU** de valor (rol: alumno) — H01/H02 🟢 construidas; H03 ⚪ borrador |
| **EP-06** · Evaluación, score y auditoría académica | [`ep-06/`](ep-06/README.md) | LLM-EP06-H01 · H02 · H03 · H04 · H05 · H06 | H01/H06 tareas (habilitadores); H02–H05 **HU** de valor |
| **EP-09** · RAG y consulta de material (F3) | [`ep-09/`](ep-09/README.md) | LLM-EP09-H01 · H02 | **HU** de valor (roles: docente, alumno) — adelantadas |

> **EP-03 H01–H03** se cargan en Taiga como **HU** (rol real, puntos Fibonacci contra la
> canónica `LLM-EP03-H02`, INVEST verificado). **EP-01 H01–H06** y **EP-02 H01** como **tareas**
> bajo su épica: fallan la **V** de INVEST (el `COMO` es «el equipo» o «la plataforma»).
> El formato largo de HU acá es para trazar los escenarios de aceptación, no para
> forzar su carga como HU ([30 · §5](../../02-arranque-agil-y-sprint-0.md)).
>
> **EP-02 H01 (ex-H10) es una incorporación de la reprogramación a 8 semanas** (ver
> [`../sprints/README.md`](../sprints/README.md)): adelanta desde S3 el primer paquete de
> EP-02 porque no depende del golden set publicado, solo del ADR (EP-01 H01).

## Historias por sprint

| Sprint | Épicas activas | Historias | Vista de sprint |
|---|---|---|---|
| **S1** | EP-01 (H01–H06) · EP-02 (H01) · EP-03 (H01–H03, histórico) | 10 | [`../sprints/sprint-1/s1-historias.md`](../sprints/sprint-1/s1-historias.md) · [`../sprints/sprint-1/s1-explicado.md`](../sprints/sprint-1/s1-explicado.md) |
| **S2** | EP-03 (LLM-EP03-H04, H05) · EP-02 (LLM-EP02-H02) | 3 — **golden set versionado + conexión de modelo real** ([verificación](../../../01-vision-alcance-y-entrega/03-entregas/verificacion-v2-golden-set-calibracion.md)) | — |
| **S3** | EP-04 (LLM-EP04-H01) | 1 — calibración de curso (motor completado con T7) | — |
| S4–S19 | ver [catálogo](../epicas/README.md) y [plan de 5 sprints](../../05-plan-de-cinco-sprints.md) | *(sprint a sprint)* | — |

> **S1–S3 dejaron de ser "a construir" y pasaron a "auditar contra lo ya construido".** El
> commit `605f381` adelantó de facto ese trabajo — ver [decision-605f381.md](../../../01-vision-alcance-y-entrega/03-entregas/decision-605f381.md)
> y la propuesta de [Entrega 1](../../../01-vision-alcance-y-entrega/03-entregas/entrega-1.md).

## Cómo se generan

Con el skill [`generar-historias-usuario`](../../../../../.agents/skills/generar-historias-usuario/SKILL.md).
Las tareas de cada historia, con [`generar-tareas`](../../../../../.agents/skills/generar-tareas/SKILL.md).
