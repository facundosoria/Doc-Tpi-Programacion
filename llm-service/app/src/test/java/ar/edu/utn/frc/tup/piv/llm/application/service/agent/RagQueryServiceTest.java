package ar.edu.utn.frc.tup.piv.llm.application.service.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.service.EmbeddingInvocationService;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DocumentChunk;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocument;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.VectorStorePort;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RagDocumentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** #675 — recuperación de contexto de la cohorte para el agente de menciones: solo documentos
 * activos de esa cohorte, con el filtro empujado hasta la consulta. */
class RagQueryServiceTest {

  private final UUID cohort = UUID.randomUUID();

  private RagDocument activeDocument(UUID id) {
    return new RagDocument(id, cohort, "material.pdf", 1000, 10, 5, OffsetDateTime.now(), "preview", true);
  }

  private EmbeddingInvocationService embeddingsReturning768() {
    var embeddings = mock(EmbeddingInvocationService.class);
    when(embeddings.embed(anyString(), any())).thenReturn(new EmbeddingResult(new float[768], "fake", "fake-embedding-768"));
    return embeddings;
  }

  @Test
  void queriesOnlyTheActiveDocumentsOfTheCohortAndPushesTheCohortIntoTheSearch() {
    UUID docId = UUID.randomUUID();
    var documents = mock(RagDocumentRepository.class);
    when(documents.findActiveByCourse(cohort)).thenReturn(List.of(activeDocument(docId)));
    var vectorStore = mock(VectorStorePort.class);
    when(vectorStore.searchTopK(any(), any(), any(), anyInt())).thenReturn(List.of(
        new DocumentChunk(UUID.randomUUID(), docId, "material.pdf", 3, 0, "fragmento relevante", 0.91)));

    var service = new RagQueryService(documents, embeddingsReturning768(), vectorStore);
    List<DocumentChunk> chunks = service.queryCohortContext(cohort, "requerimientos del producto", 3);

    assertThat(chunks).hasSize(1);
    verify(vectorStore).searchTopK(eq(cohort), eq(List.of(docId)), any(), eq(3));
  }

  @Test
  void aCohortWithoutActiveDocumentsNeverReachesTheSearch() {
    var documents = mock(RagDocumentRepository.class);
    // Una cohorte cuyas fuentes fueron todas retiradas (#672) queda sin documentos activos.
    when(documents.findActiveByCourse(cohort)).thenReturn(List.of());
    var vectorStore = mock(VectorStorePort.class);

    var service = new RagQueryService(documents, embeddingsReturning768(), vectorStore);

    assertThat(service.queryCohortContext(cohort, "requerimientos del producto", 3)).isEmpty();
    verify(vectorStore, never()).searchTopK(any(), any(), any(), anyInt());
  }

  @Test
  void chunksBelowTheSufficiencyThresholdAreDiscarded() {
    UUID docId = UUID.randomUUID();
    var documents = mock(RagDocumentRepository.class);
    when(documents.findActiveByCourse(cohort)).thenReturn(List.of(activeDocument(docId)));
    var vectorStore = mock(VectorStorePort.class);
    when(vectorStore.searchTopK(any(), any(), any(), anyInt())).thenReturn(List.of(
        new DocumentChunk(UUID.randomUUID(), docId, "material.pdf", 3, 0, "apenas relacionado", 0.10)));

    var service = new RagQueryService(documents, embeddingsReturning768(), vectorStore);

    assertThat(service.queryCohortContext(cohort, "requerimientos del producto", 3)).isEmpty();
  }

  @Test
  void anAbsentCohortOrEmptyQuestionIsAnEmptyResultWithoutTouchingAnyDependency() {
    var documents = mock(RagDocumentRepository.class);
    var vectorStore = mock(VectorStorePort.class);
    var service = new RagQueryService(documents, embeddingsReturning768(), vectorStore);

    assertThat(service.queryCohortContext(null, "una pregunta", 3)).isEmpty();
    assertThat(service.queryCohortContext(cohort, "   ", 3)).isEmpty();
    verify(documents, never()).findActiveByCourse(any());
    verify(vectorStore, never()).searchTopK(any(), any(), any(), anyInt());
  }
}
