# EP-09 · RAG y consulta de material (F3) — estado

> **2026-09-13 — primera vez que EP-09 tiene código en `llm-service`.** Hasta ayer estaba
> "sin código encontrado" (tablero de estado), con la implementación de referencia viviendo solo
> en `demoLLMSpringAi` y documentada como spike en
> [`docs/31-spike-decodificacion-imagenes-y-rag-multifuente.md`](../../../03-capacidades-de-ia/rag-e-ingesta/02-spike-rag-multifuente.md).
> Se portó a pedido explícito, adelantando el trabajo de S14–S16
> ([`35`](../../../07-planificacion-y-trabajo-equipo/04-backlog-ejecutable.md)) — no existían `docs/historias/ep-09/` todavía; se
> crearon junto con este código (ver [`docs/historias/ep-09/`](../../../07-planificacion-y-trabajo-equipo/09-epicas-historias-tareas-sprints/historias/ep-09/README.md)).

## Índice

| Endpoint / flujo | Código | Estado | Nota |
|---|---|---|---|
| [Ingesta e indexado](ingesta.md) | `RagController` (`/documents`), `RagIngestionService` | 🟡 | Sin embeddings reales; PDF en `BYTEA`, no filesystem ni referencia externa |
| [Chat con citas](chat.md) | `RagController` (`/chat`), `RagChatService`, `RagQueryGuardrail` | 🟡 | Sin proveedor real de LLM; `state=blocked` heredado sin usar |

## Qué se portó de `demoLLMSpringAi` y qué no

Mapeo completo en el plan de implementación (`plan.md` de la sesión que hizo este trabajo, no
versionado en el repo) — resumen:

- **Se portó** (reescrito detrás de puertos, no copiado literal): extracción de texto PDF
  (PDFBox), chunking con solapamiento, detección/decodificación determinística de diagramas
  (heurística de bounding boxes, **no** el pipeline de visión OpenCV/Tess4J del spike doc 31),
  búsqueda vectorial pgvector, orquestación de prompt con citas de fuente/página, y el
  `GuardrailService` (cooldown, caché, profanidad, spam) — sin duplicar la detección de
  jailbreak, que se delega a `InputGuard` ya existente.
- **No se portó:** el fallback automático a búsqueda en memoria (TF-IDF) que tenía la demo para
  correr sin Postgres — la infraestructura de `llm-service` siempre tiene Postgres+pgvector
  (regla de plataforma), así que esa rama no es código de producción acá. Tampoco se portó el
  almacenamiento en filesystem del PDF (decisión: solo `BYTEA` en `rag_documents`), ni el
  fallback de "modelo lite → modelo full" ante error (acá el modelo activo es una fila de
  `function_model_config`, se cambia administrativamente, mismo criterio que `LLM-S01-H10`).

## Huecos a dejar dicho, no inventado

1. **Decodificación de diagramas es heurística** (bounding boxes + texto vectorial de PDFBox),
   no el pipeline de visión por computadora (OpenCV/Tess4J) que describe el spike
   [`docs/31`](../../../03-capacidades-de-ia/rag-e-ingesta/02-spike-rag-multifuente.md). No presentarlo como
   "detección con IA".
2. **Sin proveedor real de embeddings ni de LLM conectado.** `FakeEmbeddingAdapter` y
   `FakeModelAdapter` son las únicas implementaciones — el chat RAG funciona end-to-end pero con
   datos simulados, no con calidad real de recuperación semántica.
3. **PDF almacenado como `BYTEA`, no como referencia externa.** El catálogo de S15 en `35`
   explícitamente pide "sin binarios en Postgres; hashes y referencias autorizadas" para la
   ingesta visual — esta pasada (alcance de S14, ingesta textual) sí guarda el PDF completo en la
   base como simplificación deliberada, a revisar antes de escalar a producción con archivos
   grandes o múltiples instancias.
4. **Autorización de `/rag/**`** usa un scope M2M propio (`RagGatewayAuthorization`,
   `llm.rag.query`), separado del tutor — decisión tomada al planificar, no derivada de un
   contrato ya acordado con otro equipo.
5. **`state=blocked`** del contrato del tutor sigue sin producirse (heredado de
   [`ep-05/interactions.md`](../ep-05/interactions.md)); el chat RAG usa sus propios estados
   `BLOCKED_*`/`UNAVAILABLE`, no reutiliza ese enum.
6. **KPIs de la épica** (`docs/epicas/ep-09.md`: 85% de recuperación correcta, sin recuperación
   cruzada entre cohortes) no se midieron con un corpus real — el aislamiento por cohorte sí está
   codificado y cubierto por test (`RagChatServiceTest.documentsFromAnotherCohortAreNeverAuthorized`),
   pero la métrica de calidad de recuperación no se evaluó contra datos reales de cátedra.

## Tests y cobertura real (medida con jacoco, agregado en esta pasada — no estaba configurado)

`TextChunkerTest`, `FakeEmbeddingAdapterTest`, `EmbeddingInvocationServiceTest` (incluye timeout),
`RagQueryGuardrailTest`, `RagIngestionServiceTest`, `RagChatServiceTest` (aislamiento por cohorte,
`BLOCKED_NO_SOURCE`, idempotencia, caché con dos alumnos distintos), `PdfTextExtractionAdapterTest`
y `PdfDiagramDetectionAdapterTest` (PDFs construidos en memoria con PDFBox, sin fixtures externos),
`RagGatewayAuthorizationTest`, `RagControllerTest`. **179 tests corriendo, 0 fallos.**

Cobertura de instrucciones de todo el código nuevo/tocado por este trabajo (EP-05 + EP-09):
**76.2%** (líneas: 77.5%) — por debajo del 95% que exige `AGENTS.md`. El hueco no está repartido
parejo:

| Clase | Cobertura | Por qué |
|---|---:|---|
| `PgVectorStoreAdapter` | **0%** | Necesita Postgres+pgvector real; `FlywaySchemaTest` (Testcontainers) no pudo correr en el entorno de desarrollo de esta sesión (Docker negocia una API vieja, `1.32` vs. mínimo `1.40`) |
| `ConversationRepository` | **0%** | Ídem — JDBC directo, sin mock posible sin una BD real |
| `RagDocumentRepository` | **0%** | Ídem |
| `MessageRepository` | **0%** | Ídem |
| `PdfDiagramDetectionAdapter` | 76.9% | Cubierto el camino principal; faltan ramas de la heurística (filtrado de encabezados repetidos entre páginas, relación de aspecto extrema) |
| `RagChatService` / `TutorInteractionService` | 85–87% | Ramas de guardarraíles/errores menos comunes sin cubrir |
| Resto (`RagController`, `ConversationController`, `RagGatewayAuthorization`, DTOs, records) | 90–100% | — |

**Las cuatro clases en 0% son las que efectivamente hablan con Postgres/pgvector** — representan
709 de las 1231 instrucciones sin cubrir (~58% del hueco total). Sin ellas verificadas, **nada de
EP-09 debería darse por "terminado" end-to-end**: la lógica de negocio está probada a fondo con
mocks, pero la consulta SQL real (`embedding <=> ?`, filtros por `document_id`) nunca se ejecutó
contra una base real en esta sesión. Correr antes de dar esto por cerrado:
```bash
mvn test -Dintegration=true -Dtest=FlywaySchemaTest
```
y, ya con eso funcionando, agregar tests de integración para los cuatro repositorios.
