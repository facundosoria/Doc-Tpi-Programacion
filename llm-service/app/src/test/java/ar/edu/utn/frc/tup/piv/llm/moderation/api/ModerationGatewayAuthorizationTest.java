package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationGatewayAuthorizationTest {

    private ModerationGatewayAuthorization authorization;

    @BeforeEach
    void setUp() {
        authorization = new ModerationGatewayAuthorization("chat-service", "moderation:decide", false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsWhenSecurityContextHasRequiredAuthority() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "chat-service", null, List.of(new SimpleGrantedAuthority("moderation:decide"))
                )
        );

        assertThatCode(() -> authorization.requireScope(new HttpHeaders())).doesNotThrowAnyException();
    }

    @Test
    void allowsWhenGatewayHeadersContainRequiredScope() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(IdentityHeaders.SERVICE_SCOPES, "some.scope,moderation:decide");

        assertThatCode(() -> authorization.requireScope(headers)).doesNotThrowAnyException();
    }

    @Test
    void throws401WhenNoAuthenticationAndNoHeaders() {
        assertThatThrownBy(() -> authorization.requireScope(new HttpHeaders()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(rse.getReason()).contains("Missing scope: moderation:decide");
                });
    }

    @Test
    void throws401WhenScopeIsMissingInContextAndHeaders() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "chat-service", null, List.of(new SimpleGrantedAuthority("other:scope"))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.add(IdentityHeaders.SERVICE_SCOPES, "other:scope");

        assertThatThrownBy(() -> authorization.requireScope(headers))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    void allowsInWorkbenchMode() {
        ModerationGatewayAuthorization workbenchAuth = new ModerationGatewayAuthorization("chat-service", "moderation:decide", true);
        assertThatCode(() -> workbenchAuth.requireScope(new HttpHeaders())).doesNotThrowAnyException();
    }
}
