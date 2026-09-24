# Tareas del `llm-service` — fichas SMART por historia

> **Qué es esta carpeta.** El desglose en **tareas** de cada Historia de Usuario, en el mismo
> patrón que [`../historias/`](../historias/README.md): **una carpeta por épica, un archivo
> por historia** (`ep-0X/hYY.md` o `ep-0X/sZZ-hYY.md`), con el formato del [template oficial
> de Tarea de la Wiki de Taiga](../../10-plantillas/tarea-taiga.md). En Taiga cada tarea **cuelga de
> su HU** (o de la tarea de sprint, si el padre es un habilitador).
>
> **Método: SMART.** Una tarea está bien escrita si es **S**pecífica (un solo paso
> técnico, verbo + resultado), **M**edible (su criterio de terminado se responde sí/no),
> **A**lcanzable (cabe en ≤ 1 jornada efectiva, una persona o pareja), **R**elevante
> (traza a un CA o escenario BDD de la historia padre) y **A**cotada en el tiempo
> (estimación en horas; si supera la jornada, se parte). Las tareas **no** llevan
> `Como / Quiero / Para` ni puntos Fibonacci: eso es de las historias
> ([29 · §4](../../08-guia-de-historias-de-usuario.md), [23 · §9.2](../../03-plan-de-construccion-del-producto.md)).
>
> **Fuente de verdad.**
>
> | Dato | Dónde vive |
> |---|---|
> | Horas por historia y por sprint (S1) | [`35`](../../04-backlog-ejecutable.md) · «S1» |
> | Criterios de aceptación y escenarios BDD (lo que cada tarea traza) | [`../historias/`](../historias/README.md) |
> | Orden de construcción de las tareas | [playbook · §4](../../06-playbook-de-construccion.md) |
> | Vista del sprint (índice, demo) | [`../sprints/sprint-1/s1-historias.md`](../sprints/sprint-1/s1-historias.md) |
> | Estado real del código (S2/S3, escritas a posteriori) | [`../entregas/verificacion-v2-golden-set-calibracion.md`](../../../01-vision-alcance-y-entrega/03-entregas/verificacion-v2-golden-set-calibracion.md) |
>
> Las horas por tarea son **orientativas** y suman la referencia de la historia. Si un
> número de acá no coincide con la fuente, **manda la fuente**. En las historias escritas a
> posteriori (S2, S3), la mayoría de las tareas ya están **hechas** — están marcadas
> explícitamente; no son planificación a futuro.

## Índice por épica

| Épica | Carpeta | Historias | Estado |
|---|---|---|---|
| **EP-01** · Plataforma, contratos e integración | [`ep-01/`](ep-01/README.md) | LLM-EP01-H01 a H06 *(H01–H04, H08, H09 originales)* | Planificadas (S1) |
| **EP-02** · AI Gateway, modelos y resiliencia | [`ep-02/`](ep-02/README.md) | LLM-EP02-H01 *(ex-H10)* · [catálogo de adaptadores real (sin HU)](ep-02/model-catalog-real.md) | H01 cerrada; catálogo real (lectura + alta) pendiente |
| **EP-03** · Golden set y referencia humana | [`ep-03/`](ep-03/README.md) | LLM-EP03-H01–H03 (histórico S1) · LLM-EP03-H04–H05 (vigentes S2) | S1 histórico; S2 ya construida |
| **EP-04** · Calibración y gobernanza del modelo | [`ep-04/`](ep-04/README.md) | LLM-EP04-H01 *(ex-S03-H01)* | Esqueleto construido, T7 cerrada |
| **EP-05** · Tutor seguro y guardarraíles | [`ep-05/`](ep-05/README.md) | LLM-EP05-H01 a H03 | 25 tareas · 77 h |
| **EP-06** · Evaluación y puntuación de entregas | [`ep-06/`](ep-06/README.md) | LLM-EP06-H01 a H06 | Tareas desglosadas |
| **EP-07** · Operación, cuotas y observabilidad | [`ep-07/`](ep-07/README.md) | LLM-S09-H01 a H03, LLM-S10-H01 a H03 | Tareas desglosadas |
| **EP-08** · Moderación integrada (F2) | [`ep-08/`](ep-08/README.md) | LLM-S11-H01/H02, LLM-S12-H01/H02, LLM-S13-H01/H02 | 31 tareas · 160 h (planificadas) |
| **EP-09** · RAG y consulta de material | [`ep-09/`](ep-09/README.md) | LLM-EP09-H01 a H02 | Tareas desglosadas |
| **EP-10** · Personalización y agente (F3) | [`ep-10/`](ep-10/README.md) | LLM-S17-H01/H02, LLM-S18-H01/H02 | 23 tareas · 116 h (planificadas S5) |

## Horas — S1 (planificación original, vigente)

**Total S1:** 240 h de tareas (piso de planificación, no tope).

| Historia | Archivo | Tareas | h |
|---|---|---:|---:|
| LLM-EP01-H01 *(LLM-S01-H01)* | [`ep-01/h01.md`](ep-01/h01.md) | 5 | 16 |
| LLM-EP01-H02 *(LLM-S01-H02)* | [`ep-01/h02.md`](ep-01/h02.md) | 7 | 30 |
| LLM-EP01-H03 *(LLM-S01-H03)* | [`ep-01/h03.md`](ep-01/h03.md) | 8 | 34 |
| LLM-EP01-H04 *(LLM-S01-H04)* | [`ep-01/h04.md`](ep-01/h04.md) | 7 | 38 |
| LLM-EP03-H01 *(histórico, ex-H05)* | [`ep-03/h01.md`](ep-03/h01.md) | 6 | 24 |
| LLM-EP03-H02 *(canónica, ex-H06)* | [`ep-03/h02.md`](ep-03/h02.md) | 4 | 14 |
| LLM-EP03-H03 *(histórico, ex-H07)* | [`ep-03/h03.md`](ep-03/h03.md) | 6 | 24 |
| LLM-EP01-H05 *(ex-H08)* | [`ep-01/h05.md`](ep-01/h05.md) | 4 | 10 |
| LLM-EP01-H06 *(ex-H09)* | [`ep-01/h06.md`](ep-01/h06.md) | 6 | 18 |
| LLM-EP02-H01 *(ex-H10)* | [`ep-02/h01.md`](ep-02/h01.md) | 6 | 32 |
| **Total** | | **59** | **240** |

## Horas — S2/S3 (escritas a posteriori, código ya construido)

No suman al total de S1: son la documentación retroactiva de `605f381`
([decision-605f381.md](../../../01-vision-alcance-y-entrega/03-entregas/decision-605f381.md)). Las horas son de referencia, no un
compromiso de sprint.

| Historia | Archivo | Tareas | Estado |
|---|---|---:|---|
| LLM-EP03-H04 *(LLM-S02-H01)* | [`ep-03/h04.md`](ep-03/h04.md) | 6 | 5 hechas, 1 pendiente de decisión |
| LLM-EP03-H05 *(LLM-S02-H02)* | [`ep-03/h05.md`](ep-03/h05.md) | 7 | 6 hechas, 1 pendiente de código |
| LLM-EP04-H01 *(LLM-S03-H01)* | [`ep-04/h01.md`](ep-04/h01.md) | 7 | 6 hechas, T7 **firmada en 14 h** — bloqueante de Entrega 1 |

## Horas — deuda de cierre de S1 y Entrega 1 (2026-09-12)

Tareas nuevas o reestimadas contra [`checklist-cierre-s1.md`](../../../01-vision-alcance-y-entrega/03-entregas/checklist-cierre-s1.md)
y [`entrega-1.md`](../../../01-vision-alcance-y-entrega/03-entregas/entrega-1.md). El Sprint Backlog que las agrupa y les pone
responsable/orden vive en [`sprints/s1-cierre.md`](../sprints/sprint-1/s1-cierre.md); la priorización en
dos vistas (valor de demo vs. dependencia técnica), en
[`entregas/backlog-priorizado-cierre-s1.md`](../../../01-vision-alcance-y-entrega/03-entregas/backlog-priorizado-cierre-s1.md).

| Origen | Archivo | Qué agrega | h nuevas/firmadas |
|---|---|---|---:|
| Entrega 1 (bloqueante) | [`ep-04/h01.md`](ep-04/h01.md) T7 | Conectar calibración con el puerto de H10 — **ya no depende del catálogo** (resuelto leyendo código, 2026-09-12: usa `function_model_config`, no `model_deployments`) | 14 |
| Entrega 1 | [`ep-02/model-catalog-real.md`](ep-02/model-catalog-real.md) T1+T3 | Catálogo de adaptadores real, lectura (nueva, sin ficha de HU) — T2 se descartó el 2026-09-12 (no aplica, ver el archivo) | 8 |
| Checklist §1.4 | [`ep-01/h03.md`](ep-01/h03.md) T9 | `404` real en vez de `409`/`403` para "no existe" | 3 |
| **Subtotal bloqueante/deuda** | | | **25** |
| Ampliación aceptada (2026-09-12) | [`ep-02/model-catalog-real.md`](ep-02/model-catalog-real.md) T4 | Alta real de adaptador (`POST /admin/model-adapters`) — repara RF-IA-11, no solo documentación | 12 |
| Ampliación aceptada (2026-09-12) | [`ep-03/rewrite-h05-h06-h07-v2.md`](ep-03/rewrite-h05-h06-h07-v2.md) | Reescribir H05/H06/H07 + verificación nueva contra el código v2 | 14 |
| **Total con ampliación** | | | **51** |

Los huecos de H01, H02, H08 y H09 del checklist **no generaron tareas nuevas** — ya estaban
cubiertos por tareas de la planificación original de S1 que simplemente no se ejecutaron
todavía (ver la tabla de S1 arriba). El Sprint Backlog de cierre las retoma tal cual están
escritas.

**Quedaron explícitamente para el siguiente ciclo** (no entraron en esta ampliación, ver
[`backlog-priorizado-cierre-s1.md`](../../../01-vision-alcance-y-entrega/03-entregas/backlog-priorizado-cierre-s1.md)): formalizar T3
(contrato WireMock) y T5 (evidencia BDD) de [`ep-01/h06.md`](ep-01/h06.md) (ex-H09), y unificar
"cohorte"/"curso" en el resto de la documentación ([checklist §3.2](../../../01-vision-alcance-y-entrega/03-entregas/checklist-cierre-s1.md)).

## Cómo se generan

Con el skill [`generar-tareas`](../../../../../.agents/skills/generar-tareas/SKILL.md): toma una
HU con sus CA y su BDD y produce sus tareas SMART en el orden de construcción del equipo.
