package ar.edu.utn.frc.tup.piv.llm.domain.rag;

import java.util.UUID;

/** Un fragmento semántico indexado de un {@link RagDocument}. Portado de
 * `demoLLMSpringAi/.../rag/model/DocumentChunk.java` — `similarityScore` solo es significativo en
 * resultados de búsqueda (0 en un chunk recién creado, todavía no buscado). */
public record DocumentChunk(
    UUID id, UUID documentId, String documentName, int pageNumber, int chunkIndex, String content, double similarityScore) {

  public static DocumentChunk nuevo(UUID documentId, String documentName, int pageNumber, int chunkIndex, String content) {
    return new DocumentChunk(UUID.randomUUID(), documentId, documentName, pageNumber, chunkIndex, content, 0.0);
  }

  public DocumentChunk conSimilitud(double score) {
    return new DocumentChunk(id, documentId, documentName, pageNumber, chunkIndex, content, score);
  }
}
