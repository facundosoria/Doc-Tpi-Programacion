# EP-05 · Tutor seguro y guardarraíles — estado

> **Primera vez que EP-05 tiene código en `llm-service`** (2026-09-12). No existe todavía
> `docs/historias/ep-05/` — esta ficha de estado no reemplaza esa historia pendiente; documenta
> lo que ya se construyó contra el contrato ya acordado
> ([`docs/contracts/llm-service.openapi.yaml`](../../../contracts/llm-service.openapi.yaml) +
> [propuesta de streaming SSE](../../../contracts/llm-service-tutor-interactions-stream-propuesta.md),
> todavía no fusionada), portado de
> [`codigo-ejemplo/ms-evaluacion-llm`](../codigo-ejemplo/ms-evaluacion-llm.md) (carpeta ya
> eliminada).

## Índice

| Endpoint | Código | Estado | Nota |
|---|---|---|---|
| [interactions](interactions.md) | `TutorInteractionController`, `TutorInteractionService` | 🟡 | Camino síncrono completo con guardarraíles e histórico; streaming/SSE fuera de esta pasada |
| [conversations](conversations.md) | `ConversationController`, `ConversationService` | 🟢 | CRUD + histórico multi-turno — revisita una decisión previa, ver el archivo |

## Pendiente (no se hizo en esta pasada, dejarlo dicho en vez de inventarlo)

- **Historia de usuario formal** para EP-05 (`docs/historias/ep-05/`) — este código se adelantó
  sin ficha, igual que pasó con golden set/calibración tras `605f381`.
- **Cuota por alumno en el tutor** (revisado 2026-09-20) — `POST /tutor/interactions` **no aplica
  ningún límite por alumno**: ni `TutorInteractionController` ni `TutorInteractionService` llaman a
  `QuotaRegistry.consume`. Lo único que frena al tutor es `GatewayBudget.check(TUTOR)` dentro del
  gateway, un techo **global** de la función (5000 llamadas o 10 USD por día, valores fijos y
  contadores en memoria). Consecuencias:
  - `QuotaRegistry` registra `FUNCTION_TUTOR` con 50 usos/día por defecto, pero nadie lo consume
    (el único `consume` real es `AgentMentionService`, con `FUNCTION_AGENT`). Además 50 no
    coincide con los 60/día de RF-IA-22 / P-05.
  - El tope de 15 mensajes por desafío no está aplicado en ningún lado.
  - No hay límite de tokens por alumno. `GatewayUsageLog` estima tokens (chars/4) pero solo
    agregados por función, sin `learnerId`.
  - Un alumno que agota el presupuesto global recibe `429 budget_exceeded` (`ApiExceptionHandler`)
    y afecta a todos los demás; nada le pone un techo individual.
  - **Dónde está la historia:** el diseño ya existe en EP-07, no en EP-05. Configurar el límite
    (función y alumno, con auditoría) es
    [`ep-07/h02.md`](../../../07-planificacion-y-trabajo-equipo/09-epicas-historias-tareas-sprints/historias/ep-07/h02.md)
    (CA9) y aplicarlo con el aviso de reintento es
    [`ep-07/h03.md`](../../../07-planificacion-y-trabajo-equipo/09-epicas-historias-tareas-sprints/historias/ep-07/h03.md)
    (CA9, el tutor figura entre las funciones sujetas a límite). Quien implemente H03 tiene que
    engancharlo en `TutorInteractionService.respond`, antes de `invokeModel`; EP-05 no tiene
    historia propia que lo pida.
  - Ver [08 P-12](../../../00-gobierno-y-evolucion/02-decisiones-y-pendientes.md) y
    [`tema-12-backoffice-admin/pendientes.md`](../../../07-planificacion-y-trabajo-equipo/11-equipos/tema-12-backoffice-admin/pendientes.md):
    el límite configurable desde el back office sigue en propuesta.
- **Streaming SSE** (`POST /tutor/interactions/stream`, Buffer Interceptor PAR-11) — la adenda
  dice explícitamente que hasta que se fusione, el contrato ejecutable es solo el síncrono.
- **Adaptador real a un proveedor** (langchain4j, ADR-016) — hoy `FakeModelAdapter`
  ([`ep-02/h10.md`](../ep-02/h10.md)) es el único adaptador; ningún proveedor real está conectado.
- **Moderador** (`EP-08`) — el contrato de referencia se preservó en
  [`docs/contracts/llm-service-v1-moderacion-borrador.yaml`](../../../contracts/llm-service-v1-moderacion.openapi.yaml)
  pero no tiene código.
