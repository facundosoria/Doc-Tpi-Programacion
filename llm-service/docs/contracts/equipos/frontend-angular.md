# Front End — Angular — contratos

> Este documento es la vista de integración para Front End. Los estados de UX y el contrato del
> componente tienen una única fuente normativa en [Operación e ingeniería §6–§7](../../06-operacion-calidad-y-pruebas/01-operacion-e-ingenieria.md#6-qué-ve-el-usuario-cuando-algo-falla);
> aquí se conserva únicamente la relación con las pantallas y sus consumidores.

## Relación

No hay un endpoint que Front End consuma directo de nosotros — es una relación de UI: ellos
construyen las pantallas que exponen lo que nuestros endpoints/eventos devuelven. Todo lo que
falta de este lado está en [`pendientes.md`](../../07-planificacion-y-trabajo-equipo/11-equipos/frontend-angular/pendientes.md).

## Estados que las pantallas tienen que poder mostrar

No es un endpoint nuevo — son los valores de enum que ya salen de los contratos de otros
equipos y que las pantallas (ver [`pendientes.md`](../../07-planificacion-y-trabajo-equipo/11-equipos/frontend-angular/pendientes.md)) tienen que renderizar sin
inventar un estado nuevo:

| Pantalla | Enum a mostrar | Fuente |
|---|---|---|
| 1 — Chat del tutor | `completed \| blocked \| unavailable` | [`tema-05-desafios-practicos.md`](tema-05-desafios-practicos.md) |
| 2 — Estado del evaluador | `queued \| running \| completed \| failed` (job) + `score_pendiente_diferido` | [`tema-03-motor-de-desafios.md`](tema-03-motor-de-desafios.md) |
| 5 — Panel de moderación | `severidad: alta \| media \| baja` + `degradacion: prefiltro_solamente \| diferido` | [`tema-11-chat.md`](tema-11-chat.md) |

El caso más flojo hoy es el 1: no hay copy ni pantalla acordada para `unavailable` — ver
[`tema-05-desafios-practicos/pendientes.md`](../../07-planificacion-y-trabajo-equipo/11-equipos/tema-05-desafios-practicos/pendientes.md).

## Estados de error y contrato visual

La tabla de mensajes visibles y los siete estados (`inactivo`, `enviando`, `pensando`, `respondido`,
`bloqueado`, `sin_servicio`, `cuota_agotada`) están definidos una sola vez en [Operación e
ingeniería §6–§7](../../06-operacion-calidad-y-pruebas/01-operacion-e-ingenieria.md#6-qué-ve-el-usuario-cuando-algo-falla).
El Front End debe implementar esos valores literalmente, incluyendo el indicador `pensando`, los
mensajes genéricos de rechazo y el contador de cuota; no debe crear una variante local.

## Nota aparte, no es contrato

`15-sincronizacion-arquitectura-y-despliegue.md` sincroniza con la unidad curricular de Front
End (Programación IV) — nginx como gateway, rolling update, etc. **No define contratos ni
requisitos nuestros**, es contenido pedagógico compartido que además nutre el glosario de
infraestructura (`11-glosario-y-metadata.md`).
