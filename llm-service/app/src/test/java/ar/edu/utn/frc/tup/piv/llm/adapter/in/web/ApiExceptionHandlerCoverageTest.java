package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frc.tup.piv.llm.application.service.CourseGoldenSetService.GoldenSetSizeException;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiExceptionHandlerCoverageTest {
  private static final String REQUEST_ID = "request-id-42";
  private final ApiExceptionHandler handler = new ApiExceptionHandler();
  private final MockHttpServletRequest request = requestWith(REQUEST_ID);

  @Test
  void goldenSetSizeMapsToConflictCarryingTheRequestId() {
    var problem = handler.goldenSetSize(new GoldenSetSizeException("too many cases"), request);

    assertThat(problem.getStatus()).isEqualTo(409);
    assertThat(problem.getProperties()).containsEntry("requestId", REQUEST_ID);
  }

  @Test
  void incompleteGoldenSetMapsToBadRequest() {
    var problem = handler.incompleteGoldenSet(new ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationRunService.IncompleteGoldenSetException("incomplete"), request);
    assertThat(problem.getStatus()).isEqualTo(400);
    assertThat(problem.getProperties()).containsEntry("error", "validation_error");
    assertThat(problem.getProperties()).containsEntry("requestId", REQUEST_ID);
  }

  @Test
  void staleDraftMapsToConflict() {
    assertThat(handler.staleDraft(new OptimisticLockException("cambió"), request).getStatus()).isEqualTo(409);
  }

  @Test
  void integrityViolationAboutReferenceScoresGetsASpecialDetail() {
    var exception = exceptionWithCause("constraint golden_set_cases_reference_scores_check violated");

    var problem = handler.invalidData(exception, request);

    assertThat(problem.getStatus()).isEqualTo(422);
    assertThat(problem.getDetail()).contains("puntajes de referencia");
  }

  @Test
  void integrityViolationAboutDuplicateModelDeploymentGetsItsOwnDetail() {
    var exception = exceptionWithCause("constraint model_deployments_adapter_id_model_id_model_version_key violated");

    var problem = handler.invalidData(exception, request);

    assertThat(problem.getStatus()).isEqualTo(422);
    assertThat(problem.getDetail()).contains("Ese modelo ya existe");
  }

  @Test
  void integrityViolationWithoutKnownConstraintFallsBackToGenericDetail() {
    var exception = exceptionWithCause("some other constraint");

    var problem = handler.invalidData(exception, request);

    assertThat(problem.getDetail()).contains("formato requerido");
  }

  private DataIntegrityViolationException exceptionWithCause(String message) {
    return new DataIntegrityViolationException(message, new RuntimeException(message));
  }

  private MockHttpServletRequest requestWith(String requestId) {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-Id", requestId);
    return request;
  }
}