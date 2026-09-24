# Sprints del `llm-service`

> **Qué es esta carpeta.** El registro de cada sprint del proyecto (arranque, capacidad,
> compromiso, cierre) **y** la vista de cada sprint sobre las historias (índice, tipo,
> demo). Empieza en el **Sprint 0** (sin incremento de software). El **registro** de S1 en
> adelante se crea copiando la [plantilla de sprint](../../10-plantillas/sprint-llm.md) al
> comenzar cada Planning.
>
> **Fuente de verdad.**
>
> | Pieza | Dónde vive |
> |---|---|
> | **Calendario vigente (máximo 5 sprints)** | [`38`](../../05-plan-de-cinco-sprints.md) — retira el horizonte de 19 sprints de abajo |
> | Checklist de Sprint 0 | [`35` · «Sprint 0»](../../04-backlog-ejecutable.md) y [30 · §1](../../02-arranque-agil-y-sprint-0.md) |
> | DoR / DoD | [23 · §9.2](../../03-plan-de-construccion-del-producto.md) |
> | Cálculo de capacidad | [`capacidad-sprints.md`](capacidad-sprints.md) · [23 · §2](../../03-plan-de-construccion-del-producto.md) / [30 · §3](../../02-arranque-agil-y-sprint-0.md) |
> | Recetas de construcción (histórico S1–S19; vigentes S1–S4, S9–S13, S15–S18 — renumeradas a Sprints 1–5 por [`38`](../../05-plan-de-cinco-sprints.md); S6, S14 y S19 quedan como antecedente) | [`35`](../../04-backlog-ejecutable.md) |
> | Épicas | [`../epicas/`](../epicas/README.md) |
> | Fichas de historias (por épica) | [`../historias/`](../historias/README.md) |
> | Tareas SMART por historia | [`../tareas/`](../tareas/README.md) |
>
> Si un número de acá no coincide con la fuente, **manda la fuente**.

## Índice

| Sprint | Carpeta | Estado | HU comprometidas | Objetivo |
|---|---|---|---|---|
| **0** | [`sprint-0/`](sprint-0/README.md) | Por ejecutar | Ninguna | Acuerdos, capacidad de S1 y ambiente. Sin incremento de software. |
| **1** | [`sprint-1/`](sprint-1/README.md) | ✅ Listo para Planning | 30 HU/Tareas (7 hilos en paralelo, ~660–681 h + 9 a estimar) | Golden set, proveedor real, tutor, calibración plataforma, cola de evaluación, **+ arranque de EP-07 y de la extensión de EP-09**. |
| **2** | [`sprint-2/`](sprint-2/README.md) | Por planificar | 12 HU/Tareas (~348–368 h + variable) | Cerrar EP-06 completa (apelación, override, bloqueo de cierre) **+ cerrar EP-07 y la extensión de EP-09** que S1 dejó a mitad. |
| **3** | [`sprint-3/`](sprint-3/README.md) | Por planificar | Regresión + cobertura (~40–65 h) | Hardening, cobertura ≥ 95%, colchón `courses-service`. Vuelve a ser colchón — EP-07 y la extensión de EP-09 se adelantaron a S1/S2. |
| **4** | [`sprint-4/`](sprint-4/README.md) | Por planificar | Integración E2E + **EP-08 completa** (~205–230 h) | Ambiente compartido real, corrección de errores de integración, moderación integrada de punta a punta. |
| **5** | [`sprint-5/`](sprint-5/README.md) | Por planificar | Demo + defensa + **EP-10 completa** + streaming (~201–221 h) | Demo grabada, deck de defensa, desafío personalizado y agente por mención. |

## Regla de nombrado

- `capacidad-sprints.md` — modelo de cálculo de capacidad del equipo y auditoría de la planilla.
- `sprint-0.md` — arranque (documento propio, **no** usa la plantilla de sprint).
- `s1.md`, `s2.md`, … — el **registro** de un sprint (Planning / ejecución / cierre), copia
  de [`plantillas/sprint-llm.md`](../../10-plantillas/sprint-llm.md). Nace en la Planning.
- `sN-historias.md` — la **vista de historias** del sprint N: índice, tipo de cada historia
  y demo, con enlaces a las fichas por épica en [`../historias/`](../historias/README.md).
- `sN-explicado.md` — las historias del sprint N contadas **sin jerga técnica** (opcional).

## Sprint de cierre de S1 + Entrega 1 (2026-09-12)

No es un sprint numerado de la receta de [`35`](../../04-backlog-ejecutable.md): agrupa los huecos
detectados al auditar S1 contra el código real más lo que falta para la Entrega 1 propuesta en
[`entregas/entrega-1.md`](../../../01-vision-alcance-y-entrega/03-entregas/entrega-1.md). Registro completo en
[`s1-cierre.md`](sprint-1/s1-cierre.md); checklist de origen en
[`entregas/checklist-cierre-s1.md`](../../../01-vision-alcance-y-entrega/03-entregas/checklist-cierre-s1.md); orden priorizado en dos
vistas (PO / equipo) en
[`entregas/backlog-priorizado-cierre-s1.md`](../../../01-vision-alcance-y-entrega/03-entregas/backlog-priorizado-cierre-s1.md); **una
página para llevar directo a la Planning** en
[`entregas/resumen-planning-cierre-s1.md`](../../../01-vision-alcance-y-entrega/03-entregas/resumen-planning-cierre-s1.md).

## Reprogramación a 8 semanas (alcance estricto del Tema 07)

> Nota de planificación, no una fuente de verdad nueva: registra por qué S1 ganó una
> historia que la receta original ([`35`](../../04-backlog-ejecutable.md)) asignaba a S3.

El plan de 19 sprints que llegó a plantear [23](../../03-plan-de-construccion-del-producto.md) pensaba
el **alcance amplio** (ser el servicio de LLM de toda la plataforma, con RAG, moderación y
personalización) — **horizonte retirado, ver [38](../../05-plan-de-cinco-sprints.md)**. Para el
**alcance estricto de la cátedra** — los 6 ítems: rúbrica, golden
set, invocación del modelo, calibración, bloqueo de activación y salvaguarda anti-fuga — ese
plan cabe en **4 sprints (8 semanas)** si se corren **dos pistas en paralelo** desde S1 en vez
de encadenar todo secuencialmente:

| Pista | Parejas | Qué cubre |
|---|---|---|
| **A — Golden set y calibración** | P4 + P5 (+ P1 en S1 y S4) | Rúbrica, golden set, referencia versionada, calibración de plataforma y por curso, bloqueo de activación |
| **B — Modelo y tutor** | P2 + P3 | Invocación del modelo, contexto del tutor, salvaguarda anti-fuga |

**S1 es el único sprint donde esto es visible como cambio de alcance:** además de EP-01/EP-03
(igual que la receta original), incorpora **LLM-S01-H10** (EP-02, Pista B) — el puerto del
proveedor de modelos y su fake — porque no depende de que el golden set esté publicado, sólo
del ADR de convenciones (H01). Así P2 arranca en paralelo desde la semana 1 en vez de esperar
a S3. El criterio de demo de S1 **no cambia**: sigue siendo el golden set.

**Condición para que la Pista B no se frene en S2:** cerrar en la primera semana las
decisiones que hoy bloquean el tutor y la calibración por curso — A-2, B-1, B-3, B-4, B-6 y
C-2 de [08 · decisiones y pendientes](../../../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md) — y confirmar con el
Product Owner la fecha del golden set por curso (C-1), que es una dependencia de calendario
docente y no de desarrollo.

## Sprint 0 en una línea

Scrum no define un «Sprint 0». Acá es una **iteración de preparación sin compromiso de
incremento**: se cierran nombres, capacidad, backlog inicial refinado y ambiente
reproducible. **No se comprometen historias ni se asignan puntos** (salvo estimar la
historia canónica). La primera Planning formal y sus 24 h-persona se cargan a **S1**, no a
Sprint 0 ([30 · §1.2](../../02-arranque-agil-y-sprint-0.md)).
