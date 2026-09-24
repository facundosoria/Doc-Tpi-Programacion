package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.service.ConversationService;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Message;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.TutorGatewayAuthorization;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** `POST/GET /api/llm/tutor/conversations` y `GET /{id}/messages` —
 * `docs/contracts/llm-service-v1.openapi.yaml`. El CRUD que
 * `docs/estado-implementacion/ep-05/interactions.md` había descartado explícitamente; esa
 * decisión se revisó, ver `docs/estado-implementacion/ep-05/conversations.md`. Reusa
 * {@link TutorGatewayAuthorization}: son el mismo scope M2M que el tutor (EP-05), no uno propio
 * como sí lo tiene RAG (EP-09, épica distinta). */
@RestController
@RequestMapping("${app.api.private-path}/tutor/conversations")
public class ConversationController {
  private final ConversationService service;
  private final TutorGatewayAuthorization authorization;

  public ConversationController(ConversationService service, TutorGatewayAuthorization authorization) {
    this.service = service;
    this.authorization = authorization;
  }

  @PostMapping
  public ResponseEntity<Conversation> create(@RequestBody CreateRequest body,
      @RequestHeader("Idempotency-Key") UUID idempotencyKey, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    UUID effectiveLearnerId = actor.delegatedUserId() != null ? actor.delegatedUserId() : body.learnerId();
    if (body.learnerId() != null && actor.delegatedUserId() != null && !body.learnerId().equals(actor.delegatedUserId())) {
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "No puede crear conversaciones para otro alumno");
    }
    Conversation conversation = service.create(body.courseCohortId(), effectiveLearnerId, body.challengeId(),
        body.titulo(), idempotencyKey, actor);
    return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
  }

  @GetMapping
  public List<Conversation> list(@RequestParam(required = false) UUID learnerId,
      @RequestParam(required = false) UUID courseCohortId,
      @RequestParam(required = false) UUID challengeId,
      @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    UUID effectiveLearnerId = learnerId != null ? learnerId : actor.delegatedUserId();
    if (learnerId != null && actor.delegatedUserId() != null && !learnerId.equals(actor.delegatedUserId())) {
      throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "No puede consultar conversaciones de otro alumno");
    }
    return service.list(effectiveLearnerId, courseCohortId, challengeId);
  }

  @GetMapping("/{id}/messages")
  public List<Message> messages(@PathVariable UUID id, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    return service.messages(id, actor.delegatedUserId());
  }

  @PostMapping("/{id}/messages")
  public ResponseEntity<Message> appendMessage(@PathVariable UUID id,
      @RequestBody AppendMessageRequest body,
      @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
      @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    Message message = service.appendMessage(id, actor.delegatedUserId(), body.contenido(), idempotencyKey, actor);
    return ResponseEntity.status(HttpStatus.CREATED).body(message);
  }

  /** Espejo de `CreateConversationRequest` del contrato v1. */
  public record CreateRequest(UUID courseCohortId, UUID learnerId, UUID challengeId, String titulo) {}

  public record AppendMessageRequest(String contenido) {}
}
