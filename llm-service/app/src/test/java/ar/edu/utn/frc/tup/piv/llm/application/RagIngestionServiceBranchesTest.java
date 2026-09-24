package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.service.EmbeddingInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RagIngestionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DiagramDecodeResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DiagramDetectionPort;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DocumentChunk;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ExtractedPage;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ExtractedPdf;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ImageDetection;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.PdfTextExtractionPort;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocument;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.VectorStorePort;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RagDocumentRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RagIngestionServiceBranchesTest {
  private final PdfTextExtractionPort extractor = mock(PdfTextExtractionPort.class);
  private final DiagramDetectionPort diagrams = mock(DiagramDetectionPort.class);
  private final VectorStorePort vectorStore = mock(VectorStorePort.class);
  private final RagDocumentRepository documents = mock(RagDocumentRepository.class);
  private final EmbeddingInvocationService embeddings = mock(EmbeddingInvocationService.class);
  private final IdempotencyRepository idempotency = mock(IdempotencyRepository.class);
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private final CallerIdentity actor = new CallerIdentity("practice-service", UUID.randomUUID(), null, null);
  private final RagIngestionService service =
      new RagIngestionService(extractor, diagrams, vectorStore, documents, embeddings, idempotency, mapper, 1_000_000L, 5000L);

  private static final String LONG_TEXT = "Docker es una plataforma de contenedores. ".repeat(20);

  private void stubPdf(String text) throws Exception {
    when(extractor.extractTextWithPages(any()))
        .thenReturn(new ExtractedPdf(1, List.of(new ExtractedPage(1, text)), text));
    when(documents.save(any(), any())).thenAnswer(i -> i.getArgument(0));
    when(embeddings.embedBatch(anyList(), any())).thenReturn(List.of());
  }

  @Test
  void longTextProducesATruncatedPreviewEndingInEllipsis() throws Exception {
    stubPdf(LONG_TEXT);
    when(diagrams.detectImages(any())).thenReturn(List.of());

    RagDocument doc = service.upload(UUID.randomUUID(), "GRANDE.PDF", fakePdf("x"), UUID.randomUUID(), actor);

    assertThat(doc.previewText()).hasSize(253).endsWith("...");
    assertThat(doc.chunkCount()).isGreaterThan(0);
    assertThat(doc.pageCount()).isEqualTo(1);
    assertThat(doc.active()).isTrue();
  }

  @Test
  void recognizedDiagramsAreAppendedAsExtraChunksWithMermaid() throws Exception {
    stubPdf("corto");
    when(diagrams.detectImages(any())).thenReturn(List.of(
        new ImageDetection(0, 2, 100, 100, "png", "d", "F1"), new ImageDetection(1, 2, 100, 100, "png", "d", "F2")));
    when(diagrams.decodeDiagram(any(), eq(0))).thenReturn(
        new DiagramDecodeResult(0, 2, "Arquitectura", "DIAGRAMA_DOCUMENTO", null, null, List.of()));
    when(diagrams.decodeDiagram(any(), eq(1))).thenReturn(DiagramDecodeResult.vacio(1));

    RagDocument doc = service.upload(UUID.randomUUID(), "d.pdf", fakePdf("x"), UUID.randomUUID(), actor);

    assertThat(doc.chunkCount()).isEqualTo(1);
    @SuppressWarnings("unchecked") ArgumentCaptor<List<DocumentChunk>> captor = ArgumentCaptor.forClass(List.class);
    verify(vectorStore).indexChunks(any(), captor.capture(), anyList());
    assertThat(captor.getValue().get(0).content())
        .contains("Figura/Diagrama: Arquitectura").contains("Tipo: DIAGRAMA_DOCUMENTO").contains("```mermaid");
    assertThat(captor.getValue().get(0).pageNumber()).isEqualTo(2);
  }

  @Test
  void diagramDetectionFailureDoesNotAbortTheUpload() throws Exception {
    stubPdf(LONG_TEXT);
    when(diagrams.detectImages(any())).thenThrow(new IllegalStateException("boom"));

    RagDocument doc = service.upload(UUID.randomUUID(), "d.pdf", fakePdf("x"), UUID.randomUUID(), actor);

    assertThat(doc.chunkCount()).isGreaterThan(0);
    verify(vectorStore).indexChunks(any(), anyList(), anyList());
  }

  @Test
  void indexingFailureDeactivatesTheSavedDocumentAndRethrows() throws Exception {
    stubPdf(LONG_TEXT);
    when(diagrams.detectImages(any())).thenReturn(List.of());
    doThrow(new IllegalStateException("vector store caído")).when(vectorStore).indexChunks(any(), anyList(), anyList());

    assertThatThrownBy(() -> service.upload(UUID.randomUUID(), "d.pdf", fakePdf("x"), UUID.randomUUID(), actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("vector store caído");

    ArgumentCaptor<UUID> id = ArgumentCaptor.forClass(UUID.class);
    verify(documents).deactivate(id.capture());
    ArgumentCaptor<RagDocument> saved = ArgumentCaptor.forClass(RagDocument.class);
    verify(documents).save(saved.capture(), any());
    assertThat(id.getValue()).isEqualTo(saved.getValue().id());
  }

  @Test
  void getChunksDelegatesToTheVectorStore() {
    UUID id = UUID.randomUUID();
    var chunk = DocumentChunk.nuevo(id, "a.pdf", 1, 0, "texto");
    when(vectorStore.getChunks(id)).thenReturn(List.of(chunk));
    assertThat(service.getChunks(id)).containsExactly(chunk);
  }

  @Test
  void indexDiagramUsesNextChunkIndexAndEmptyInterpretationWhenMissing() {
    UUID id = UUID.randomUUID();
    var doc = new RagDocument(id, UUID.randomUUID(), "m.pdf", 10, 2, 3, OffsetDateTime.now(), "p", true);
    when(documents.findById(id)).thenReturn(Optional.of(doc));
    var emb = new EmbeddingResult(new float[3], "fake", "m");
    when(embeddings.embed(any(), any())).thenReturn(emb);

    service.indexDiagram(id, new DiagramDecodeResult(0, 7, "T", "FLUJO", null, null, List.of()));

    ArgumentCaptor<DocumentChunk> chunk = ArgumentCaptor.forClass(DocumentChunk.class);
    verify(vectorStore).addChunk(chunk.capture(), eq(emb));
    assertThat(chunk.getValue().chunkIndex()).isEqualTo(4);
    assertThat(chunk.getValue().pageNumber()).isEqualTo(7);
    assertThat(chunk.getValue().documentName()).isEqualTo("m.pdf");
    assertThat(chunk.getValue().content()).contains("Interpretación:").contains("Tipo: FLUJO");
  }

  /** #669 — la ingesta ahora valida la firma binaria, así que un doble de PDF tiene que empezar
   * con `%PDF-` aunque el extractor esté mockeado. */
  private static byte[] fakePdf(String contenido) {
    return ("%PDF-1.7\n" + contenido).getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

}
