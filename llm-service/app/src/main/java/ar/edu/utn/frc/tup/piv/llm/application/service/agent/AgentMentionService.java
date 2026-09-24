package ar.edu.utn.frc.tup.piv.llm.application.service.agent;

import ar.edu.utn.frc.tup.piv.llm.application.service.ModelInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RagQueryGuardrail;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.InputGuard;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.rag.DocumentChunk;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.agent.AgentSelfMentionSanitizer;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationDecisionCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.port.ModerationDecisionUseCase;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecision;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Orquestador principal de menciones al @agente (HU LLM-S18-H01).
 * Implementa recuperación RAG aislada por cohorte, abstención explícita BLOCKED_NO_SOURCE,
 * moderación previa síncrona, control de cuotas y guardarraíles.
 */
@Service
public class AgentMentionService {

  private static final Logger log = LoggerFactory.getLogger(AgentMentionService.class);

  public static final String STATUS_OK = "OK";
  public static final String STATUS_BLOCKED_NO_SOURCE = "BLOCKED_NO_SOURCE";
  public static final String STATUS_BLOCKED = "BLOCKED";

  public static final String ABSTENTION_MESSAGE =
      "No encontré esto en el material de tu curso, así que no puedo responderte con una fuente confiable. "
          + "Probá reformular tu pregunta sobre un tema del material indexado.";

  public static final String MODERATION_BLOCKED_MESSAGE =
      "El contenido generado por el asistente fue bloqueado por las políticas de moderación de la cátedra.";

  private final RagQueryService ragQueryService;
  private final ModelInvocationService modelInvocationService;
  private final ModerationDecisionUseCase moderationDecisionUseCase;
  private final QuotaRegistry quotaRegistry;
  private final AgentSelfMentionSanitizer selfMentionSanitizer;
  private final RagQueryGuardrail ragQueryGuardrail;
  private final InputGuard inputGuard = new InputGuard();
  private final AuditRepository auditRepository;
  private final Duration modelTimeout;

  @Autowired
  public AgentMentionService(
      RagQueryService ragQueryService,
      ModelInvocationService modelInvocationService,
      ModerationDecisionUseCase moderationDecisionUseCase,
      QuotaRegistry quotaRegistry,
      AgentSelfMentionSanitizer selfMentionSanitizer,
      RagQueryGuardrail ragQueryGuardrail,
      @Autowired(required = false) AuditRepository auditRepository,
      @Value("${llm.tutor.invocation-timeout-ms:8000}") long modelTimeoutMs) {
    this.ragQueryService = ragQueryService;
    this.modelInvocationService = modelInvocationService;
    this.moderationDecisionUseCase = moderationDecisionUseCase;
    this.quotaRegistry = quotaRegistry;
    this.selfMentionSanitizer = selfMentionSanitizer;
    this.ragQueryGuardrail = ragQueryGuardrail;
    this.auditRepository = auditRepository;
    this.modelTimeout = Duration.ofMillis(modelTimeoutMs);
  }

  public AgentMentionService(
      RagQueryService ragQueryService,
      ModelInvocationService modelInvocationService,
      ModerationDecisionUseCase moderationDecisionUseCase,
      QuotaRegistry quotaRegistry,
      AgentSelfMentionSanitizer selfMentionSanitizer,
      RagQueryGuardrail ragQueryGuardrail) {
    this(ragQueryService, modelInvocationService, moderationDecisionUseCase, quotaRegistry,
        selfMentionSanitizer, ragQueryGuardrail, null, 8000L);
  }

  /**
   * Procesa la mención al agente una vez validado el remitente y el hilo.
   */
  public Optional<Response> processMention(Request request, CallerIdentity actor) {
    // 1. Guardarraíles de entrada anti-jailbreak (CA4, T5) — Se evalúa antes de descontar cuota
    if (inputGuard.isJailbreak(request.text())) {
      log.warn("Jailbreak detectado en mención de alumno '{}': abortando antes de RAG/LLM.", request.studentId());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Intento de manipulación bloqueado");
    }

    if (ragQueryGuardrail != null) {
      var guardrailResult = ragQueryGuardrail.validate(request.text(), request.studentId());
      if (!guardrailResult.valid()) {
        log.warn("Guardarraíl RAG activado en mención: status='{}'", guardrailResult.status());
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, guardrailResult.userMessage());
      }
    }

    // 2. Control de cuota diaria por alumno (CA5, T5)
    quotaRegistry.consume(QuotaRegistry.FUNCTION_AGENT, request.studentId());

    // 3. Recuperación RAG aislada estrictamente por cohorte (CA1, T2)
    UUID cohortUuid = request.cohortUuid();
    List<DocumentChunk> chunks = ragQueryService.queryCohortContext(cohortUuid, request.text(), 4);

    // 4. Abstención explícita si no hay respaldo suficiente (CA2, Escenario 2, T3)
    if (chunks.isEmpty()) {
      log.info("Abstención explícita (BLOCKED_NO_SOURCE) para consulta en cohorte '{}': 0 tokens LLM consumidos.",
          request.cohortId());
      return Optional.of(new Response(ABSTENTION_MESSAGE, List.of(), STATUS_BLOCKED_NO_SOURCE));
    }

    // 5. Extracción de citas (documento, página, extracto)
    List<Citation> citations = new ArrayList<>();
    for (DocumentChunk chunk : chunks) {
      String docName = chunk.documentName() != null ? chunk.documentName() : "Documento de la cátedra";
      citations.add(new Citation(docName, chunk.pageNumber(), chunk.content()));
    }

    // 6. Generación con LLM fundamentada en las fuentes
    String systemPrompt = "Eres el asistente académico del curso. Responde de manera concisa y clara basándote en el material oficial.";
    String userPrompt = buildPromptWithContext(citations, request.text());
    var modelResult = modelInvocationService.invoke(ModelFunction.TUTOR, systemPrompt, userPrompt, modelTimeout);
    String generatedReply = modelResult != null ? modelResult.text() : "";

    // 7. Sanitización de auto-mención (LLM-S18-H02 · CA3 / T2)
    String sanitizedReply = selfMentionSanitizer != null ? selfMentionSanitizer.sanitizeText(generatedReply) : generatedReply;

    // 8. Interceptor síncrono de moderación previa (CA3, Escenario 3, T4)
    ModerationDecisionCommand modCommand = new ModerationDecisionCommand(
        "agent-" + request.messageId(),
        request.cohortId(),
        "system",
        sanitizedReply,
        Map.of("source", "agent-mention")
    );
    ModerationDecision decision = moderationDecisionUseCase.decide(modCommand);

    if (decision.getDecision() == ModerationDecisionEnum.BLOCK) {
      log.warn("Respuesta generada por el agente BLOQUEADA por moderación para mensaje '{}'.", request.messageId());
      recordAuditIfAvailable("agent.mention.moderation.blocked", request, actor, decision.getReasonCode());
      return Optional.of(new Response(MODERATION_BLOCKED_MESSAGE, List.of(), STATUS_BLOCKED));
    }

    recordAuditIfAvailable("agent.mention.answered", request, actor, "OK");

    return Optional.of(new Response(sanitizedReply, citations, STATUS_OK));
  }

  private String buildPromptWithContext(List<Citation> citations, String question) {
    StringBuilder sb = new StringBuilder();
    sb.append("<contexto_fuentes>\n");
    for (Citation c : citations) {
      sb.append(String.format("[Fuente: \"%s\" | Página %d]: %s%n%n", c.documentName(), c.pageNumber(), c.excerpt()));
    }
    sb.append("</contexto_fuentes>\n<pregunta_alumno>\n").append(question.trim()).append("\n</pregunta_alumno>");
    return sb.toString();
  }

  private void recordAuditIfAvailable(String action, Request request, CallerIdentity actor, String detail) {
    if (auditRepository != null && actor != null) {
      try {
        auditRepository.record(
            action,
            "agent-mention",
            UUID.randomUUID(),
            actor,
            "{\"messageId\":\"" + request.messageId() + "\",\"cohortId\":\"" + request.cohortId()
                + "\",\"detail\":\"" + detail + "\"}"
        );
      } catch (Exception e) {
        log.warn("No se pudo registrar auditoría de mención: {}", e.getMessage());
      }
    }
  }

  public record Request(
      String messageId,
      String cohortId,
      String threadId,
      String senderRole,
      String studentId,
      String text
  ) {
    public UUID cohortUuid() {
      if (cohortId == null) {
        return null;
      }
      try {
        return UUID.fromString(cohortId);
      } catch (IllegalArgumentException e) {
        return UUID.nameUUIDFromBytes(cohortId.getBytes(StandardCharsets.UTF_8));
      }
    }
  }

  public record Response(
      String replyText,
      List<Citation> citations,
      String status
  ) {
    public Response {
      if (citations == null) {
        citations = List.of();
      }
    }
  }

  public record Citation(
      String documentName,
      int pageNumber,
      String excerpt
  ) {}
}
