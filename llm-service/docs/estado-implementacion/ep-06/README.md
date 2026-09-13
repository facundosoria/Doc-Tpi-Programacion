# EP-06 · Evaluación, score y auditoría académica — estado

> No existe `docs/historias/ep-06/` todavía — no hay ninguna HU escrita para esta épica. Esta
> auditoría (2026-09-12) parte directamente del objetivo de
> [`epicas/ep-06.md`](../../epicas/ep-06.md) y de las recetas de
> [`35-backlog-ejecutable.md`](../../35-backlog-ejecutable.md) (S6, S7), no de una ficha.

## Índice

| Ítem | Estado | Nota en una línea |
|---|---|---|
| [evaluacion-y-apelacion](evaluacion-y-apelacion.md) | 🔴 No iniciado | Cero código propio; lo que parecía EP-06 resultó ser EP-04 (compuerta de calibración), ya migrado a [`ep-04/pending-evaluations-gate.md`](../ep-04/pending-evaluations-gate.md) |

## Resumen

Búsqueda exhaustiva (`grep` de score/apelación/override/appeal + lectura de los 6 archivos con
nombres relacionados a "evaluation") sobre todo `llm-service/src/main/java`: **no hay ningún
consumidor de eventos, evaluador de intento real, tabla de evaluación, publicador de resultados,
ni mecanismo de apelación u override.** El detalle CA-por-CA está en
[`evaluacion-y-apelacion.md`](evaluacion-y-apelacion.md), incluida una **contradicción de alcance
entre `sprints/README.md` y `00-fuentes-de-verdad-y-convenciones.md`** sobre si esto es parte del
corte actual del proyecto o no — es la decisión más importante para poder responder "¿cuántos
sprints faltan?" con un número real.
