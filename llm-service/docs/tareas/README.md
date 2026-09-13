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
| **EP-02** · AI Gateway, modelos y resiliencia | [`ep-02/`](ep-02/README.md) | LLM-S01-H10 · [catálogo de adaptadores real (sin HU)](ep-02/model-catalog-real.md) | H10 cerrada; catálogo real (lectura + alta) pendiente (Entrega 1 + ampliación) |
| **EP-03** · Golden set y referencia humana | [`ep-03/`](ep-03/README.md) | LLM-S01-H05 · H06 · H07 (histórico, [reescritura en curso](ep-03/rewrite-h05-h06-h07-v2.md)) · LLM-S02-H01 · H02 (vigentes) | S1 histórico; S2 ya construida |
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
| LLM-S03-H01 | [`ep-04/s03-h01.md`](ep-04/s03-h01.md) | 7 | 6 hechas, T7 **firmada en 14 h** — bloqueante de Entrega 1 |

## Horas — deuda de cierre de S1 y Entrega 1 (2026-09-12)

Tareas nuevas o reestimadas contra [`checklist-cierre-s1.md`](../entregas/checklist-cierre-s1.md)
y [`entrega-1.md`](../entregas/entrega-1.md). El Sprint Backlog que las agrupa y les pone
responsable/orden vive en [`sprints/s1-cierre.md`](../sprints/s1-cierre.md); la priorización en
dos vistas (valor de demo vs. dependencia técnica), en
[`entregas/backlog-priorizado-cierre-s1.md`](../entregas/backlog-priorizado-cierre-s1.md).

| Origen | Archivo | Qué agrega | h nuevas/firmadas |
|---|---|---|---:|
| Entrega 1 (bloqueante) | [`ep-04/s03-h01.md`](ep-04/s03-h01.md) T7 | Conectar calibración con el puerto de H10 — **ya no depende del catálogo** (resuelto leyendo código, 2026-09-12: usa `function_model_config`, no `model_deployments`) | 14 |
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
[`backlog-priorizado-cierre-s1.md`](../entregas/backlog-priorizado-cierre-s1.md)): formalizar T3
(contrato WireMock) y T5 (evidencia BDD) de [`ep-01/h09.md`](ep-01/h09.md), y unificar
"cohorte"/"curso" en el resto de la documentación ([checklist §3.2](../entregas/checklist-cierre-s1.md)).

## Cómo se generan

Con el skill [`generar-tareas`](../../.agents/skills/generar-tareas/SKILL.md): toma una
HU con sus CA y su BDD y produce sus tareas SMART en el orden de construcción del equipo.
