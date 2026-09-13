package ar.edu.utn.frc.tup.piv.llm.api;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
  private final ApiExceptionHandler handler = new ApiExceptionHandler();
  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @Test void mapsValidationAndIdempotencyConflictsToExpectedProblemStatuses() {
    assertThat(handler.invalid(new IllegalArgumentException("inválido"), request).getStatus()).isEqualTo(422);
    assertThat(handler.conflict(new IllegalStateException("en curso"), request).getStatus()).isEqualTo(409);
  }
}
