package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GoldenSetAuthorizationTest {

  private final GoldenSetAuthorization authorization =
      new GoldenSetAuthorization("gateway-service", "llm:admin", "llm:templates");

  private static HttpHeaders headers(String service, String scopes, String delegated) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Service-Id", service);
    headers.set("X-Service-Scopes", scopes);
    headers.set("X-Delegated-User", delegated);
    return headers;
  }

  @Test
  void allowsTheTrustedServiceWithTheEvaluationScope() {
    var identity = authorization.require(headers("gateway-service", "llm:admin other", UUID.randomUUID().toString()));

    assertThat(identity.serviceId()).isEqualTo("gateway-service");
    assertThat(identity.delegatedUserId()).isNotNull();
  }

  @Test
  void allowsTemplateManagersWithTheirOwnScope() {
    var identity = authorization.requireTemplateManager(headers("gateway-service", "llm:templates", UUID.randomUUID().toString()));

    assertThat(identity.serviceId()).isEqualTo("gateway-service");
  }

  @Test
  void requireRejectsAnUntrustedService() {
    assertUnauthorized(() -> authorization.require(headers("other", "llm:admin", UUID.randomUUID().toString())));
  }

  @Test
  void requireRejectsMissingScopes() {
    assertUnauthorized(() -> authorization.require(headers("gateway-service", null, UUID.randomUUID().toString())));
  }

  @Test
  void requireRejectsAWrongScope() {
    assertUnauthorized(() -> authorization.require(headers("gateway-service", "llm:templates", UUID.randomUUID().toString())));
  }

  @Test
  void requireRejectsAMissingDelegatedUser() {
    assertForbidden(() -> authorization.require(headers("gateway-service", "llm:admin", null)));
  }

  @Test
  void requireRejectsAnInvalidDelegatedUserUuid() {
    assertForbidden(() -> authorization.require(headers("gateway-service", "llm:admin", "nope")));
  }

  @Test
  void requireTemplateManagerRejectsTheEvaluationScope() {
    assertUnauthorized(() -> authorization.requireTemplateManager(headers("gateway-service", "llm:admin", UUID.randomUUID().toString())));
  }

  @Test
  void requireInstitutionalManagerRequiresAdminRole() {
    var headers = headers("gateway-service", "llm.institutional-calibration.manage", UUID.randomUUID().toString());
    assertForbidden(() -> authorization.requireInstitutionalManager(headers));
    
    headers.set("X-User-Roles", "USER, ADMIN");
    var identity = authorization.requireInstitutionalManager(headers);
    assertThat(identity.serviceId()).isEqualTo("gateway-service");
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