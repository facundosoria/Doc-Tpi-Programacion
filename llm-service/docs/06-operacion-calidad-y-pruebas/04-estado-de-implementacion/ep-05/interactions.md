# `POST /api/llm/tutor/interactions` — interacción síncrona del tutor

- **Estado:** 🟡 Camino síncrono completo; streaming fuera de alcance (documentado, no es un hueco)
- **Contrato:** [`docs/contracts/llm-service-v1.openapi.yaml`](../../../contracts/llm-service.openapi.yaml)
  (`TutorInteractionRequest`/`Response`),
  [propuesta de streaming SSE](../../../contracts/llm-service-tutor-interactions-stream-propuesta.md)
  (todavía no fusionada, ver I-10)
- **Demo con modelo real (Groq):** [`demo-h01.md`](demo-h01.md) — los tres escenarios de la ficha, con respuestas y auditoría reales
- **Código:** `api/TutorInteractionController`, `application/TutorInteractionService`,
  `domain/ai/InputGuard`, `domain/ai/OutputAntiLeakGuard`, `security/TutorGatewayAuthorization`

## Qué hace

Portado (adaptado) de `codigo-ejemplo/ms-evaluacion-llm`
(`TutorServiceImpl`/`InputGuard`/`OutputAntiLeakGuard`):

1. **Idempotencia** — reutiliza `IdempotencyRepository` (la misma pieza que ya usan golden set e
   import), no se creó nada nuevo para esto: reintento con la misma `Idempotency-Key` devuelve la
   misma respuesta sin invocar al modelo de nuevo.
2. **Guardarraíl de entrada** — `InputGuard.isJailbreak` (normalización Unicode antes de comparar
   contra keywords) corta antes de llamar al modelo; responde con un mensaje fijo,
   `state=completed`.
3. **Prompt** — `prompts/tutor/system-v1.txt`/`user-v1.txt` (portados tal cual), cargados una vez
   por instancia del service.
4. **Invocación** — vía [`ModelInvocationService`](../ep-02/h10.md). Si el modelo no puede responder (timeout,
   respuesta inválida, proveedor caído o con breaker abierto, presupuesto agotado, función sin modelo
   asignado) el servicio responde `200` con `state=unavailable` y un aviso fijo, en vez de un error HTTP:
   así la `Idempotency-Key` queda completada y no reservada sin respuesta (que hacía responder "sigue en
   curso" a todo reintento). Una respuesta `unavailable` queda guardada bajo esa clave: para reintentar hay
   que usar una nueva.
5. **Guardarraíl de salida** — `OutputAntiLeakGuard` tiene dos reglas con alcance distinto
   (decidido en la integración main↔dev del 2026-09-21, donde `main` filtraba siempre y `dev`
   salteaba `low`):
   - `revealsExpectedSolution` — compara contra el `expectedSolution` opcional que manda Tema 05
     (solo en memoria). Corre en **todos** los niveles de riesgo: que el llamador declare `low` no
     puede habilitar que le devolvamos la solución del desafío.
   - `looksLikeCode` — heurística de forma de código (bloque largo, snippet inline, línea con forma
     de código). Corre solo en `high`/`medium`, no en `low`, según la propia adenda SSE: en `low`
     sobre-bloquea respuestas legítimas.

   Si cualquiera de las dos da positivo, reemplaza el mensaje por una redirección socrática.
6. **Auditoría** — una fila en `audit_events` (reutilizado, no se creó tabla nueva) por
   interacción, con `courseCohortId`/`learnerId`/`riskLevel`/`state`/si se disparó un guardarraíl.

## Qué NO se portó (decisión explícita, no descuido)

> **🟢 2026-09-13 — revisada.** El CRUD de conversaciones y el histórico multi-turno, que este
> archivo daba por descartados, **sí se portaron** a pedido explícito del usuario — ver
> [`conversations.md`](conversations.md). Se deja el texto original abajo (tachado en el
> historial, no en el archivo) solo para trazar que la decisión cambió, no para que quede como
> vigente.

- ~~**`TutorChatController`** (CRUD de conversaciones, `/api/conversaciones`) — no está en ningún
  contrato acordado. El tutor real audita cada interacción pero no expone un historial navegable
  por API todavía.~~ Portado, ver [`conversations.md`](conversations.md).
- ~~**Persistencia de conversación/histórico multi-turno** — `TutorServiceImpl` original mantenía
  un histórico completo por conversación; el contrato v1 (`TutorInteractionRequest`) no modela una
  `conversacionId`, así que cada interacción se trata independiente. Si el producto necesita
  histórico multi-turno, es un cambio de contrato, no de este código.~~ El contrato v1 ahora sí
  modela `conversacionId` (opcional en el request, siempre presente en la response) — ver
  [`conversations.md`](conversations.md).
- **`state=blocked`** — el enum del contrato lo permite, pero esta implementación nunca lo
  produce: cuando el guardarraíl de salida actúa, sustituye el mensaje y queda `completed` (la
  adenda SSE reserva `blocked` para cuando la respuesta final se suprime por completo, escenario
  que en la variante sin streaming no se modeló). A revisar si el producto quiere distinguirlo.

## Autorización

`TutorGatewayAuthorization` (archivo nuevo, mismo patrón M2M que `GoldenSetAuthorization`:
servicio confiable + scope + usuario delegado) — no hay chequeo de "el alumno pertenece al
curso" a nivel de header: `courseCohortId`/`learnerId` viajan en el body, igual que en el
contrato. Si se necesita esa validación, es una decisión de producto pendiente, no algo que este
código asumiera.

## Tests

`InputGuardTest`, `OutputAntiLeakGuardTest`, `TutorInteractionServiceTest` (jailbreak nunca llama
al modelo, guardarraíl de salida por `riskLevel`, idempotencia), `TutorInteractionControllerTest`.
