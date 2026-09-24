package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.RagController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.service.RagChatService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RagIngestionService;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DiagramDecodeResult;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DocumentChunk;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.ImageDetection;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocument;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocumentNotFoundException;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.RagGatewayAuthorization;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class RagControllerTest {
  private final CallerIdentity actor = new CallerIdentity("practice-service", UUID.randomUUID(), null, null);

  @Test
  void authorizesAndAppliesTheTeacherCourseBeforeListingDocuments() {
    RagIngestionService ingestion = mock(RagIngestionService.class);
    RagGatewayAuthorization authorization = mock(RagGatewayAuthorization.class);
    CourseAuthorization courses = mock(CourseAuthorization.class);
    HttpHeaders headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID courseCohortId = UUID.randomUUID();
    List<RagDocument> expected = List.of(sampleDocument());
    when(ingestion.list(courseCohortId)).thenReturn(expected);
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, courses);

    List<RagDocument> result = controller.listDocuments(courseCohortId, headers);

    assertThat(result).isEqualTo(expected);
    InOrder order = Mockito.inOrder(authorization, courses, ingestion);
    order.verify(authorization).require(headers);
    order.verify(courses).requireTeacher(courseCohortId, actor, headers);
    order.verify(ingestion).list(courseCohortId);
  }

  @Test
  void authorizesAndUploadsADocument() throws Exception {
    RagIngestionService ingestion = mock(RagIngestionService.class);
    RagGatewayAuthorization authorization = mock(RagGatewayAuthorization.class);
    CourseAuthorization courses = mock(CourseAuthorization.class);
    HttpHeaders headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID courseCohortId = UUID.randomUUID();
    MockMultipartFile file = new MockMultipartFile("file", "docker.pdf", "application/pdf", "contenido".getBytes());
    RagDocument expected = sampleDocument();
    when(ingestion.upload(ArgumentMatchers.eq(courseCohortId), ArgumentMatchers.eq("docker.pdf"), any(byte[].class),
        any(UUID.class), ArgumentMatchers.eq(actor))).thenReturn(expected);
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, courses);

    ResponseEntity<RagDocument> response = controller.uploadDocument(courseCohortId, file, UUID.randomUUID(), headers);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isEqualTo(expected);
    InOrder order = Mockito.inOrder(authorization, courses, ingestion);
    order.verify(authorization).require(headers);
    order.verify(courses).requireTeacher(courseCohortId, actor, headers);
    order.verify(ingestion).upload(ArgumentMatchers.eq(courseCohortId), ArgumentMatchers.eq("docker.pdf"),
        any(byte[].class), any(UUID.class), ArgumentMatchers.eq(actor));
  }

  @Test
  void authorizesAndUploadsSampleDocument() throws Exception {
    RagIngestionService ingestion = mock(RagIngestionService.class);
    RagGatewayAuthorization authorization = mock(RagGatewayAuthorization.class);
    CourseAuthorization courses = mock(CourseAuthorization.class);
    HttpHeaders headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID courseCohortId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();
    RagDocument expected = sampleDocument();
    when(ingestion.uploadSample(courseCohortId, idempotencyKey, actor)).thenReturn(expected);
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, courses);

    ResponseEntity<RagDocument> response = controller.uploadSample(courseCohortId, idempotencyKey, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    assertThat(response.getBody()).isEqualTo(expected);
    verify(authorization).require(headers);
    verify(courses).requireTeacher(courseCohortId, actor, headers);
    verify(ingestion).uploadSample(courseCohortId, idempotencyKey, actor);
  }

  @Test
  void uploadReturnsTheFourAcceptedFieldsAssociatedToTheTeacherCourse() throws Exception {
    RagIngestionService ingestion = mock(RagIngestionService.class);
    RagGatewayAuthorization authorization = mock(RagGatewayAuthorization.class);
    CourseAuthorization courses = mock(CourseAuthorization.class);
    HttpHeaders headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID courseCohortId = UUID.randomUUID();
    MockMultipartFile file = new MockMultipartFile("file", "docker.pdf", "application/pdf", "contenido".getBytes());
    RagDocument expected = new RagDocument(UUID.randomUUID(), courseCohortId, "docker.pdf", 1000, 5, 3,
        OffsetDateTime.now(), "preview", true);
    when(ingestion.upload(ArgumentMatchers.eq(courseCohortId), ArgumentMatchers.eq("docker.pdf"), any(byte[].class),
        any(UUID.class), ArgumentMatchers.eq(actor))).thenReturn(expected);
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, courses);

    ResponseEntity<RagDocument> response = controller.uploadDocument(courseCohortId, file, UUID.randomUUID(), headers);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    RagDocument body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.courseCohortId()).isEqualTo(courseCohortId);
    assertThat(body.fileName()).isEqualTo("docker.pdf");
    assertThat(body.pageCount()).isEqualTo(5);
    assertThat(body.chunkCount()).isEqualTo(3);
    assertThat(body.previewText()).isEqualTo("preview");
    verify(courses).requireTeacher(courseCohortId, actor, headers);
  }

  @Test
  void authorizesTheTeacherOnTheDocumentCohortBeforeRetiringIt() {
    var ingestion = mock(RagIngestionService.class);
    var authorization = mock(RagGatewayAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    RagDocument document = sampleDocument();
    when(ingestion.get(document.id())).thenReturn(Optional.of(document));
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, courses);

    var response = controller.deleteDocument(document.id(), headers);

    assertThat(response.getStatusCode().value()).isEqualTo(204);
    var order = Mockito.inOrder(authorization, courses, ingestion);
    order.verify(authorization).require(headers);
    order.verify(courses).requireTeacher(document.courseCohortId(), actor, headers);
    order.verify(ingestion).retire(document.id(), document.courseCohortId());
  }

  @Test
  void retiringADocumentOfACohortTheActorDoesNotManageLooksLikeNotFound() {
    var ingestion = mock(RagIngestionService.class);
    var authorization = mock(RagGatewayAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    RagDocument document = sampleDocument();
    when(ingestion.get(document.id())).thenReturn(Optional.of(document));
    Mockito.doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "El actor no puede administrar este curso"))
        .when(courses).requireTeacher(document.courseCohortId(), actor, headers);
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, courses);

    // 404 y no 403: no se filtra la existencia de material de otra cohorte.
    assertThatThrownBy(() -> controller.deleteDocument(document.id(), headers))
        .isInstanceOf(RagDocumentNotFoundException.class);
    verify(ingestion, never()).retire(any(), any());
  }

  @Test
  void retiringAnUnknownDocumentIsNotFound() {
    var ingestion = mock(RagIngestionService.class);
    var authorization = mock(RagGatewayAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID id = UUID.randomUUID();
    when(ingestion.get(id)).thenReturn(Optional.empty());
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, courses);

    assertThatThrownBy(() -> controller.deleteDocument(id, headers))
        .isInstanceOf(RagDocumentNotFoundException.class)
        .hasMessageContaining(id.toString());
    verify(courses, never()).requireTeacher(any(UUID.class), any(CallerIdentity.class), any(HttpHeaders.class));
    verify(ingestion, never()).retire(any(), any());
  }

  @Test
  void deleteRejectsADocumentFromAnotherCourseAndDoesNotDeactivateIt() {
    var ingestion = mock(RagIngestionService.class);
    var authorization = mock(RagGatewayAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID id = UUID.randomUUID();
    when(ingestion.get(id)).thenReturn(Optional.of(document(id)));
    org.mockito.Mockito.doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN))
        .when(courses).requireTeacher(any(UUID.class), any(CallerIdentity.class), any(HttpHeaders.class));
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, courses);

    // Tras #672 el 403 de requireTeacher se traduce a 404 para no filtrar existencia, y el retiro
    // pasa por retire(id, cohorte) en vez de deactivate(id).
    assertThatThrownBy(() -> controller.deleteDocument(id, headers))
        .isInstanceOf(RagDocumentNotFoundException.class);
    verify(ingestion, never()).retire(any(), any());
  }

  @Test
  void documentScopedEndpointsReturn404WhenTheDocumentDoesNotExist() {
    var ingestion = mock(RagIngestionService.class);
    var authorization = mock(RagGatewayAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID id = UUID.randomUUID();
    when(ingestion.get(id)).thenReturn(Optional.empty());
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, mock(CourseAuthorization.class));

    assertThatThrownBy(() -> controller.chunks(id, headers)).isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> controller.images(id, headers)).isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> controller.decodeImage(id, 0, headers)).isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> controller.indexDiagram(id, DiagramDecodeResult.vacio(0), headers))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void returnsChunksOfADocument() {
    var ingestion = mock(RagIngestionService.class);
    var authorization = mock(RagGatewayAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID id = UUID.randomUUID();
    when(ingestion.get(id)).thenReturn(Optional.of(document(id)));
    var expected = List.of(new DocumentChunk(UUID.randomUUID(), id, "doc.pdf", 1, 0, "contenido", 0.0));
    when(ingestion.getChunks(id)).thenReturn(expected);
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, mock(CourseAuthorization.class));

    assertThat(controller.chunks(id, headers)).isEqualTo(expected);
  }

  @Test
  void returnsImagesWhenTheDocumentHasPdfBytes() {
    var ingestion = mock(RagIngestionService.class);
    var authorization = mock(RagGatewayAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID id = UUID.randomUUID();
    when(ingestion.get(id)).thenReturn(Optional.of(document(id)));
    byte[] bytes = "pdf".getBytes();
    when(ingestion.getPdfBytes(id)).thenReturn(Optional.of(bytes));
    var expected = List.of(new ImageDetection(0, 1, 200, 200, "png", "data:...", "Figura 1"));
    when(ingestion.detectImages(bytes)).thenReturn(expected);
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, mock(CourseAuthorization.class));

    assertThat(controller.images(id, headers)).isEqualTo(expected);
  }

  @Test
  void imagesReturns404WhenTheDocumentHasNoPdfBytes() {
    var ingestion = mock(RagIngestionService.class);
    var authorization = mock(RagGatewayAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID id = UUID.randomUUID();
    when(ingestion.get(id)).thenReturn(Optional.of(document(id)));
    when(ingestion.getPdfBytes(id)).thenReturn(Optional.empty());
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, mock(CourseAuthorization.class));

    assertThatThrownBy(() -> controller.images(id, headers))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404");
    verify(ingestion, never()).detectImages(any());
  }

  @Test
  void decodesAnImageWhenTheDocumentHasPdfBytes() {
    var ingestion = mock(RagIngestionService.class);
    var authorization = mock(RagGatewayAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID id = UUID.randomUUID();
    when(ingestion.get(id)).thenReturn(Optional.of(document(id)));
    byte[] bytes = "pdf".getBytes();
    when(ingestion.getPdfBytes(id)).thenReturn(Optional.of(bytes));
    var expected = DiagramDecodeResult.vacio(0);
    when(ingestion.decodeImage(bytes, 0)).thenReturn(expected);
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, mock(CourseAuthorization.class));

    assertThat(controller.decodeImage(id, 0, headers)).isEqualTo(expected);
  }

  @Test
  void authorizesBeforeIndexingADiagram() {
    var ingestion = mock(RagIngestionService.class);
    var authorization = mock(RagGatewayAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    UUID id = UUID.randomUUID();
    when(ingestion.get(id)).thenReturn(Optional.of(document(id)));
    var result = DiagramDecodeResult.vacio(0);
    RagController controller = new RagController(ingestion, mock(RagChatService.class), authorization, mock(CourseAuthorization.class));

    var response = controller.indexDiagram(id, result, headers);

    assertThat(response.getStatusCode().value()).isEqualTo(201);
    verify(ingestion).indexDiagram(id, result);
  }

  @Test
  void authorizesBeforeChatting() {
    var chat = mock(RagChatService.class);
    var authorization = mock(RagGatewayAuthorization.class);
    var courses = mock(CourseAuthorization.class);
    var headers = new HttpHeaders();
    when(authorization.require(headers)).thenReturn(actor);
    var idempotencyKey = UUID.randomUUID();
    var body = new RagController.ChatRequest(UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID()), "¿qué es Docker?", null);
    var expected = new RagChatService.Response("respuesta", "OK", null, 5, false, "Profesor Tutor Pedagógico", List.of(), UUID.randomUUID());
    when(chat.responder(any(), org.mockito.ArgumentMatchers.eq(idempotencyKey), org.mockito.ArgumentMatchers.eq(actor))).thenReturn(expected);
    RagController controller = new RagController(mock(RagIngestionService.class), chat, authorization, courses);

    var response = controller.chat(body, idempotencyKey, headers);

    assertThat(response).isEqualTo(expected);
    var order = Mockito.inOrder(authorization, courses, chat);
    order.verify(authorization).require(headers);
    order.verify(courses).requireTeacher(body.courseCohortId(), actor, headers);
    order.verify(chat).responder(any(), org.mockito.ArgumentMatchers.eq(idempotencyKey), org.mockito.ArgumentMatchers.eq(actor));
  }

  private RagDocument document(UUID id) {
    return new RagDocument(id, UUID.randomUUID(), "doc.pdf", 100, 1, 1, OffsetDateTime.now(), "preview", true);
  }

  private RagDocument sampleDocument() {
    return new RagDocument(UUID.randomUUID(), UUID.randomUUID(), "docker.pdf", 1000, 5, 3, OffsetDateTime.now(), "preview", true);
  }
}
