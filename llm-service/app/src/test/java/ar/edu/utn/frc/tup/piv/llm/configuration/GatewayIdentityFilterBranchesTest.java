package ar.edu.utn.frc.tup.piv.llm.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class GatewayIdentityFilterBranchesTest {
  private final GatewayIdentityFilter filter = new GatewayIdentityFilter();

  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  private static String jwt(String payload) {
    return "h." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".s";
  }

  private Authentication run(MockHttpServletRequest req) throws Exception {
    filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
    return SecurityContextHolder.getContext().getAuthentication();
  }

  private static java.util.List<String> names(Authentication a) {
    return a.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
  }

  @Test
  void bearerWithScopeScopesAndRolesBuildsAuthorities() throws Exception {
    filter.setTrustUnsignedBearer(true);
    var req = new MockHttpServletRequest();
    req.addHeader("Authorization", "bearer " + jwt("{\"sub\":\"u1\",\"scope\":\"a.b MS\",\"scopes\":[\"x.y\"],\"roles\":[\"ADMIN\"]}"));
    Authentication a = run(req);
    assertThat(a.getName()).isEqualTo("u1");
    assertThat(names(a)).containsExactlyInAnyOrder("a.b", "ROLE_MS", "x.y", "ROLE_ADMIN");
  }

  @Test
  void bearerFallsBackToServiceIdThenM2mDefault() throws Exception {
    filter.setTrustUnsignedBearer(true);
    var r1 = new MockHttpServletRequest();
    r1.addHeader("Authorization", "Bearer " + jwt("{\"service_id\":\"svc\"}"));
    assertThat(run(r1).getName()).isEqualTo("svc");
    SecurityContextHolder.clearContext();
    var r2 = new MockHttpServletRequest();
    r2.addHeader("Authorization", "Bearer " + jwt("{}"));
    assertThat(run(r2).getName()).isEqualTo("m2m-service");
  }

  @Test
  void undecodableOrBlankBearerDoesNotAuthenticate() throws Exception {
    filter.setTrustUnsignedBearer(true);
    var r1 = new MockHttpServletRequest();
    r1.addHeader("Authorization", "Bearer h.!!!notbase64.s");
    assertThat(run(r1)).isNull();
    var r2 = new MockHttpServletRequest();
    r2.addHeader("Authorization", "Bearer    ");
    assertThat(run(r2)).isNull();
    var r3 = new MockHttpServletRequest();
    r3.addHeader("Authorization", "Bearer nodots");
    assertThat(run(r3)).isNull();
  }

  @Test
  void workbenchModeGrantsLocalUserAuthorities() throws Exception {
    ReflectionTestUtils.setField(filter, "workbench", true);
    ReflectionTestUtils.setField(filter, "workbenchUser", "wb-user");
    Authentication a = run(new MockHttpServletRequest());
    assertThat(a.getName()).isEqualTo("wb-user");
    assertThat(names(a)).contains("ROLE_ADMIN", "ROLE_PROFESSOR", "llm.rag.query", "llm.tutor.interact");
  }

  @Test
  void serviceHeaderMapsMsScopeToRoleAndIgnoresBlankParts() throws Exception {
    var req = new MockHttpServletRequest();
    req.addHeader(IdentityHeaders.PRINCIPAL_TYPE, "service");
    req.addHeader(IdentityHeaders.SERVICE_ID, "svc-1");
    req.addHeader(IdentityHeaders.SERVICE_SCOPES, "MS, ,llm.rag.query");
    Authentication a = run(req);
    assertThat(names(a)).containsExactlyInAnyOrder("ROLE_MS", "llm.rag.query");
  }

  @Test
  void userWithoutRolesHeaderHasNoAuthorities() throws Exception {
    var req = new MockHttpServletRequest();
    req.addHeader(IdentityHeaders.PRINCIPAL_TYPE, "user");
    req.addHeader(IdentityHeaders.USER_ID, "u9");
    Authentication a = run(req);
    assertThat(a.getAuthorities()).isEmpty();
  }
}
