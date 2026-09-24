# Historias de usuario — EP-02 · AI Gateway, modelos y resiliencia

> Fichas en el formato del [template oficial de Historia de Usuario de la Wiki de
> Taiga](../../../10-plantillas/historia-de-usuario-taiga.md). **H01** (anteriormente H10) es un **habilitador
> técnico** adelantado a S1 en la reprogramación de 8 semanas: falla la **V** de INVEST
> (el `COMO` es «el equipo»), así que en Taiga se carga como **tarea** bajo EP-02, sin
> puntos de valor. El formato largo acá es para trazar los escenarios de aceptación.
>
> **Por qué existe esta carpeta.** El catálogo original ([`35`](../../../04-backlog-ejecutable.md))
> asigna EP-02 a S3, S8 y S9. La reprogramación a 8 semanas adelanta su primer paquete
> ("Puerto AI Gateway y fake") a **S1**, en paralelo con EP-01/EP-03, porque no depende
> del golden set publicado — solo del ADR de convenciones (H01). Detalle de la
> reprogramación en [`../../sprints/README.md`](../../sprints/README.md).
>
> **Fuente de verdad.** ID, épica, pareja, dependencias y horas mandan desde
> [`35` · «S1»](../../../04-backlog-ejecutable.md). DoR/DoD, desde
> [23 · §9.2](../../../03-plan-de-construccion-del-producto.md). El desglose en **tareas
> SMART** está en [`../../tareas/ep-02/`](../../tareas/ep-02/README.md).

## Índice

| ID | Título | Tipo | Pareja | Dep. | h |
|---|---|---|---|---|--:|
| [LLM-EP02-H01](h01.md) | Puerto del proveedor de modelos (AI Gateway) y fake para pruebas *(ex-H10)* | Tarea (habilitador) | P2 | EP-01·H01 | 32 |
| [LLM-EP02-H02](h02.md) | Conectar un proveedor real detrás del puerto de invocación *(ex-H11)* | Tarea (habilitador) | P2 | H01 (✅) | ~25–35 (referencia) |
| [LLM-EP02-H03](h03.md) *(propuesta)* | Resiliencia síncrona de la invocación (reintentos y circuit breaker) | Tarea (habilitador) | P2 (sugerido) | H01 (✅) | *(a fijar; referencia ~18–24)* |

> **H03 es una propuesta, no backlog confirmado.** Cierra la restricción de épica "reintentos y
> límites de tiempo controlados... para que una demora de un proveedor no bloquee al resto del
> sistema" ([`epicas/ep-02.md`](../../epicas/ep-02.md)) — H01/H02 solo resuelven el timeout de una
> llamada individual. No depende de H02: se prueba contra el fake, así que puede refinarse y
> construirse en paralelo.
>
> No es parte del criterio de demo de S1 (ese sigue siendo el golden set, EP-03). Es
> preparación de EP-02 para que S3 no arranque de cero.
>
> **`LLM-EP02-H02` (ex `LLM-S03-H11`) es el único bloqueante duro del alcance obligatorio de la cátedra** — ver
> [38 · Parte 1](../../../05-plan-de-cinco-sprints.md). Entra en el **Sprint 2** del plan vigente de
> 5 sprints, no en un "S3" de calendario.
