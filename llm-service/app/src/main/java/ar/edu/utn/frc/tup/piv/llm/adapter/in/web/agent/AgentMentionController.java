package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.validation.MentionSenderValidator;
import ar.edu.utn.frc.tup.piv.llm.application.service.agent.AgentMentionService;
import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.agent.AgentSelfMentionSanitizer;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.agent.MentionLoopCircuitBreaker;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el endpoint de menciones al @agente (HU LLM-S18-H01 · T1).
 * Responde en POST /api/llm/v1/agent/mentions y ${app.api.private-path}/v1/agent/mentions.
 */
@RestController
public class AgentMentionController {

  private final AgentMentionService service;
  private final MentionSenderValidator senderValidator;
  private final MentionLoopCircuitBreaker circuitBreaker;
  private final AgentSelfMentionSanitizer sanitizer;

  @Autowired
  public AgentMentionController(
      AgentMentionService service,
      MentionSenderValidator senderValidator,
      MentionLoopCircuitBreaker circuitBreaker,
      AgentSelfMentionSanitizer sanitizer) {
    this.service = service;
    this.senderValidator = senderValidator;
    this.circuitBreaker = circuitBreaker;
    this.sanitizer = sanitizer;
  }

  public AgentMentionController(AgentMentionService service) {
    this(service, new MentionSenderValidator(), new MentionLoopCircuitBreaker(), new AgentSelfMentionSanitizer());
  }

  public AgentMentionController(
      MentionSenderValidator senderValidator,
      MentionLoopCircuitBreaker circuitBreaker,
      AgentSelfMentionSanitizer sanitizer) {
    this(null, senderValidator, circuitBreaker, sanitizer);
  }

  @PostMapping({"/api/llm/v1/agent/mentions", "${app.api.private-path:/api/llm}/v1/agent/mentions"})
  public ResponseEntity<?> handleMention(
      @Valid @RequestBody AgentMentionRequest request,
      @RequestHeader(required = false) HttpHeaders headers) {

    // 1. Validación de rol de remitente (LLM-S18-H02 CA2/CA5)
    if (!senderValidator.isAuthorized(request.senderRole())) {
      return ResponseEntity.noContent().build();
    }

    // 2. Control de bucle por hilo (LLM-S18-H02 CA4)
    var loopResult = circuitBreaker.evaluateMention(request.threadId());
    if (!loopResult.allowed()) {
      ProblemDetail p = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
      p.setDetail("Posible bucle detectado en el hilo");
      p.setProperty("code", MentionLoopCircuitBreaker.CODE_LOOP_SUSPECTED);
      p.setProperty("thread_id", request.threadId());
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(p);
    }

    if (service == null) {
      String clean = sanitizer.sanitizeText(request.text());
      return ResponseEntity.ok(new AgentMentionResponse(
          clean, Collections.emptyList(), "OK"
      ));
    }

    CallerIdentity actor = extractActor(headers, request.studentId());
    AgentMentionService.Request serviceRequest = new AgentMentionService.Request(
        request.messageId(),
        request.cohortId(),
        request.threadId(),
        request.senderRole(),
        request.studentId(),
        request.text()
    );

    Optional<AgentMentionService.Response> serviceResponse = service != null ? service.processMention(serviceRequest, actor) : Optional.empty();

    return serviceResponse.map(res -> {
      List<MentionCitationDto> citations = res.citations().stream()
          .map(c -> new MentionCitationDto(c.documentName(), c.pageNumber(), c.excerpt()))
          .toList();
      return ResponseEntity.ok(new AgentMentionResponse(res.replyText(), citations, res.status()));
    }).orElseGet(() -> ResponseEntity.noContent().build());
  }

  public ResponseEntity<?> handleMention(AgentMentionRequest request) {
    return handleMention(request, null);
  }

  /**
   * Sobrecarga para compatibilidad con la suite de pruebas LLM-S18-H02.
   */
  public ResponseEntity<?> handleMention(ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.dto.AgentMentionRequest req) {
    if (req == null) {
      return ResponseEntity.noContent().build();
    }
    if (!senderValidator.isAuthorized(req.senderRole())) {
      return ResponseEntity.noContent().build();
    }
    var loopResult = circuitBreaker.evaluateMention(req.threadId());
    if (!loopResult.allowed()) {
      ProblemDetail p = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
      p.setDetail("Posible bucle detectado en el hilo");
      p.setProperty("code", MentionLoopCircuitBreaker.CODE_LOOP_SUSPECTED);
      p.setProperty("thread_id", req.threadId());
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(p);
    }
    if (service == null) {
      String clean = sanitizer.sanitizeText(req.text());
      return ResponseEntity.ok(new ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.dto.AgentMentionResponse(
          req.messageId(), clean, false, Collections.emptyMap()
      ));
    }
    AgentMentionRequest request = new AgentMentionRequest(
        req.messageId(), req.cohortId(), req.threadId(), req.senderRole(), req.studentId(), req.text()
    );
    return handleMention(request, null);
  }

  private CallerIdentity extractActor(HttpHeaders headers, String studentId) {
    if (headers == null) {
      return new CallerIdentity("chat-service", safeUuid(studentId), null, null);
    }
    String serviceId = headers.getFirst("X-Caller-Service-Id");
    if (serviceId == null || serviceId.isBlank()) {
      serviceId = "chat-service";
    }
    String userId = headers.getFirst(IdentityHeaders.USER_ID);
    if (userId == null || userId.isBlank()) {
      userId = studentId;
    }
    String requestId = headers.getFirst(IdentityHeaders.REQUEST_ID);
    String traceparent = headers.getFirst("traceparent");
    return new CallerIdentity(serviceId, safeUuid(userId), requestId, traceparent);
  }

  private UUID safeUuid(String id) {
    if (id == null || id.isBlank()) {
      return UUID.randomUUID();
    }
    try {
      return UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      return UUID.nameUUIDFromBytes(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
  }
}
