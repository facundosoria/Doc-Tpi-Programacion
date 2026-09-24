package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ChallengeNotActiveException;
import ar.edu.utn.frc.tup.piv.llm.domain.tutor.ConversationOwnershipException;
import ar.edu.utn.frc.tup.piv.llm.application.service.CourseGoldenSetService.GoldenSetSizeException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ConversationOwnershipException.class)
  ProblemDetail conversationOwnership(ConversationOwnershipException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    p.setProperty("error", "forbidden");
    return p;
  }

  @ExceptionHandler(ChallengeNotActiveException.class)
  ProblemDetail challengeNotActive(ChallengeNotActiveException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    p.setProperty("error", "challenge_not_found");
    return p;
  }

  @ExceptionHandler(GoldenSetSizeException.class)
  ProblemDetail goldenSetSize(GoldenSetSizeException exception, HttpServletRequest request) {
    return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
  }
  @ExceptionHandler(ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationRunService.IncompleteGoldenSetException.class)
  ProblemDetail incompleteGoldenSet(ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationRunService.IncompleteGoldenSetException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    p.setProperty("error", "validation_error");
    return p;
  }
  @ExceptionHandler(ar.edu.utn.frc.tup.piv.llm.moderation.application.exception.AppealAlreadyExistsException.class)
  ProblemDetail appealAlreadyExists(ar.edu.utn.frc.tup.piv.llm.moderation.application.exception.AppealAlreadyExistsException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.CONFLICT, "appeal_already_exists", request);
    p.setProperty("error", "appeal_already_exists");
    if (exception.getExistingAppealId() != null) {
      p.setProperty("appeal_id", exception.getExistingAppealId().toString());
    }
    return p;
  }
  @ExceptionHandler(ar.edu.utn.frc.tup.piv.llm.application.service.agent.QuotaExceededException.class)
  org.springframework.http.ResponseEntity<ProblemDetail> quotaExceeded(ar.edu.utn.frc.tup.piv.llm.application.service.agent.QuotaExceededException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.TOO_MANY_REQUESTS, exception.getReason(), request);
    p.setProperty("error", "quota_exceeded");
    p.setProperty("function", exception.getFunctionKey());
    p.setProperty("retryAfter", exception.getRetryAfterSeconds());
    return org.springframework.http.ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(org.springframework.http.HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()))
        .body(p);
  }
  @ExceptionHandler(ar.edu.utn.frc.tup.piv.llm.domain.ai.BudgetExceededException.class)
  ProblemDetail budgetExceeded(ar.edu.utn.frc.tup.piv.llm.domain.ai.BudgetExceededException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), request);
    p.setProperty("error", "budget_exceeded");
    p.setProperty("function", exception.function().name().toLowerCase());
    return p;
  }
  @ExceptionHandler(ar.edu.utn.frc.tup.piv.llm.domain.ai.ProviderUnavailableException.class)
  ProblemDetail providerUnavailable(ar.edu.utn.frc.tup.piv.llm.domain.ai.ProviderUnavailableException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    p.setProperty("error", "provider_unavailable");
    return p;
  }
  /**
   * Los adaptadores del SPI validan la credencial/configuración y devuelven su diagnóstico como
   * {@link ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderException}. Una entrada inválida es del
   * cliente (422); el resto (proveedor caído, HTTP 5xx) es indisponibilidad (503). Sin este mapeo
   * el error salía como 500, ocultando la causa.
   */
  @ExceptionHandler(ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderException.class)
  ProblemDetail providerFailure(ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderException exception, HttpServletRequest request) {
    HttpStatus status = providerStatus(exception.code());
    ProblemDetail p = problem(status, exception.getMessage(), request);
    p.setProperty("error", status == HttpStatus.UNPROCESSABLE_ENTITY ? "validation_error" : "provider_unavailable");
    p.setProperty("provider_code", exception.code());
    return p;
  }
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
  @ExceptionHandler(ResponseStatusException.class)
  ProblemDetail statusException(ResponseStatusException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.valueOf(exception.getStatusCode().value()), exception.getReason(), request);
    if (exception.getStatusCode() == HttpStatus.FORBIDDEN) {
      p.setProperty("error", "forbidden");
    } else if (exception.getStatusCode() == HttpStatus.BAD_REQUEST) {
      p.setProperty("error", "validation_error");
    }
    return p;
  }
  @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
  ProblemDetail validationException(org.springframework.web.bind.MethodArgumentNotValidException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.BAD_REQUEST, "Los datos enviados no son vǭlidos o contienen campos obligatorios ausentes.", request);
    p.setProperty("error", "validation_error");
    return p;
  }
  @ExceptionHandler(ar.edu.utn.frc.tup.piv.llm.domain.rag.InvalidPdfSourceException.class)
  ProblemDetail invalidPdfSource(ar.edu.utn.frc.tup.piv.llm.domain.rag.InvalidPdfSourceException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    p.setProperty("error", "invalid_source_file");
    return p;
  }
  @ExceptionHandler(ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocumentNotFoundException.class)
  ProblemDetail ragDocumentNotFound(ar.edu.utn.frc.tup.piv.llm.domain.rag.RagDocumentNotFoundException exception, HttpServletRequest request) {
    return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }
  @ExceptionHandler(EvaluatorSkillsController.UnknownSkillKeyException.class)
  ProblemDetail unknownSkillKey(EvaluatorSkillsController.UnknownSkillKeyException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    p.setProperty("code", "UNKNOWN_SKILL_KEY");
    return p;
  }
  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail invalid(IllegalArgumentException exception, HttpServletRequest request) { return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request); }
  @ExceptionHandler(OptimisticLockException.class)
  ProblemDetail staleDraft(OptimisticLockException exception, HttpServletRequest request) { return problem(HttpStatus.CONFLICT, exception.getMessage(), request); }
  @ExceptionHandler(ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException.class)
  ProblemDetail notFound(ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException exception, HttpServletRequest request) {
    ProblemDetail p = problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    p.setProperty("error", "not_found");
    return p;
  }
  @ExceptionHandler(IllegalStateException.class)
  ProblemDetail conflict(IllegalStateException exception, HttpServletRequest request) { return problem(HttpStatus.CONFLICT, exception.getMessage(), request); }
  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail invalidData(DataIntegrityViolationException exception, HttpServletRequest request) {
    String cause = exception.getMostSpecificCause().getMessage();
    log.warn("Data integrity error while handling {}: {}", request.getRequestURI(), cause);
    String detail = cause != null && cause.contains("golden_set_cases_reference_scores_check")
        ? "No se pudo copiar la versión porque uno de sus casos tiene puntajes de referencia incompatibles. Editá o recreá el caso y volvé a publicar."
        : cause != null && cause.contains("model_deployments_adapter_id_model_id_model_version_key")
            ? "Ese modelo ya existe. Se reutilizará al asignarlo a una tarjeta; volvé a intentar."
            : "Los datos no cumplen el formato requerido; revisá los puntajes y campos obligatorios.";
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, detail, request);
  }
  private HttpStatus providerStatus(String code) {
    if (code == null) return HttpStatus.SERVICE_UNAVAILABLE;
    return switch (code) {
      case "INVALID_PROVIDER", "PROVIDER_NOT_INSTALLED", "INVALID_CONFIGURATION", "INVALID_CREDENTIAL",
          "PROVIDER_AUTHENTICATION_FAILED", "PROVIDER_INVALID_REQUEST", "PROVIDER_MODEL_NOT_FOUND" ->
          HttpStatus.UNPROCESSABLE_ENTITY;
      default -> code.startsWith("PROVIDER_HTTP_4")
          ? HttpStatus.UNPROCESSABLE_ENTITY
          : HttpStatus.SERVICE_UNAVAILABLE;
    };
  }
  private ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    String requestId = request.getHeader("X-Request-Id");
    if (requestId == null) {
      Object attr = request.getAttribute("X-Request-Id");
      if (attr != null) {
        requestId = attr.toString();
      }
    }
    problem.setProperty("requestId", requestId);
    return problem;
  }
}
