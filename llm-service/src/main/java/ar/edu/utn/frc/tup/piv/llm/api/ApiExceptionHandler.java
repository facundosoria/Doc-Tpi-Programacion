package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ar.edu.utn.frc.tup.piv.llm.application.CourseGoldenSetService.GoldenSetSizeException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(GoldenSetSizeException.class)
  ProblemDetail goldenSetSize(GoldenSetSizeException exception, HttpServletRequest request) {
    return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
  }
  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail invalid(IllegalArgumentException exception, HttpServletRequest request) { return problem(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request); }
  @ExceptionHandler(OptimisticLockException.class)
  ProblemDetail staleDraft(OptimisticLockException exception, HttpServletRequest request) { return problem(HttpStatus.CONFLICT, exception.getMessage(), request); }
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
  private ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setProperty("requestId", request.getHeader("X-Request-Id"));
    return problem;
  }
}
