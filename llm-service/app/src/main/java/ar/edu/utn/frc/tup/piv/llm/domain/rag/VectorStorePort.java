package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;
import java.util.List;
import java.util.UUID;

/** Persistencia y búsqueda por similitud de {@link DocumentChunk}. Aislado detrás de un puerto
 * porque el motor concreto (pgvector hoy) es un detalle de infraestructura reemplazable, no una
 * regla de negocio — mismo principio que {@link ar.edu.utn.frc.tup.piv.llm.application.port.out.ModelInvocationPort}.
 * No conoce `RagDocument`: la metadata del documento vive en
 * `infrastructure/persistence/RagDocumentRepository` (persistencia simple, sin puerto, igual que
 * el resto de los repositorios JDBC del servicio). */
public interface VectorStorePort {
  /** Reemplaza (si existieran) los chunks de `documentId` por los nuevos, con su embedding. */
  void indexChunks(UUID documentId, List<DocumentChunk> chunks, List<EmbeddingResult> embeddings);

  /** Agrega un chunk individual (ej. un diagrama decodificado) sin tocar los demás. */
  void addChunk(DocumentChunk chunk, EmbeddingResult embedding);

  /** Búsqueda semántica cruzada multi-documento. El aislamiento por cohorte (`AGENTS.md` §2) y el
   * retiro lógico (`active`) se resuelven **dentro de la consulta**, no filtrando en memoria: se
   * devuelven solo chunks de documentos activos de `courseCohortId` que además estén en
   * `documentIds`. Que el llamador ya haya validado la selección no exime a la base de filtrar —
   * es defensa en profundidad: un llamador equivocado no puede recuperar material ajeno. */
  List<DocumentChunk> searchTopK(UUID courseCohortId, List<UUID> documentIds, float[] queryVector, int topK);

  List<DocumentChunk> getChunks(UUID documentId);

  void deleteChunks(UUID documentId);
}
