# `POST /api/llm/rag/chat` — consulta con citas de fuente/página

- **Estado:** 🟡 Camino completo con guardarraíles y citas; sin proveedor real de LLM ni de embeddings
- **Contrato:** [`docs/contracts/llm-service-v1.openapi.yaml`](../../../contracts/llm-service.openapi.yaml)
  (`RagChatRequest`/`Response`, `RagSourceCitation`)
- **Historia:** [`docs/historias/ep-09/h02.md`](../../../07-planificacion-y-trabajo-equipo/09-epicas-historias-tareas-sprints/historias/ep-09/h02.md)
- **Código:** `api/RagController`, `application/{RagChatService,RagQueryGuardrail}`,
  `domain/rag/VectorStorePort`, `infrastructure/persistence/PgVectorStoreAdapter`

## Qué hace

Portado de `demoLLMSpringAi/BE/.../rag/service/TutorRagService.java`:

1. **Idempotencia** — reutiliza `IdempotencyRepository`, mismo patrón que el tutor sin RAG.
2. **Sin fuentes seleccionadas o ninguna autorizada** — responde `BLOCKED_NO_SOURCE` sin invocar
   ni al embedding ni al modelo (0 tokens). "Autorizada" significa: pertenece a un
   `RagDocument` **activo** del `courseCohortId` del request — nunca se mezcla material de otra
   cohorte (`AGENTS.md` §2), a diferencia de la demo, que era single-tenant y no podía garantizar
   esto.
3. **Guardarraíles** (`RagQueryGuardrail`) — longitud, spam, cooldown, profanidad, y jailbreak
   delegado a `InputGuard` (no una lista propia).
4. **Caché** — pregunta idéntica sobre el mismo conjunto de fuentes, servida sin re-invocar nada.
5. **Embedding de la pregunta** (`EmbeddingInvocationService`, vía `FakeEmbeddingAdapter` hoy).
6. **Búsqueda semántica** (`VectorStorePort.searchTopK`, top 8 para contexto, top 4 para citas —
   se reutiliza el mismo resultado en vez de hacer una segunda consulta, a diferencia de la demo).
7. **Histórico multi-turno** — últimos 2 turnos (4 mensajes) en el prompt, misma ventana que usa
   el tutor sin RAG.
8. **Prompt con citas** (`prompts/tutor-rag/{system,user}-v1.txt`) — vía
   `ModelInvocationService.invoke(ModelFunction.TUTOR, ...)`, **sin** el fallback "modelo lite →
   full" que tenía la demo ante error: acá el modelo activo es una fila de
   `function_model_config`.
9. **Auditoría** — una fila en `audit_events` por interacción RAG.

## Qué NO se portó (decisión explícita, no descuido)

- **Fallback de modelo ante error** ("modelo lite → full" de la demo) — el modelo activo se
  cambia administrativamente (`PUT /model-assignments/{function}`), no en código.
- **`state=blocked`** del contrato del tutor — el chat RAG usa sus propios estados
  (`BLOCKED_NO_SOURCE`, `BLOCKED_PROFANITY`, etc., `UNAVAILABLE`), no reutiliza el enum del
  tutor sin RAG.
- **Validación V1 de "documento existente" del `GuardrailService` original** — en la demo
  comprobaba `vectorStore.hasDocument(documentId)` contra una clave compuesta que en la práctica
  nunca coincidía (bug de la demo, no una regla real); acá esa validación se resuelve
  correctamente antes, filtrando `documentIds` contra los `RagDocument` activos del
  `courseCohortId`.

## Autorización

`RagGatewayAuthorization` — ver [`ingesta.md`](ingesta.md#autorización).

## Tests

`RagChatServiceTest`: sin `documentIds` (`BLOCKED_NO_SOURCE`), documentos de otra cohorte nunca
autorizados, guardarraíl bloquea antes de calcular el embedding, camino feliz con citas y
persistencia de ambos mensajes, idempotencia (replay sin reinvocar el modelo).
`RagQueryGuardrailTest`: los 7 motivos de bloqueo (vacío, corto, largo, spam, rate limit,
profanidad, injection) y que solo las respuestas `OK` se cachean.
