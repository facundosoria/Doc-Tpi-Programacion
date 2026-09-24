package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

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
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** `/api/llm/rag/**` — `docs/contracts/llm-service-v1.openapi.yaml` (EP-09). Reusa
 * {@link RagGatewayAuthorization}: scope M2M propio, distinto del tutor (EP-05). Portado de
 * `demoLLMSpringAi/.../controller/RagController.java`. */
@RestController
@RequestMapping("${app.api.private-path}/rag")
public class RagController {
  private final RagIngestionService ingestion;
  private final RagChatService chat;
  private final RagGatewayAuthorization authorization;
  private final CourseAuthorization courseAuthorization;

  public RagController(RagIngestionService ingestion, RagChatService chat, RagGatewayAuthorization authorization,
      CourseAuthorization courseAuthorization) {
    this.ingestion = ingestion;
    this.chat = chat;
    this.authorization = authorization;
    this.courseAuthorization = courseAuthorization;
  }

  @GetMapping("/documents")
  public List<RagDocument> listDocuments(@RequestParam UUID courseCohortId, @RequestHeader HttpHeaders headers) {
    CallerIdentity actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseCohortId, actor, headers);
    return ingestion.list(courseCohortId);
  }

  @PostMapping("/documents")
  public ResponseEntity<RagDocument> uploadDocument(@RequestParam UUID courseCohortId,
      @RequestParam("file") MultipartFile file, @RequestHeader("Idempotency-Key") UUID idempotencyKey,
      @RequestHeader HttpHeaders headers) throws IOException {
    CallerIdentity actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseCohortId, actor, headers);
    // Corta por tamaño antes de materializar el archivo en memoria (#669).
    ingestion.validateDeclaredUploadSize(file.getSize(), file.getOriginalFilename());
    RagDocument document = ingestion.upload(courseCohortId, file.getOriginalFilename(), file.getBytes(),
        idempotencyKey, actor);
    return ResponseEntity.status(HttpStatus.CREATED).body(document);
  }

  @PostMapping("/documents/sample")
  public ResponseEntity<RagDocument> uploadSample(@RequestParam UUID courseCohortId,
      @RequestHeader("Idempotency-Key") UUID idempotencyKey, @RequestHeader HttpHeaders headers) throws IOException {
    CallerIdentity actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseCohortId, actor, headers);
    RagDocument document = ingestion.uploadSample(courseCohortId, idempotencyKey, actor);
    return ResponseEntity.status(HttpStatus.CREATED).body(document);
  }

  /** Retiro lógico (#672). La cohorte se deriva de la fuente y se exige rol docente sobre ella;
   * una fuente inexistente o de una cohorte que el actor no administra responde 404 (no 403) para
   * no filtrar existencia. El contrato (`DELETE /rag/documents/{documentId}` → 204/404) no cambia. */
  @DeleteMapping("/documents/{id}")
  public ResponseEntity<Void> deleteDocument(@PathVariable UUID id, @RequestHeader HttpHeaders headers) {
    CallerIdentity actor = authorization.require(headers);
    UUID courseCohortId = ingestion.get(id).map(RagDocument::courseCohortId)
        .orElseThrow(() -> new RagDocumentNotFoundException(id));
    try {
      courseAuthorization.requireTeacher(courseCohortId, actor, headers);
    } catch (ResponseStatusException forbidden) {
      throw new RagDocumentNotFoundException(id);
    }
    ingestion.retire(id, courseCohortId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/documents/{id}/chunks")
  public List<DocumentChunk> chunks(@PathVariable UUID id, @RequestHeader HttpHeaders headers) {
    CallerIdentity actor = authorization.require(headers);
    authorizeDocument(id, actor, headers);
    return ingestion.getChunks(id);
  }

  @GetMapping("/documents/{id}/images")
  public List<ImageDetection> images(@PathVariable UUID id, @RequestHeader HttpHeaders headers) {
    CallerIdentity actor = authorization.require(headers);
    authorizeDocument(id, actor, headers);
    byte[] bytes = resolvePdfBytesOrThrow(id);
    return ingestion.detectImages(bytes);
  }

  @PostMapping("/documents/{id}/images/{imageIndex}/decode")
  public DiagramDecodeResult decodeImage(@PathVariable UUID id, @PathVariable int imageIndex, @RequestHeader HttpHeaders headers) {
    CallerIdentity actor = authorization.require(headers);
    authorizeDocument(id, actor, headers);
    byte[] bytes = resolvePdfBytesOrThrow(id);
    return ingestion.decodeImage(bytes, imageIndex);
  }

  @PostMapping("/documents/{id}/diagrams")
  public ResponseEntity<Void> indexDiagram(@PathVariable UUID id, @RequestBody DiagramDecodeResult result,
      @RequestHeader HttpHeaders headers) {
    CallerIdentity actor = authorization.require(headers);
    authorizeDocument(id, actor, headers);
    ingestion.indexDiagram(id, result);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/chat")
  public RagChatService.Response chat(@RequestBody ChatRequest body,
      @RequestHeader("Idempotency-Key") UUID idempotencyKey, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(body.courseCohortId(), actor, headers);
    var request = new RagChatService.Request(body.courseCohortId(), body.learnerId(), body.documentIds(),
        body.pregunta(), body.conversacionId());
    return chat.responder(request, idempotencyKey, actor);
  }

  /** Resuelve la fuente por id y exige que el actor sea docente del curso dueno de esa fuente:
   * sin esto cualquier docente autenticado podia operar sobre documentos de otro curso (IDOR). */
  private void authorizeDocument(UUID id, CallerIdentity actor, HttpHeaders headers) {
    RagDocument document = ingestion.get(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fuente no encontrada: " + id));
    courseAuthorization.requireTeacher(document.courseCohortId(), actor, headers);
  }

  private byte[] resolvePdfBytesOrThrow(UUID id) {
    return ingestion.getPdfBytes(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fuente no encontrada: " + id));
  }

  /** Espejo de `RagChatRequest` del contrato v1. */
  public record ChatRequest(UUID courseCohortId, UUID learnerId, List<UUID> documentIds, String pregunta, UUID conversacionId) {}
}
