# Plantilla — Tarea (Wiki / backlog de Taiga)

> Template oficial de Tarea de la Wiki de Taiga «Plataforma de Aprendizaje
> Gamificado de Programación». Uso **obligatorio** para uniformidad
> (ver [27 · Guía de la Wiki §7](../27-guia-wiki-taiga.md)).
>
> Copiar este contenido al crear una tarea nueva en el backlog. En Taiga la Tarea
> **cuelga de su Historia de Usuario** (o de la tarea de sprint, si el padre es un
> habilitador): no vive suelta. Reemplazar `GXX` por el número de grupo asignado por
> la cátedra y borrar los textos de ejemplo antes de publicar.
>
> Esta plantilla es el **formato de presentación**. El ID interno del equipo sigue el
> esquema `LLM-Sxx-Hyy-Tzz`. Las horas de la tarea son **orientativas** y suman la
> referencia de planificación de la historia padre; la fuente de esa referencia es
> [`35`](../35-backlog-ejecutable.md).
>
> Qué hace a una tarea correcta —**SMART**: Específica, Medible, Alcanzable, Relevante,
> Acotada en el tiempo; partida en **una jornada efectiva o menos**; sin
> `Como / Quiero / Para`, sin puntos Fibonacci—: [`tareas/README.md`](../tareas/README.md).
> La regla de la jornada y la distinción épica/historia/tarea:
> [29 · §4](../29-guia-catedra-historias-de-usuario.md) y
> [23 · §9.2](../23-plan-construccion-producto-llm.md).

---

# [GXX] — [Sxx-Hyy] · T## [TÍTULO DE LA TAREA]

> Historia padre: `[GXX] — [TÍTULO DE LA HISTORIA]` (enlace permanente del backlog).

## Objetivo (SMART)

- **Específica:** [un solo paso técnico: verbo + resultado concreto]
- **Medible:** [el criterio de terminado de abajo responde sí/no, sin discusión]
- **Alcanzable:** [cabe en ≤ 1 jornada efectiva, para una persona o pareja]
- **Relevante:** [a qué criterio de aceptación o escenario BDD de la historia sirve]
- **Acotada en el tiempo:** [estimación en horas; si supera la jornada, se parte en dos]

## Pasos / alcance

- [sub-paso 1 — sin formato Como/Quiero/Para, sin nombres de tablas ni clases]
- [sub-paso 2]
- [qué queda **fuera** de esta tarea, si hay riesgo de confusión]

## Criterio de terminado (Done)

- [condición observable que se responde sí/no: prueba en verde, endpoint responde,
  migración corre, revisión aprueba…]

## Estimación y dependencias

- **Horas:** [N]  ·  **Paso del orden de construcción:** [contrato y amenaza / dominio y
  migración / caso de uso / adaptadores / seguridad y resiliencia / observabilidad /
  prueba E2E / demo y evidencia]
- **Depende de:** [T## de la misma historia / otra historia / — ]
- **Traza:** [CA## y/o Escenario BDD de `Sxx-Hyy`]
