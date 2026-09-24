package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validador de identidad y alcance M2M para endpoints de moderación (POST /moderation/v1/decisions).
 * Exige JWT o cabeceras de Gateway con el scope requerido ('moderation:decide').
 */
@Component
public class ModerationGatewayAuthorization {

    private final String trustedService;
    private final String requiredScope;
    private final boolean workbench;

    public ModerationGatewayAuthorization(
            @Value("${llm.moderation.trusted-service:chat-service}") String trustedService,
            @Value("${llm.moderation.required-scope:moderation:decide}") String requiredScope,
            @Value("${llm.workbench.enabled:false}") boolean workbench) {
        this.trustedService = trustedService;
        this.requiredScope = requiredScope;
        this.workbench = workbench;
    }

    /**
     * Valida que la petición cuente con la autenticación requerida y el scope 'moderation:decide'.
     * Si no cumple los requisitos, lanza ResponseStatusException con 401 UNAUTHORIZED según CA5.
     */
    public void requireScope(HttpHeaders headers) {
        if (workbench) {
            return;
        }

        // 1. Verificar Authentication en el contexto de Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean hasAuthority = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> requiredScope.equals(a) || ("ROLE_" + requiredScope).equals(a));
            if (hasAuthority) {
                return;
            }
        }

        // 2. Verificar cabeceras directas de Gateway si están presentes
        if (headers != null) {
            String serviceScopes = headers.getFirst(IdentityHeaders.SERVICE_SCOPES);
            if (serviceScopes != null && Arrays.stream(serviceScopes.split("[,\\s]+"))
                    .anyMatch(requiredScope::equals)) {
                return;
            }
        }

        // 3. Si no cuenta con el scope requerido ni token válido
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing scope: " + requiredScope);
    }
}
