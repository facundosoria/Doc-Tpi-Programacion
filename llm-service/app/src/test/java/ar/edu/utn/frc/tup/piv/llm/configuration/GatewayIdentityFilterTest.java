package ar.edu.utn.frc.tup.piv.llm.configuration;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayIdentityFilterTest {

    private final GatewayIdentityFilter filter = new GatewayIdentityFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesUserPrincipalWithRoles() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(IdentityHeaders.PRINCIPAL_TYPE, "user");
        request.addHeader(IdentityHeaders.USER_ID, "user-uuid-123");
        request.addHeader(IdentityHeaders.USER_ROLES, "ADMIN,PROFESSOR");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getName()).isEqualTo("user-uuid-123");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_PROFESSOR");
    }

    @Test
    void authenticatesServicePrincipalWithScopes() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(IdentityHeaders.PRINCIPAL_TYPE, "service");
        request.addHeader(IdentityHeaders.SERVICE_ID, "users-service");
        request.addHeader(IdentityHeaders.SERVICE_SCOPES, "MS,llm.golden-set.manage");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getName()).isEqualTo("users-service");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_MS", "llm.golden-set.manage");
    }

    @Test
    void passesThroughUnauthenticatedWhenNoHeadersPresent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
    }

    private static String unsignedJwt(String payloadJson) {
        java.util.Base64.Encoder enc = java.util.Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString("{\"alg\":\"none\"}".getBytes()) + "."
                + enc.encodeToString(payloadJson.getBytes()) + ".x";
    }

    @Test
    void forgedBearerIsIgnoredByDefault() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer "
                + unsignedJwt("{\"sub\":\"attacker\",\"scope\":\"llm.golden-set.manage\"}"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void unsignedBearerIsAcceptedOnlyWhenExplicitlyTrusted() throws ServletException, IOException {
        filter.setTrustUnsignedBearer(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer "
                + unsignedJwt("{\"sub\":\"dev-user\",\"scope\":\"llm.tutor.interact\"}"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("dev-user");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("llm.tutor.interact");
    }
}
