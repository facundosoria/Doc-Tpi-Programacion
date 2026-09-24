package ar.edu.utn.frc.tup.piv.llm.application.service.agent;

import ar.edu.utn.frc.tup.piv.llm.application.service.EmbeddingInvocationService;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DocumentChunk;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocument;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.VectorStorePort;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RagDocumentRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servicio de consulta RAG con aislamiento estricto por cohorte (HU LLM-S18-H01 · T2 / T3).
 */
@Service
public class RagQueryService {

  private static final Logger log = LoggerFactory.getLogger(RagQueryService.class);
  private static final double DEFAULT_MIN_SCORE = 0.30;
  private static final int DEFAULT_TOP_K = 4;

  private final RagDocumentRepository documents;
  private final EmbeddingInvocationService embeddings;
  private final VectorStorePort vectorStore;
  private final Duration timeout;
  private final double minScoreThreshold;

  @Autowired
  public RagQueryService(
      RagDocumentRepository documents,
      EmbeddingInvocationService embeddings,
      VectorStorePort vectorStore,
      @Value("${llm.rag.embedding-timeout-ms:8000}") long timeoutMs,
      @Value("${llm.rag.min-score-threshold:0.30}") double minScoreThreshold) {
    this.documents = documents;
    this.embeddings = embeddings;
    this.vectorStore = vectorStore;
    this.timeout = Duration.ofMillis(timeoutMs);
    this.minScoreThreshold = minScoreThreshold;
  }

  public RagQueryService(
      RagDocumentRepository documents,
      EmbeddingInvocationService embeddings,
      VectorStorePort vectorStore) {
    this(documents, embeddings, vectorStore, 8000L, DEFAULT_MIN_SCORE);
  }

  /**
   * Recupera fragmentos de documentos activos pertenecientes exclusivamente a la cohorte indicada.
   * Si no hay documentos o los fragmentos no superan el umbral de similitud, retorna lista vacía.
   */
  public List<DocumentChunk> queryCohortContext(UUID cohortId, String queryText, int topK) {
    if (cohortId == null || queryText == null || queryText.isBlank()) {
      return List.of();
    }

    // 1. Filtrar estrictamente documentos activos de la cohorte
    List<RagDocument> activeDocs = documents.findActiveByCourse(cohortId);
    if (activeDocs.isEmpty()) {
      log.debug("No hay documentos RAG activos para la cohorte '{}'", cohortId);
      return List.of();
    }

    List<UUID> authorizedDocIds = activeDocs.stream().map(RagDocument::id).toList();

    // 2. Vectorizar la consulta
    float[] queryVector;
    try {
      EmbeddingResult embeddingResult = embeddings.embed(queryText, timeout);
      queryVector = embeddingResult.vector();
    } catch (Exception e) {
      log.warn("Falla al generar embedding para consulta de cohorte '{}': {}", cohortId, e.getMessage());
      return List.of();
    }

    // 3. Búsqueda vectorial multi-documento en el espacio de la cohorte
    int k = topK > 0 ? topK : DEFAULT_TOP_K;
    List<DocumentChunk> chunks = vectorStore.searchTopK(cohortId, authorizedDocIds, queryVector, k);
    if (chunks == null || chunks.isEmpty()) {
      return List.of();
    }

    // 4. Filtrar por umbral de suficiencia de respaldo
    List<DocumentChunk> sufficientChunks = chunks.stream()
        .filter(c -> c.similarityScore() >= minScoreThreshold)
        .toList();

    log.debug("Recuperados {} fragmentos con score >= {} para cohorte '{}'",
        sufficientChunks.size(), minScoreThreshold, cohortId);

    return sufficientChunks;
  }
}
