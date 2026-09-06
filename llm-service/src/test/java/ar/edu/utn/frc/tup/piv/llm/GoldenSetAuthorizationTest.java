package ar.edu.utn.frc.tup.piv.llm;

import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoldenSetAuthorizationTest {
  private final UUID demoUser = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private final GoldenSetAuthorization authorization = new GoldenSetAuthorization("admin-service", "llm.golden-set.manage", false, demoUser);
  @Test void acceptsDelegatedAdminService() { assertThat(authorization.require(headers("admin-service", "llm.golden-set.manage")).serviceId()).isEqualTo("admin-service"); }
  @Test void rejectsMissingScope() { assertThatThrownBy(() -> authorization.require(headers("admin-service", "other.scope"))).isInstanceOf(ResponseStatusException.class); }
  @Test void workbenchDoesNotTrustBrowserIdentity() {
    GoldenSetAuthorization workbench = new GoldenSetAuthorization("admin-service", "llm.golden-set.manage", true, demoUser);
    assertThat(workbench.require(new HttpHeaders()).delegatedUserId()).isEqualTo(demoUser);
  }
  private HttpHeaders headers(String service, String scope) { HttpHeaders headers = new HttpHeaders(); headers.add("X-Service-Id", service); headers.add("X-Service-Scopes", scope); headers.add("X-Delegated-User", demoUser.toString()); return headers; }
}
