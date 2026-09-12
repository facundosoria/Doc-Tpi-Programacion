# Sprints del `llm-service`

> **Qué es esta carpeta.** El registro de cada sprint del proyecto (arranque, capacidad,
> compromiso, cierre) **y** la vista de cada sprint sobre las historias (índice, tipo,
> demo). Empieza en el **Sprint 0** (sin incremento de software). El **registro** de S1 en
> adelante se crea copiando la [plantilla de sprint](../plantillas/sprint-llm.md) al
> comenzar cada Planning.
>
> **Fuente de verdad.**
>
> | Pieza | Dónde vive |
> |---|---|
> | Checklist de Sprint 0 | [`35` · «Sprint 0»](../35-backlog-ejecutable.md) y [30 · §1](../30-arranque-agil-y-sprint-0.md) |
> | DoR / DoD | [23 · §9.2](../23-plan-construccion-producto-llm.md) |
> | Cálculo de capacidad | [`capacidad-sprints.md`](capacidad-sprints.md) · [23 · §2](../23-plan-construccion-producto-llm.md) / [30 · §3](../30-arranque-agil-y-sprint-0.md) |
> | Recetas de construcción S1–S19 | [`35`](../35-backlog-ejecutable.md) |
> | Épicas | [`../epicas/`](../epicas/README.md) |
> | Fichas de historias (por épica) | [`../historias/`](../historias/README.md) |
> | Tareas SMART por historia | [`../tareas/`](../tareas/README.md) |
>
> Si un número de acá no coincide con la fuente, **manda la fuente**.

## Índice

| Sprint | Registro | Vista de historias | Estado | Incremento / objetivo |
|---|---|---|---|---|
| **0** | [`sprint-0.md`](sprint-0.md) | — | Por ejecutar | Arranque: acuerdos, capacidad de S1 y ambiente. **Sin incremento de software.** |
| **1** | *(crear desde [plantilla](../plantillas/sprint-llm.md) en la Planning de S1)* | [`s1-historias.md`](s1-historias.md) · [`s1-explicado.md`](s1-explicado.md) | — | Un docente autorizado carga y consulta un golden set que sobrevive al reinicio. |
| 2–19 | *(uno por sprint, al planificarlo)* | *(uno por sprint)* | — | Ver recetas en [`35`](../35-backlog-ejecutable.md). |

## Regla de nombrado

- `capacidad-sprints.md` — modelo de cálculo de capacidad del equipo y auditoría de la planilla.
- `sprint-0.md` — arranque (documento propio, **no** usa la plantilla de sprint).
- `s1.md`, `s2.md`, … — el **registro** de un sprint (Planning / ejecución / cierre), copia
  de [`plantillas/sprint-llm.md`](../plantillas/sprint-llm.md). Nace en la Planning.
- `sN-historias.md` — la **vista de historias** del sprint N: índice, tipo de cada historia
  y demo, con enlaces a las fichas por épica en [`../historias/`](../historias/README.md).
- `sN-explicado.md` — las historias del sprint N contadas **sin jerga técnica** (opcional).

## Reprogramación a 8 semanas (alcance estricto del Tema 07)

> Nota de planificación, no una fuente de verdad nueva: registra por qué S1 ganó una
> historia que la receta original ([`35`](../35-backlog-ejecutable.md)) asignaba a S3.

El plan de 19 sprints ([23](../23-plan-construccion-producto-llm.md)) está pensado para el
**alcance amplio** (ser el servicio de LLM de toda la plataforma, con RAG, moderación y
personalización). Para el **alcance estricto de la cátedra** — los 6 ítems: rúbrica, golden
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
C-2 de [08 · decisiones y pendientes](../08-decisiones-y-pendientes.md) — y confirmar con el
Product Owner la fecha del golden set por curso (C-1), que es una dependencia de calendario
docente y no de desarrollo.

## Sprint 0 en una línea

Scrum no define un «Sprint 0». Acá es una **iteración de preparación sin compromiso de
incremento**: se cierran nombres, capacidad, backlog inicial refinado y ambiente
reproducible. **No se comprometen historias ni se asignan puntos** (salvo estimar la
historia canónica). La primera Planning formal y sus 24 h-persona se cargan a **S1**, no a
Sprint 0 ([30 · §1.2](../30-arranque-agil-y-sprint-0.md)).
