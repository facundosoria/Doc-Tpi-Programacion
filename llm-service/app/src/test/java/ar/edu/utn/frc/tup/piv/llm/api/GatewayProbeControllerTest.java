package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.GatewayProbeController;

import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayProbeControllerTest {

    private final GatewayProbeController controller = new GatewayProbeController();

    @Test
    void pingReturnsHeadersAndServiceIdWithoutAuth() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/llm/public/ping");
        request.addHeader(IdentityHeaders.REQUEST_ID, "req-123");

        Map<String, Object> res = controller.ping(request);

        assertThat(res.get("servicio")).isEqualTo("llm-service");
        assertThat(res.get("path")).isEqualTo("/api/llm/public/ping");
        assertThat(res.get("principal")).isNull();
        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) res.get("headersRecibidos");
        assertThat(headers.get(IdentityHeaders.REQUEST_ID)).isEqualTo("req-123");
    }

    @Test
    void quienSoyReturnsPrincipalAndAuthoritiesWhenAuthenticated() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/llm/quien-soy");
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "user-uuid", null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));

        Map<String, Object> res = controller.quienSoy(request, auth);

        assertThat(res.get("servicio")).isEqualTo("llm-service");
        assertThat(res.get("principal")).isEqualTo("user-uuid");
        @SuppressWarnings("unchecked")
        List<String> authorities = (List<String>) res.get("authorities");
        assertThat(authorities).containsExactly("ROLE_PROFESSOR");
    }

    @Test
    void internoReturnsServicePrincipal() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/llm/interno");
        var auth = UsernamePasswordAuthenticationToken.authenticated(
                "practice-service", null, List.of(
                        new SimpleGrantedAuthority("ROLE_MS"),
                        new SimpleGrantedAuthority("llm.golden-set.manage")
                ));

        Map<String, Object> res = controller.interno(request, auth);

        assertThat(res.get("servicio")).isEqualTo("llm-service");
        assertThat(res.get("principal")).isEqualTo("practice-service");
    }
}