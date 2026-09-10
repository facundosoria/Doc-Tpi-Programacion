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
> | Cálculo de capacidad | [23 · §2](../23-plan-construccion-producto-llm.md) / [30 · §3](../30-arranque-agil-y-sprint-0.md) |
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

- `sprint-0.md` — arranque (documento propio, **no** usa la plantilla de sprint).
- `s1.md`, `s2.md`, … — el **registro** de un sprint (Planning / ejecución / cierre), copia
  de [`plantillas/sprint-llm.md`](../plantillas/sprint-llm.md). Nace en la Planning.
- `sN-historias.md` — la **vista de historias** del sprint N: índice, tipo de cada historia
  y demo, con enlaces a las fichas por épica en [`../historias/`](../historias/README.md).
- `sN-explicado.md` — las historias del sprint N contadas **sin jerga técnica** (opcional).

## Sprint 0 en una línea

Scrum no define un «Sprint 0». Acá es una **iteración de preparación sin compromiso de
incremento**: se cierran nombres, capacidad, backlog inicial refinado y ambiente
reproducible. **No se comprometen historias ni se asignan puntos** (salvo estimar la
historia canónica). La primera Planning formal y sus 24 h-persona se cargan a **S1**, no a
Sprint 0 ([30 · §1.2](../30-arranque-agil-y-sprint-0.md)).
