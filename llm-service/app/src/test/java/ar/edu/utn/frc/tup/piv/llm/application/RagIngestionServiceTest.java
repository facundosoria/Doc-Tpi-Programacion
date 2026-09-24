package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.service.EmbeddingInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RagIngestionService;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.TextChunker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DiagramDecodeResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DiagramDetectionPort;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ExtractedPage;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ExtractedPdf;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ImageDetection;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.InvalidPdfSourceException;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocumentNotFoundException;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.PdfTextExtractionPort;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocument;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.VectorStorePort;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RagDocumentRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RagIngestionServiceTest {
  private final CallerIdentity actor = new CallerIdentity("practice-service", UUID.randomUUID(), null, null);

  @Test
  void rejectsAFileWithoutCourseCohortId() {
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), mock(RagDocumentRepository.class), mock(EmbeddingInvocationService.class));

    assertThatThrownBy(() -> service.upload(null, "doc.pdf", new byte[] {1, 2, 3}, UUID.randomUUID(), actor))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void uploadExtractsChunksEmbedsAndIndexes() throws Exception {
    String pageText = "Contenido de la página uno, con longitud suficiente (más de 100 caracteres) para "
        + "que TextChunker no descarte el fragmento por ser demasiado corto.";
    var extractor = mock(PdfTextExtractionPort.class);
    when(extractor.extractTextWithPages(any())).thenReturn(new ExtractedPdf(1, List.of(new ExtractedPage(1, pageText)), pageText));
    var diagrams = mock(DiagramDetectionPort.class);
    when(diagrams.detectImages(any())).thenReturn(List.of());
    var vectorStore = mock(VectorStorePort.class);
    var documents = mock(RagDocumentRepository.class);
    when(documents.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    var embeddings = mock(EmbeddingInvocationService.class);
    when(embeddings.embedBatch(anyList(), any())).thenReturn(List.of(new EmbeddingResult(new float[768], "fake", "fake-embedding-768")));

    UUID courseCohortId = UUID.randomUUID();
    RagIngestionService service = buildService(extractor, diagrams, vectorStore, documents, embeddings);
    RagDocument result = service.upload(courseCohortId, "docker.pdf", fakePdf("contenido pdf simulado"),
        UUID.randomUUID(), actor);

    assertThat(result.fileName()).isEqualTo("docker.pdf");
    assertThat(result.courseCohortId()).isEqualTo(courseCohortId);
    assertThat(result.pageCount()).isEqualTo(1);
    assertThat(result.chunkCount()).isEqualTo(1);
    assertThat(result.previewText()).isNotBlank();
    assertThat(result.active()).isTrue();
    verify(vectorStore).indexChunks(any(), anyList(), anyList());
  }

  @Test
  void uploadPersistsTheDocumentBeforeIndexingItsChunks() throws Exception {
    String pageText = "Contenido de la página uno, con longitud suficiente (más de 100 caracteres) para "
        + "que TextChunker no descarte el fragmento por ser demasiado corto.";
    PdfTextExtractionPort extractor = mock(PdfTextExtractionPort.class);
    when(extractor.extractTextWithPages(any())).thenReturn(new ExtractedPdf(1, List.of(new ExtractedPage(1, pageText)), pageText));
    DiagramDetectionPort diagrams = mock(DiagramDetectionPort.class);
    when(diagrams.detectImages(any())).thenReturn(List.of());
    VectorStorePort vectorStore = mock(VectorStorePort.class);
    RagDocumentRepository documents = mock(RagDocumentRepository.class);
    when(documents.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    EmbeddingInvocationService embeddings = mock(EmbeddingInvocationService.class);
    when(embeddings.embedBatch(anyList(), any())).thenReturn(List.of(new EmbeddingResult(new float[768], "fake", "fake-embedding-768")));

    RagIngestionService service = buildService(extractor, diagrams, vectorStore, documents, embeddings);
    service.upload(UUID.randomUUID(), "docker.pdf", fakePdf("contenido pdf simulado"), UUID.randomUUID(), actor);

    org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(documents, vectorStore);
    inOrder.verify(documents).save(any(), any());
    inOrder.verify(vectorStore).indexChunks(any(), anyList(), anyList());
  }

  @Test
  void uploadDoesNotAbortIndexingWhenAnIndividualChunkEmbeddingIsNull() throws Exception {
    String pageText = "Contenido de la página uno, con longitud suficiente (más de 100 caracteres) para "
        + "que TextChunker no descarte el fragmento por ser demasiado corto.";
    PdfTextExtractionPort extractor = mock(PdfTextExtractionPort.class);
    when(extractor.extractTextWithPages(any()))
        .thenReturn(new ExtractedPdf(2, List.of(new ExtractedPage(1, pageText), new ExtractedPage(2, pageText)), pageText));
    DiagramDetectionPort diagrams = mock(DiagramDetectionPort.class);
    when(diagrams.detectImages(any())).thenReturn(List.of());
    VectorStorePort vectorStore = mock(VectorStorePort.class);
    RagDocumentRepository documents = mock(RagDocumentRepository.class);
    when(documents.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    EmbeddingInvocationService embeddings = mock(EmbeddingInvocationService.class);
    when(embeddings.embedBatch(anyList(), any())).thenReturn(List.of(
        new EmbeddingResult(new float[768], "fake", "fake-embedding-768"),
        new EmbeddingResult(null, "fake", "fake-embedding-768")));

    RagIngestionService service = buildService(extractor, diagrams, vectorStore, documents, embeddings);
    RagDocument result = service.upload(UUID.randomUUID(), "docker.pdf", fakePdf("contenido pdf simulado"), UUID.randomUUID(), actor);

    // Un vector nulo en un chunk individual no aborta el resto de la indexación.
    assertThat(result.chunkCount()).isEqualTo(2);
    verify(vectorStore).indexChunks(any(), anyList(), anyList());
    verify(documents).save(any(), any());
  }

  @Test
  void uploadReplaysTheStoredDocumentWhenTheSameIdempotencyKeyIsReused() throws Exception {
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    RagDocument stored = sampleDocument(UUID.randomUUID());
    IdempotencyRepository idempotency = mock(IdempotencyRepository.class);
    when(idempotency.replay(anyString(), any(), any(), anyString()))
        .thenReturn(Optional.of(mapper.valueToTree(stored)));
    RagIngestionService service = new RagIngestionService(mock(PdfTextExtractionPort.class),
        mock(DiagramDetectionPort.class), mock(VectorStorePort.class), mock(RagDocumentRepository.class),
        mock(EmbeddingInvocationService.class), idempotency, mapper, 26_214_400L, 8000L);

    RagDocument result = service.upload(UUID.randomUUID(), "docker.pdf", fakePdf("bytes"), UUID.randomUUID(), actor);

    assertThat(result.fileName()).isEqualTo(stored.fileName());
    assertThat(result.pageCount()).isEqualTo(stored.pageCount());
    assertThat(result.chunkCount()).isEqualTo(stored.chunkCount());
    assertThat(result.previewText()).isEqualTo(stored.previewText());
    assertThat(result.uploadedAt().toInstant()).isEqualTo(stored.uploadedAt().toInstant());
    verify(idempotency, never()).complete(anyString(), any(), any(), any(), any());
  }

  @Test
  void uploadCompletesTheIdempotencyRecordForANewDocument() throws Exception {
    String pageText = "Contenido de la página uno, con longitud suficiente (más de 100 caracteres) para "
        + "que TextChunker no descarte el fragmento por ser demasiado corto.";
    PdfTextExtractionPort extractor = mock(PdfTextExtractionPort.class);
    when(extractor.extractTextWithPages(any())).thenReturn(new ExtractedPdf(1, List.of(new ExtractedPage(1, pageText)), pageText));
    DiagramDetectionPort diagrams = mock(DiagramDetectionPort.class);
    when(diagrams.detectImages(any())).thenReturn(List.of());
    VectorStorePort vectorStore = mock(VectorStorePort.class);
    RagDocumentRepository documents = mock(RagDocumentRepository.class);
    when(documents.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    EmbeddingInvocationService embeddings = mock(EmbeddingInvocationService.class);
    when(embeddings.embedBatch(anyList(), any())).thenReturn(List.of(new EmbeddingResult(new float[768], "fake", "fake-embedding-768")));
    IdempotencyRepository idempotency = mock(IdempotencyRepository.class);
    when(idempotency.replay(anyString(), any(), any(), anyString())).thenReturn(Optional.empty());
    RagIngestionService service = new RagIngestionService(extractor, diagrams, vectorStore, documents, embeddings,
        idempotency, new ObjectMapper().findAndRegisterModules(), 26_214_400L, 8000L);

    UUID idempotencyKey = UUID.randomUUID();
    service.upload(UUID.randomUUID(), "docker.pdf", fakePdf("contenido pdf simulado"), idempotencyKey, actor);

    verify(idempotency).complete(anyString(), org.mockito.ArgumentMatchers.eq(actor),
        org.mockito.ArgumentMatchers.eq(idempotencyKey), any(UUID.class), any(JsonNode.class));
  }

  @Test
  void diagramsMarkedAsUnknownAreNotIndexedAsChunks() throws Exception {
    var extractor = mock(PdfTextExtractionPort.class);
    when(extractor.extractTextWithPages(any())).thenReturn(new ExtractedPdf(1, List.of(), ""));
    var diagrams = mock(DiagramDetectionPort.class);
    when(diagrams.detectImages(any())).thenReturn(List.of(new ImageDetection(0, 1, 200, 200, "png", "data:...", "Figura 1")));
    when(diagrams.decodeDiagram(any(), org.mockito.ArgumentMatchers.eq(0))).thenReturn(DiagramDecodeResult.vacio(0));
    var vectorStore = mock(VectorStorePort.class);
    var documents = mock(RagDocumentRepository.class);
    when(documents.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    var embeddings = mock(EmbeddingInvocationService.class);
    when(embeddings.embedBatch(anyList(), any())).thenReturn(List.of());

    var service = buildService(extractor, diagrams, vectorStore, documents, embeddings);
    RagDocument result = service.upload(UUID.randomUUID(), "sin-figuras.pdf", fakePdf("bytes"), UUID.randomUUID(), actor);

    assertThat(result.chunkCount()).isZero();
  }

  
  @Test
  void getPdfBytesIsEmptyWhenTheDocumentDoesNotExist() {
    var documents = mock(RagDocumentRepository.class);
    when(documents.findById(any())).thenReturn(Optional.empty());
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), documents, mock(EmbeddingInvocationService.class));

    assertThat(service.getPdfBytes(UUID.randomUUID())).isEmpty();
  }

  @Test
  void getPdfBytesReturnsTheBytesWhenTheDocumentExists() {
    var documents = mock(RagDocumentRepository.class);
    UUID id = UUID.randomUUID();
    byte[] bytes = "pdf".getBytes();
    when(documents.findById(id)).thenReturn(Optional.of(sampleDocument(id)));
    when(documents.getPdfBytes(id)).thenReturn(bytes);
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), documents, mock(EmbeddingInvocationService.class));

    assertThat(service.getPdfBytes(id)).contains(bytes);
  }

  @Test
  void getPdfBytesIsEmptyWhenTheStoredBytesAreNull() {
    // CA3/robustez: un pdf_bytes NULL en la fila no debe provocar un NPE en Optional.map.
    var documents = mock(RagDocumentRepository.class);
    UUID id = UUID.randomUUID();
    when(documents.findById(id)).thenReturn(Optional.of(sampleDocument(id)));
    when(documents.getPdfBytes(id)).thenReturn(null);
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), documents, mock(EmbeddingInvocationService.class));

    assertThat(service.getPdfBytes(id)).isEmpty();
  }

  @Test
  void uploadRejectsANonPdfFileWithUnprocessableEntity() throws Exception {
    var extractor = mock(PdfTextExtractionPort.class);
    when(extractor.extractTextWithPages(any())).thenThrow(new IOException("Not a PDF"));
    var service = buildService(extractor, mock(DiagramDetectionPort.class), mock(VectorStorePort.class),
        mock(RagDocumentRepository.class), mock(EmbeddingInvocationService.class));

    // Un archivo sin la firma %PDF- lo rechaza PdfUploadValidator antes de llegar al extractor
    // (#669); InvalidPdfSourceException tambien se mapea a 422 en ApiExceptionHandler.
    assertThatThrownBy(() -> service.upload(UUID.randomUUID(), "fake.pdf", "no soy un pdf".getBytes(),
        UUID.randomUUID(), actor))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("no es un PDF");

    // Un archivo que SI tiene la firma pero que PDFBox no puede leer sigue cortando en 422.
    assertThatThrownBy(() -> service.upload(UUID.randomUUID(), "roto.pdf", fakePdf("cuerpo corrupto"),
        UUID.randomUUID(), actor))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(422));
  }

  
  @Test
  void listDelegatesToTheActiveDocumentsOfTheCourse() {
    var documents = mock(RagDocumentRepository.class);
    UUID courseCohortId = UUID.randomUUID();
    var expected = List.of(sampleDocument(UUID.randomUUID()));
    when(documents.findActiveByCourse(courseCohortId)).thenReturn(expected);
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), documents, mock(EmbeddingInvocationService.class));

    assertThat(service.list(courseCohortId)).isEqualTo(expected);
  }

  @Test
  void getReturnsTheDocumentWhenItExists() {
    var documents = mock(RagDocumentRepository.class);
    UUID id = UUID.randomUUID();
    var expected = sampleDocument(id);
    when(documents.findById(id)).thenReturn(Optional.of(expected));
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), documents, mock(EmbeddingInvocationService.class));

    assertThat(service.get(id)).contains(expected);
  }

  @Test
  void detectImagesDelegatesToTheDiagramDetectionPort() {
    var diagrams = mock(DiagramDetectionPort.class);
    byte[] bytes = "pdf".getBytes();
    var expected = List.of(new ImageDetection(0, 1, 200, 200, "png", "data:...", "Figura 1"));
    when(diagrams.detectImages(bytes)).thenReturn(expected);
    var service = buildService(mock(PdfTextExtractionPort.class), diagrams,
        mock(VectorStorePort.class), mock(RagDocumentRepository.class), mock(EmbeddingInvocationService.class));

    assertThat(service.detectImages(bytes)).isEqualTo(expected);
  }

  @Test
  void decodeImageDelegatesToTheDiagramDetectionPort() {
    var diagrams = mock(DiagramDetectionPort.class);
    byte[] bytes = "pdf".getBytes();
    var expected = DiagramDecodeResult.vacio(0);
    when(diagrams.decodeDiagram(bytes, 0)).thenReturn(expected);
    var service = buildService(mock(PdfTextExtractionPort.class), diagrams,
        mock(VectorStorePort.class), mock(RagDocumentRepository.class), mock(EmbeddingInvocationService.class));

    assertThat(service.decodeImage(bytes, 0)).isEqualTo(expected);
  }

  @Test
  void indexDiagramThrowsWhenTheDocumentDoesNotExist() {
    var documents = mock(RagDocumentRepository.class);
    when(documents.findById(any())).thenReturn(Optional.empty());
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), documents, mock(EmbeddingInvocationService.class));

    assertThatThrownBy(() -> service.indexDiagram(UUID.randomUUID(), DiagramDecodeResult.vacio(0)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void indexDiagramAddsAChunkWithItsEmbeddingForAnExistingDocument() {
    var documents = mock(RagDocumentRepository.class);
    UUID id = UUID.randomUUID();
    when(documents.findById(id)).thenReturn(Optional.of(sampleDocument(id)));
    var vectorStore = mock(VectorStorePort.class);
    var embeddings = mock(EmbeddingInvocationService.class);
    var embedding = new EmbeddingResult(new float[768], "fake", "fake-embedding-768");
    when(embeddings.embed(any(), any())).thenReturn(embedding);
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class), vectorStore, documents, embeddings);
    var result = new DiagramDecodeResult(0, 4, "Figura 1", "DIAGRAMA_DOCUMENTO", "interpretación", "graph TD\n  A", List.of());

    service.indexDiagram(id, result);

    verify(vectorStore).addChunk(any(), org.mockito.ArgumentMatchers.eq(embedding));
  }

  @Test
  void indexDiagramStillAddsTheChunkWhenTheEmbeddingFails() {
    var documents = mock(RagDocumentRepository.class);
    UUID id = UUID.randomUUID();
    when(documents.findById(id)).thenReturn(Optional.of(sampleDocument(id)));
    var vectorStore = mock(VectorStorePort.class);
    var embeddings = mock(EmbeddingInvocationService.class);
    when(embeddings.embed(any(), any())).thenThrow(new RuntimeException("adaptador caído"));
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class), vectorStore, documents, embeddings);
    var result = new DiagramDecodeResult(0, 4, "Figura 1", "DIAGRAMA_DOCUMENTO", "interpretación", "graph TD\n  A", List.of());

    service.indexDiagram(id, result);

    verify(vectorStore).addChunk(any(), org.mockito.ArgumentMatchers.isNull());
  }

  @Test
  void uploadSampleLoadsTheSampleDocument() throws Exception {
    String pageText = "Contenido de la página uno con longitud suficiente para que TextChunker genere un chunk.";
    var extractor = mock(PdfTextExtractionPort.class);
    when(extractor.extractTextWithPages(any()))
        .thenReturn(new ExtractedPdf(1, List.of(new ExtractedPage(1, pageText)), pageText));
    var detector = mock(DiagramDetectionPort.class);
    when(detector.detectImages(any())).thenReturn(List.of());
    var vectorStore = mock(VectorStorePort.class);
    var repository = mock(RagDocumentRepository.class);
    when(repository.save(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    var embeddings = mock(EmbeddingInvocationService.class);
    when(embeddings.embedBatch(anyList(), any())).thenReturn(List.of(new EmbeddingResult(new float[768], "fake", "fake-embedding-768")));

    var service = buildService(extractor, detector, vectorStore, repository, embeddings);
    UUID courseCohortId = UUID.randomUUID();

    RagDocument doc = service.uploadSample(courseCohortId, UUID.randomUUID(), actor);

    assertThat(doc).isNotNull();
    assertThat(doc.fileName()).isEqualTo("PRD-Plataforma-Gamificada-TP.pdf");
    assertThat(doc.courseCohortId()).isEqualTo(courseCohortId);
    verify(repository).save(any(), any());
    verify(vectorStore).indexChunks(any(), anyList(), anyList());
  }

  @Test
  void retireThrowsNotFoundWhenTheDocumentDoesNotExist() {
    var documents = mock(RagDocumentRepository.class);
    when(documents.findById(any())).thenReturn(Optional.empty());
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), documents, mock(EmbeddingInvocationService.class));
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> service.retire(id, UUID.randomUUID()))
        .isInstanceOf(RagDocumentNotFoundException.class)
        .hasMessageContaining(id.toString());
    verify(documents, never()).deactivate(any());
  }

  @Test
  void retireTreatsADocumentOfAnotherCohortAsNotFound() {
    var documents = mock(RagDocumentRepository.class);
    UUID id = UUID.randomUUID();
    when(documents.findById(id)).thenReturn(Optional.of(sampleDocument(id)));
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), documents, mock(EmbeddingInvocationService.class));

    // Misma excepción que "no existe": no se filtra la existencia de material de otra cohorte.
    assertThatThrownBy(() -> service.retire(id, UUID.randomUUID())).isInstanceOf(RagDocumentNotFoundException.class);
    verify(documents, never()).deactivate(any());
  }

  @Test
  void retireIsIdempotentForAnAlreadyRetiredDocument() {
    var documents = mock(RagDocumentRepository.class);
    UUID id = UUID.randomUUID();
    RagDocument alreadyRetired = sampleDocument(id).retire();
    when(documents.findById(id)).thenReturn(Optional.of(alreadyRetired));
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), documents, mock(EmbeddingInvocationService.class));

    RagDocument result = service.retire(id, alreadyRetired.courseCohortId());

    assertThat(result.active()).isFalse();
    verify(documents, never()).deactivate(any());
  }

  @Test
  void retireMarksAnExistingDocumentOfTheCohortAsInactiveWithoutDeletingAnything() {
    var documents = mock(RagDocumentRepository.class);
    var vectorStore = mock(VectorStorePort.class);
    UUID id = UUID.randomUUID();
    RagDocument document = sampleDocument(id);
    when(documents.findById(id)).thenReturn(Optional.of(document));
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        vectorStore, documents, mock(EmbeddingInvocationService.class));

    RagDocument result = service.retire(id, document.courseCohortId());

    assertThat(result.active()).isFalse();
    assertThat(result.id()).isEqualTo(id);
    verify(documents).deactivate(id);
    verify(vectorStore, never()).deleteChunks(any());
  }

/** #669 — un archivo rechazado no deja rastro: ni fila en `rag_documents`, ni chunks, ni
   * `pdf_bytes` a medio guardar. La validación corre antes de tocar la base. */
  @Test
  void aRejectedUploadPersistsNothing() {
    var documents = mock(RagDocumentRepository.class);
    var vectorStore = mock(VectorStorePort.class);
    var extractor = mock(PdfTextExtractionPort.class);
    var idempotency = mock(IdempotencyRepository.class);
    var service = new RagIngestionService(extractor, mock(DiagramDetectionPort.class), vectorStore, documents,
        mock(EmbeddingInvocationService.class), idempotency, new ObjectMapper().findAndRegisterModules(), 26_214_400L, 8000L);
    // Un .docx renombrado: firma ZIP "PK\003\004", no "%PDF-".
    byte[] docx = new byte[] {0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00};

    assertThatThrownBy(() -> service.upload(UUID.randomUUID(), "trabajo.pdf", docx, UUID.randomUUID(), actor))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("no es un PDF");

    verify(documents, never()).save(any(), any());
    verify(vectorStore, never()).indexChunks(any(), anyList(), anyList());
    verify(vectorStore, never()).addChunk(any(), any());
    // Ni siquiera se intenta extraer texto ni se reserva una clave de idempotencia.
    verifyNoInteractions(extractor);
    verifyNoInteractions(idempotency);
  }

  @Test
  void anUploadOverTheSizeLimitIsRejectedBeforeReadingTheFile() {
    var documents = mock(RagDocumentRepository.class);
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), documents, mock(EmbeddingInvocationService.class));

    assertThatThrownBy(() -> service.validateDeclaredUploadSize(26_214_401L, "enorme.pdf"))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("supera el máximo permitido");
    verify(documents, never()).save(any(), any());
  }

  @Test
  void anEmptyUploadIsRejectedWithItsOwnMessage() {
    var documents = mock(RagDocumentRepository.class);
    var service = buildService(mock(PdfTextExtractionPort.class), mock(DiagramDetectionPort.class),
        mock(VectorStorePort.class), documents, mock(EmbeddingInvocationService.class));

    assertThatThrownBy(() -> service.upload(UUID.randomUUID(), "vacio.pdf", new byte[0], UUID.randomUUID(), actor))
        .isInstanceOf(InvalidPdfSourceException.class)
        .hasMessageContaining("vacío");
    verify(documents, never()).save(any(), any());
  }

  private RagDocument sampleDocument(UUID id) {
    return new RagDocument(id, UUID.randomUUID(), "docker.pdf", 1000, 5, 3, OffsetDateTime.now(), "preview", true);
  }

  private RagIngestionService buildService(PdfTextExtractionPort extractor, DiagramDetectionPort diagrams,
      VectorStorePort vectorStore, RagDocumentRepository documents, EmbeddingInvocationService embeddings) {
    IdempotencyRepository idempotency = mock(IdempotencyRepository.class);
    when(idempotency.replay(anyString(), any(), any(), anyString())).thenReturn(Optional.empty());
    return new RagIngestionService(extractor, diagrams, vectorStore, documents, embeddings, idempotency,
        new ObjectMapper().findAndRegisterModules(), 26_214_400L, 8000L);
  }

  /** #669 — la ingesta valida la firma binaria, así que un doble de PDF tiene que empezar con
   * `%PDF-` aunque el extractor esté mockeado. */
  private static byte[] fakePdf(String contenido) {
    return ("%PDF-1.7\n" + contenido).getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

}
