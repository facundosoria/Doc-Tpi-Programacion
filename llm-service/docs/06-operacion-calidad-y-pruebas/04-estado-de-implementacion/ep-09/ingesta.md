# `POST/GET/DELETE /api/llm/rag/documents/**` — ingesta e indexado de fuentes

- **Estado:** 🟡 Camino completo de subida/indexado/retiro construido; sin embeddings reales
- **Contrato:** [`docs/contracts/llm-service-v1.openapi.yaml`](../../../contracts/llm-service.openapi.yaml)
  (`RagDocument`, `DocumentChunk`, `ImageDetection`, `DiagramDecodeResult`)
- **Historia:** [`docs/historias/ep-09/h01.md`](../../../07-planificacion-y-trabajo-equipo/09-epicas-historias-tareas-sprints/historias/ep-09/h01.md)
- **Código:** `api/RagController`, `application/RagIngestionService`, `domain/rag/*`,
  `infrastructure/rag/{PdfTextExtractionAdapter,PdfDiagramDetectionAdapter}`,
  `infrastructure/persistence/{RagDocumentRepository,PgVectorStoreAdapter}`

## Qué hace

Portado de `demoLLMSpringAi/BE/.../controller/RagController.java` (parte de `processAndIndexPdf`)
y sus services de `rag/service/`:

1. **Validación de subida** — solo `.pdf`, no vacío, hasta 25MB (`llm.rag.max-upload-bytes`).
2. **Extracción de texto por página** (`PdfTextExtractionAdapter`, Apache PDFBox) — portado tal
   cual de `PdfTextExtractorService`.
3. **Chunking** (`domain/rag/TextChunker`, puro, sin Spring) — fragmentos de ~1000 caracteres con
   200 de solapamiento, respetando fronteras de oración cuando es posible.
4. **Detección y auto-decodificación de diagramas** (`PdfDiagramDetectionAdapter`) — heurística
   determinística de bounding boxes + texto vectorial de la página (**no** visión por
   computadora); los diagramas decodificados con éxito se agregan como chunks adicionales.
5. **Embeddings por lote** (`EmbeddingInvocationService.embedBatch`, vía `FakeEmbeddingAdapter`
   hoy) — un fragmento sin vector no aborta la indexación completa.
6. **Persistencia** — metadata + `pdf_bytes` en `RagDocumentRepository` (JDBC directo, sin puerto,
   igual que el resto de repos del servicio); chunks + embeddings en `PgVectorStoreAdapter`
   (puerto `VectorStorePort`, implementación pgvector).
7. **Retiro lógico** (`DELETE /documents/{id}`) — `active = false`, conserva historial de
   auditoría; no borra la fila ni sus chunks.
8. **Inspección** — `GET /documents/{id}/chunks`, `GET /documents/{id}/images`,
   `POST /documents/{id}/images/{imageIndex}/decode`, `POST /documents/{id}/diagrams` (indexar
   manualmente un diagrama ya decodificado).

## Qué NO se portó (decisión explícita, no descuido)

- **`FileStorageService`** (guardar el PDF en disco local) — decisión tomada al planificar: solo
  `pdf_bytes` en la tabla, sin filesystem (no multi-instancia-safe). El catálogo de S15 en
  [`35`](../../../07-planificacion-y-trabajo-equipo/04-backlog-ejecutable.md) de hecho pide lo contrario para la ingesta visual ("sin
  binarios en Postgres") — esta pasada es alcance de S14 (ingesta textual) y prioriza simplicidad
  de despliegue sobre esa recomendación futura; a revisar antes de escalar.
- **`InMemoryRagVectorStore`** (fallback TF-IDF automático sin Postgres) — no es rama de
  producción en `llm-service` (siempre hay pgvector). No se portó ni como test double activo.
- **Pipeline de visión (OpenCV/Tess4J)** del spike `docs/31` — la decodificación de diagramas
  sigue siendo la heurística de la demo, no el pipeline completo que el spike investigó.

## Autorización

`RagGatewayAuthorization` (`llm.rag.trusted-service`/`llm.rag.required-scope`) — mismo patrón M2M
que `TutorGatewayAuthorization`/`GoldenSetAuthorization`, con scope propio (`llm.rag.query`) para
no acoplar permisos de RAG (EP-09) a los del tutor (EP-05).

## Tests

`TextChunkerTest` (fronteras, solapamiento, descarte de fragmentos cortos),
`FakeEmbeddingAdapterTest` (determinismo, 768 dimensiones), `EmbeddingInvocationServiceTest`
(timeout, validación de dimensión, tolerancia a nulls en lote), `RagIngestionServiceTest`
(validaciones de subida, indexado, diagramas `DESCONOCIDO` no se indexan, retiro de una fuente
inexistente falla). Sin test de integración contra Postgres/pgvector real en esta pasada (ver
`docs/estado-implementacion/ep-09/README.md`).
