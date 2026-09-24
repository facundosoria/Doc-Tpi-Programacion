# `POST/GET /api/llm/tutor/conversations` y `GET /{id}/messages` — CRUD e histórico

- **Estado:** 🟡 Construido; revisita una decisión previa de no portarlo
- **Contrato:** [`docs/contracts/llm-service-v1.openapi.yaml`](../../../contracts/llm-service.openapi.yaml)
  (`Conversation`, `Message`, `CreateConversationRequest`)
- **Código:** `api/ConversationController`, `application/ConversationService`,
  `domain/tutor/{Conversation,Message}`, `infrastructure/persistence/{ConversationRepository,MessageRepository}`.
  `TutorInteractionService` también los usa internamente para el histórico multi-turno de
  `POST /tutor/interactions` (ver [`interactions.md`](interactions.md)).

> **2026-09-13 — se revisita la decisión de `interactions.md` de no portar este CRUD.**
> Motivo: pedido explícito del usuario de traer **todo** lo de `demoLLMSpringAi` (tutor CRUD +
> histórico + RAG + guardarraíles) a `llm-service`. La decisión anterior no estaba equivocada
> para su momento (el contrato acordado hasta entonces solo definía la interacción síncrona) —
> simplemente cambió el alcance pedido. Este archivo documenta el cambio; `interactions.md` queda
> actualizado con un link acá en vez de seguir afirmando que el CRUD "no está en ningún contrato
> acordado".

## Qué hace

Portado de `demoLLMSpringAi/BE/.../controller/ChatController.java` (CRUD) y
`.../service/TutorSocraticoService.java` (histórico), fusionado en `TutorInteractionService`
existente en vez de un servicio paralelo:

1. **`POST /tutor/conversations`** — crea una conversación (`courseCohortId`, `learnerId`,
   `challengeId` opcional, `titulo`), con idempotencia (`IdempotencyRepository`, operación
   `tutor.conversation.create`) y auditoría.
2. **`GET /tutor/conversations`** — lista, filtrable por `learnerId`/`courseCohortId`.
3. **`GET /tutor/conversations/{id}/messages`** — histórico completo, ordenado por timestamp.
4. **`conversacionId` en `TutorInteractionRequest`/`Response`** (opcional en el request, siempre
   presente en la response) — si no viene, `TutorInteractionService` igual crea una conversación
   de un solo turno de forma transparente (no cambia `message`/`state` para callers que ya
   ignoraban este campo — compatibilidad hacia atrás real, no solo de contrato). Si viene,
   resuelve o crea la conversación, carga los últimos 2 turnos de histórico y los pasa al prompt
   (`{historico}` en `prompts/tutor/user-v1.txt`, que antes se reemplazaba por `""`).

## Qué NO se portó (decisión explícita, no descuido)

- **Lista de keywords de jailbreak propia de `TutorSocraticoService`** — se descarta; se usa
  `InputGuard.isJailbreak` ya existente (unifica las listas que había dispersas en la demo).
- **`ChatController.enviarMensaje`** como endpoint separado del tutor — no se creó un tercer
  endpoint: enviar un mensaje con histórico sigue siendo `POST /tutor/interactions` (con
  `conversacionId`), no un endpoint nuevo bajo `/conversations/{id}/mensajes`. Si el producto
  necesita esa forma específica de la ruta, es una decisión de contrato pendiente.
- **`state=blocked`** sigue sin producirse (heredado de [`interactions.md`](interactions.md)) —
  esto no cambió con este trabajo.

## Autorización

Reusa `TutorGatewayAuthorization` (mismo scope M2M que el tutor, `llm.tutor.interact`) — a
diferencia de RAG (EP-09), que sí tiene un scope propio por ser una épica distinta.

## Tests

`ConversationServiceTest` (validación de campos obligatorios, creación, idempotencia,
"conversación no encontrada"), `ConversationControllerTest`,
`TutorInteractionServiceTest.aRequestWithAnExistingConversationIdReusesItAndCarriesHistory`.
`ConversationService` da 83% de cobertura de instrucciones; `ConversationController`, 100%.
**`ConversationRepository`/`MessageRepository` (JDBC directo) están en 0%** — necesitan Postgres
real; `FlywaySchemaTest` (Testcontainers) no pudo correr en esta sesión (ver
[`ep-09/README.md`](../ep-09/README.md) para el detalle del bloqueo de Docker). No dar este CRUD
por verificado end-to-end hasta correr esa integración.
