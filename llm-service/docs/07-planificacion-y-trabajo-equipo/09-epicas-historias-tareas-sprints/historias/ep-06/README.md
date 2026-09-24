# Historias de usuario — EP-06 · Evaluación, score y auditoría académica

> Fichas en el formato del [template oficial de Historia de Usuario de la Wiki de
> Taiga](../../../10-plantillas/historia-de-usuario-taiga.md). **Carpeta nueva.** EP-06 estaba en
> **🔴 no iniciado** hasta esta pasada — ver la auditoría completa en
> [`docs/estado-implementacion/ep-06/evaluacion-y-apelacion.md`](../../../../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-06/evaluacion-y-apelacion.md):
> grep de todo el repo, cero resultados. Estas tres fichas son el primer recorte, pensado para
> entrar en el mismo sprint que lo que ya está abierto de EP-04 (`LLM-S04-H01` y los paquetes de
> vencimiento/avisos).
>
> **Fuente de verdad.** Épica: [`../../epicas/ep-06.md`](../../epicas/ep-06.md). Requisitos:
> RF-IA-12 a 18/25/27/34. Contrato de eventos:
> [`../../contracts/llm-service-v1.asyncapi.yaml`](../../../../contracts/llm-service.asyncapi.yaml)
> (`ATTEMPT_CLOSED` consumido, `SCORE_CALCULATED` y `SCORE_DEFERRED`
> publicados — **el `data` de estos dos últimos todavía es un `Envelope` vacío en el contrato: sin
> schema de campos acordado**, no inventar de qué se compone en ninguna ficha). DoR/DoD:
> [23 · §9.2](../../../03-plan-de-construccion-del-producto.md).

## Índice

| ID | Título | Tipo | Dep. | Estado |
|---|---|---|---|---|
| [LLM-EP06-H01](h01.md) | Consumir el cierre de un intento y encolar su evaluación *(ex-S06-H01)* | Tarea (habilitador) | Bus de eventos, compuerta de [EP-04](../ep-04/README.md) | ⚪ Borrador |
| [LLM-EP06-H02](h02.md) | Recibir un puntaje explicado en las cinco dimensiones *(ex-S06-H02)* | **HU** de valor | H01, `LLM-EP02-H01` (✅), rúbrica vigente (EP-03) | ⚪ Borrador |
| [LLM-EP06-H03](h03.md) | Que mi entrega se acepte igual si el evaluador está caído *(ex-S06-H03)* | **HU** de valor | H02 | 🟡 Parcial — el aviso de diferido ya se publica; falta persistir el pendiente, reintentar y no duplicar por intento |
| [LLM-EP06-H04](h04.md) | Impugnar la nota que me dio la IA en una entrega *(ex-S07-H01)* | **HU** de valor | H02 (evaluación completada) | ⚪ Borrador |
| [LLM-EP06-H05](h05.md) | Que el docente pueda corregir la nota de la IA sin borrar lo que ya había *(ex-S07-H02)* | **HU** de valor | H04, H02 | ⚪ Borrador |
| [LLM-EP06-H06](h06.md) | Consultar si un curso puede cerrarse o tiene evaluaciones pendientes de resolver *(ex-S07-H03)* | Tarea (habilitador M2M) | H01, H04 | ⚪ Borrador |

> No confundir esta carpeta con [`estado-implementacion/ep-06/`](../../../../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-06/README.md):
> esa documenta lo que **no** hay; esta propone lo que se construiría a continuación.
