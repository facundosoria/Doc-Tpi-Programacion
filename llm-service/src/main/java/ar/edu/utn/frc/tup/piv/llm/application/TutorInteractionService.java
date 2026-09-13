package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.InputGuard;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.InvalidModelResponseException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelTimeoutException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.OutputAntiLeakGuard;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.IdempotencyRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Implementa `POST /api/llm/tutor/interactions` tal cual
 * `docs/contracts/llm-service-v1.openapi.yaml` (`TutorInteractionRequest`/`Response`). Orquesta:
 * idempotencia (reutiliza {@link IdempotencyRepository}, igual que el resto del servicio) →
 * {@link InputGuard} (jailbreak → respuesta fija, sin llamar al modelo) → prompt desde
 * `prompts/tutor/{system,user}-v1.txt` → {@link ModelInvocationService} →
 * {@link OutputAntiLeakGuard} cuando `riskLevel` es `high`/`medium` → auditoría.
 *
 * <p>Portado de `codigo-ejemplo/ms-evaluacion-llm/.../tutor/TutorServiceImpl.java` — no se porta
 * el CRUD de conversaciones (`TutorChatController`, `/api/conversaciones`): el contrato acordado
 * solo define esta interacción síncrona, sin historial navegable por API todavía. Streaming/SSE
 * (Buffer Interceptor, `llm-service-v1-tutor-sse-adenda.md`) queda fuera de esta pasada. */
@Service
public class TutorInteractionService {
  private static final String OPERATION = "tutor.interaction";

  private final ModelInvocationService models;
  private final IdempotencyRepository idempotency;
  private final AuditRepository audit;
  private final ObjectMapper mapper;
  private final InputGuard inputGuard = new InputGuard();
  private final OutputAntiLeakGuard outputGuard = new OutputAntiLeakGuard();
  private final Duration timeout;
  private final String systemPrompt;
  private final String userPromptTemplate;

  public TutorInteractionService(ModelInvocationService models, IdempotencyRepository idempotency,
      AuditRepository audit, ObjectMapper mapper,
      @Value("${llm.tutor.invocation-timeout-ms:8000}") long timeoutMs) {
    this.models = models;
    this.idempotency = idempotency;
    this.audit = audit;
    this.mapper = mapper;
    this.timeout = Duration.ofMillis(timeoutMs);
    this.systemPrompt = readPrompt("system-v1.txt");
    this.userPromptTemplate = readPrompt("user-v1.txt");
  }

  private String readPrompt(String file) {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("prompts/tutor/" + file)) {
      if (in == null) return "";
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      return "";
    }
  }

  public Response respond(Request request, UUID idempotencyKey, CallerIdentity actor) {
    String hash = hash(request);
    var replay = idempotency.replay(OPERATION, actor, idempotencyKey, hash);
    if (replay.isPresent()) {
      return parse(replay.get());
    }

    UUID interactionId = UUID.randomUUID();
    boolean guardTriggered = false;
    Response response;

    if (inputGuard.isJailbreak(request.message())) {
      response = new Response(InputGuard.SAFE_REDIRECT, "completed");
      guardTriggered = true;
    } else {
      response = invokeModel(request);
      if ("completed".equals(response.state()) && !"low".equals(request.riskLevel())
          && outputGuard.containsLeak(response.message(), null)) {
        response = new Response(OutputAntiLeakGuard.SAFE_REPLACEMENT, "completed");
        guardTriggered = true;
      }
    }

    audit.record(OPERATION, "tutor-interaction", interactionId, actor, auditDetails(request, response, guardTriggered));
    idempotency.complete(OPERATION, actor, idempotencyKey, interactionId, mapper.valueToTree(response));
    return response;
  }

  private Response invokeModel(Request request) {
    String userPrompt = userPromptTemplate.isBlank()
        ? request.message()
        : userPromptTemplate
            .replace("{tema}", "Desafío " + request.challengeId())
            .replace("{historico}", "")
            .replace("{pregunta}", request.message());
    String system = systemPrompt.isBlank()
        ? "Eres un tutor socrático. Guía al alumno sin dar la solución de código."
        : systemPrompt;
    try {
      var result = models.invoke(ModelFunction.TUTOR, system, userPrompt, timeout);
      return new Response(result.text(), "completed");
    } catch (ModelTimeoutException | InvalidModelResponseException exception) {
      return new Response(
          "El tutor no está disponible en este momento. Podés seguir intentando el desafío mientras se restablece.",
          "unavailable");
    }
  }

  private String auditDetails(Request request, Response response, boolean guardTriggered) {
    return "{\"attemptId\":\"" + request.attemptId() + "\",\"challengeId\":\"" + request.challengeId()
        + "\",\"courseCohortId\":\"" + request.courseCohortId() + "\",\"learnerId\":\"" + request.learnerId()
        + "\",\"riskLevel\":\"" + request.riskLevel() + "\",\"state\":\"" + response.state()
        + "\",\"guardTriggered\":" + guardTriggered + "}";
  }

  private Response parse(com.fasterxml.jackson.databind.JsonNode node) {
    try {
      return mapper.treeToValue(node, Response.class);
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo leer la respuesta idempotente del tutor", exception);
    }
  }

  private String hash(Request request) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      String canonical = request.attemptId() + "|" + request.challengeId() + "|" + request.courseCohortId()
          + "|" + request.learnerId() + "|" + request.message() + "|" + request.riskLevel();
      return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  /** Espejo de `TutorInteractionRequest` del contrato v1. */
  public record Request(UUID attemptId, UUID challengeId, UUID courseCohortId, UUID learnerId, String message, String riskLevel) {}

  /** Espejo de `TutorInteractionResponse` del contrato v1 (`state`: completed | blocked | unavailable). */
  public record Response(String message, String state) {}
}
