package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.GoldenSetNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
  private final ApiExceptionHandler handler = new ApiExceptionHandler();
  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @Test void mapsMissingGoldenSetTo404AndPreservesRequestId() {
    request.addHeader("X-Request-Id", "req-42");
    var response = handler.missing(new GoldenSetNotFoundException(UUID.randomUUID()), request);
    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.getProperties()).containsEntry("requestId", "req-42");
  }

  @Test void mapsValidationAndIdempotencyConflictsToExpectedProblemStatuses() {
    assertThat(handler.invalid(new IllegalArgumentException("inválido"), request).getStatus()).isEqualTo(422);
    assertThat(handler.conflict(new IllegalStateException("en curso"), request).getStatus()).isEqualTo(409);
  }
}
