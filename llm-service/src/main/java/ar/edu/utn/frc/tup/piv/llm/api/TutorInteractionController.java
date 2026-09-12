package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.TutorInteractionService;
import ar.edu.utn.frc.tup.piv.llm.security.TutorGatewayAuthorization;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** `POST /api/llm/tutor/interactions` — `docs/contracts/llm-service-v1.openapi.yaml`
 * (`TutorInteractionRequest`/`Response`). Cierra el hueco que
 * `llm-service/CORRECCIONES-SUGERIDAS.md` ítem 14 señalaba: EP-05 no tenía ningún controller
 * todavía. La variante SSE (`/interactions/stream`) no está implementada — ver
 * `llm-service-v1-tutor-sse-adenda.md` ("hasta entonces el contrato ejecutable es solo el
 * endpoint síncrono"). */
@RestController
@RequestMapping("/api/llm/tutor")
public class TutorInteractionController {
  private static final Set<String> RISK_LEVELS = Set.of("high", "medium", "low");

  private final TutorInteractionService service;
  private final TutorGatewayAuthorization authorization;

  public TutorInteractionController(TutorInteractionService service, TutorGatewayAuthorization authorization) {
    this.service = service;
    this.authorization = authorization;
  }

  @PostMapping("/interactions")
  public TutorInteractionService.Response create(@RequestBody Request body,
      @RequestHeader("Idempotency-Key") UUID idempotencyKey, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    validate(body);
    var request = new TutorInteractionService.Request(body.attemptId(), body.challengeId(),
        body.courseCohortId(), body.learnerId(), body.message(), body.riskLevel());
    return service.respond(request, idempotencyKey, actor);
  }

  private void validate(Request body) {
    if (body.attemptId() == null || body.challengeId() == null || body.courseCohortId() == null || body.learnerId() == null) {
      throw new IllegalArgumentException("attemptId, challengeId, courseCohortId y learnerId son obligatorios");
    }
    if (body.message() == null || body.message().isBlank()) {
      throw new IllegalArgumentException("message es obligatorio");
    }
    if (body.riskLevel() == null || !RISK_LEVELS.contains(body.riskLevel())) {
      throw new IllegalArgumentException("riskLevel debe ser high, medium o low");
    }
  }

  /** Espejo de `TutorInteractionRequest` del contrato v1. */
  public record Request(UUID attemptId, UUID challengeId, UUID courseCohortId, UUID learnerId, String message, String riskLevel) {}
}
