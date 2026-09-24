# Historias de usuario — EP-05 · Tutor seguro y guardarraíles

> Fichas en el formato del [template oficial de Historia de Usuario de la Wiki de
> Taiga](../../../10-plantillas/historia-de-usuario-taiga.md). **Carpeta nueva.** El código de EP-05
> se construyó el 2026-09-12/13 sin ficha de HU — ver
> [`docs/estado-implementacion/ep-05/README.md`](../../../../06-operacion-calidad-y-pruebas/04-estado-de-implementacion/ep-05/README.md),
> que ya marcaba explícito: *"Historia de usuario formal para EP-05 — este código se adelantó sin
> ficha."* Estas dos fichas cierran ese hueco.
>
> **Fuente de verdad.** Épica: [`../../epicas/ep-05.md`](../../epicas/ep-05.md). Contrato:
> [`docs/contracts/llm-service-v1.openapi.yaml`](../../../../contracts/llm-service.openapi.yaml) +
> [adenda SSE](../../../../contracts/llm-service-tutor-interactions-stream-propuesta.md). DoR/DoD:
> [23 · §9.2](../../../03-plan-de-construccion-del-producto.md).

## Índice

| ID | Título | Tipo | Pareja | Dep. | Estado |
|---|---|---|---|---|---|
| [LLM-EP05-H01](h01.md) | Recibir una respuesta socrática del tutor, filtrada por los dos guardarraíles *(ex-S05-H01)* | **HU** de valor | P3+P2 | `LLM-EP02-H01` | 🟢 Construida — camino síncrono completo; streaming fuera de alcance (documentado, no es hueco) |
| [LLM-EP05-H02](h02.md) | Retomar una conversación anterior con el tutor *(ex-S05-H02)* | **HU** de valor | P3+P2 | `LLM-EP05-H01` | 🟢 Construida — repositorios sin cobertura de integración real todavía (bloqueo de Docker/Testcontainers) |
| [LLM-EP05-H03](h03.md) | Retomar el hilo de una consulta con el tutor sin perder el contexto *(ex-S05-H03)* | **HU** de valor | P3 | `LLM-EP05-H01`, `LLM-EP05-H02` | ⚪ Borrador |
| [LLM-EP05-H04](h04.md) *(propuesta)* | Aviso claro al alumno cuando supera su cuota de consultas al tutor | **HU** de valor | P3+P2 | `LLM-EP05-H01`, `EP-07·H02`/`H03` | ⚪ Propuesta — sin alta en `35` |

> **H04 es una propuesta, no backlog confirmado.** Cierra el KPI de épica de cuota por alumno,
> que hoy ninguna ficha de EP-05 cubre — reutiliza el mecanismo de cuota de EP-07, no lo duplica.
>
> Las fichas H01–H03 están **escritas a posteriori**: documentan código ya construido, no
> planificación a futuro. Los puntos Fibonacci siguen sin asignar — se fijan en Refinamiento
> igual que cualquier otra historia, aunque ya esté hecha, para que Taiga tenga el número.
