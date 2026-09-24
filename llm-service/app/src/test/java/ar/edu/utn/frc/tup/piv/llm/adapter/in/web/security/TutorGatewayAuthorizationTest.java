package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TutorGatewayAuthorizationTest {

  private final TutorGatewayAuthorization authorization = new TutorGatewayAuthorization("tutor-service", "tutor:scope");

  private static HttpHeaders validHeaders(String delegated) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Service-Id", "tutor-service");
    headers.set("X-Service-Scopes", "other tutor:scope");
    headers.set("X-Delegated-User", delegated);
    headers.set("X-Request-Id", "req-1");
    headers.set("traceparent", "tp-1");
    return headers;
  }

  @Test
  void allowsTheTrustedServiceWithTheRequiredScope() {
    var identity = authorization.require(validHeaders(UUID.randomUUID().toString()));

    assertThat(identity.serviceId()).isEqualTo("tutor-service");
    assertThat(identity.requestId()).isEqualTo("req-1");
    assertThat(identity.traceparent()).isEqualTo("tp-1");
  }

  @Test
  void rejectsAnUntrustedService() {
    HttpHeaders headers = validHeaders(UUID.randomUUID().toString());
    headers.set("X-Service-Id", "evil-service");

    assertUnauthorized(() -> authorization.require(headers));
  }

  @Test
  void rejectsMissingScopes() {
    HttpHeaders headers = validHeaders(UUID.randomUUID().toString());
    headers.remove("X-Service-Scopes");

    assertUnauthorized(() -> authorization.require(headers));
  }

  @Test
  void rejectsMissingScope() {
    HttpHeaders headers = validHeaders(UUID.randomUUID().toString());
    headers.set("X-Service-Scopes", "something-else");

    assertUnauthorized(() -> authorization.require(headers));
  }

  @Test
  void rejectsAMissingDelegatedUser() {
    assertForbidden(() -> authorization.require(validHeaders(null)));
  }

  @Test
  void rejectsAnInvalidDelegatedUserUuid() {
    assertForbidden(() -> authorization.require(validHeaders("not-a-uuid")));
  }

  private static void assertUnauthorized(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
    assertThatThrownBy(callable)
        .isInstanceOfSatisfying(ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
  }

  private static void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
    assertThatThrownBy(callable)
        .isInstanceOfSatisfying(ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
  }
}