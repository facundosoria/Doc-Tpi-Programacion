# Front End — Angular — contratos

> Fuente completa: [18 §4.7](../../18-contratos-inter-equipos.md#47-front-end--angular),
> [15-sincronizacion-arquitectura-y-despliegue.md](../../15-sincronizacion-arquitectura-y-despliegue.md).

## Relación

No hay un endpoint que Front End consuma directo de nosotros — es una relación de UI: ellos
construyen las pantallas que exponen lo que nuestros endpoints/eventos devuelven. Todo lo que
falta de este lado está en [`pendientes.md`](pendientes.md).

## Estados que las pantallas tienen que poder mostrar

No es un endpoint nuevo — son los valores de enum que ya salen de los contratos de otros
equipos y que las 7 pantallas (ver [`pendientes.md`](pendientes.md)) tienen que renderizar sin
inventar un estado nuevo:

| Pantalla | Enum a mostrar | Fuente |
|---|---|---|
| 1 — Chat del tutor | `completed \| blocked \| unavailable` | [`tema-05-desafios-practicos/contratos.md`](../tema-05-desafios-practicos/contratos.md) |
| 2 — Estado del evaluador | `queued \| running \| completed \| failed` (job) + `score_pendiente_diferido` | [`tema-03-motor-de-desafios/contratos.md`](../tema-03-motor-de-desafios/contratos.md) |
| 5 — Panel de moderación | `severidad: alta \| media \| baja` + `degradacion: prefiltro_solamente \| diferido` | [`tema-11-chat/contratos.md`](../tema-11-chat/contratos.md) |

El caso más flojo hoy es el 1: no hay copy ni pantalla acordada para `unavailable` — ver
[`tema-05-desafios-practicos/pendientes.md`](../tema-05-desafios-practicos/pendientes.md).

## Nota aparte, no es contrato

`15-sincronizacion-arquitectura-y-despliegue.md` sincroniza con la unidad curricular de Front
End (Programación IV) — nginx como gateway, rolling update, etc. **No define contratos ni
requisitos nuestros**, es contenido pedagógico compartido que además nutre el glosario de
infraestructura (`11-glosario-y-metadata.md`).
