# Plantilla — Tarea (Wiki / backlog de Taiga)

> Template oficial de Tarea. Uso **obligatorio** para uniformidad del backlog.
>
> Copiar desde `# [GXX] — [Sxx-Hyy] · T## …` al crear la tarea en Taiga. En Taiga la
> Tarea **cuelga de su Historia de Usuario** (o de la tarea de sprint, si el padre es un
> habilitador): no vive suelta. Reemplazar `GXX` por el número de grupo/equipo y `Sxx-Hyy`
> por el ID de la historia padre; borrar los textos de ejemplo antes de publicar.
>
> **Qué NO lleva una tarea:**
> - No usa `Como / Quiero / Para` ni escenarios BDD — eso es de las Historias de Usuario.
> - No se estima en puntos Fibonacci ni lleva prioridad MoSCoW ni checklist INVEST.
> - No entrega valor perceptible por sí sola: es trabajo técnico interno del equipo.
>
> El ID interno del equipo sigue el esquema `Sxx-Hyy-Tzz`. Las horas de la tarea son
> **orientativas** y suman la referencia de planificación de la historia padre. Cómo se
> parte una historia en tareas SMART y cada cuánto: `guia-metodo-smart.md`.

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
