package ar.edu.utn.frc.tup.piv.llm.security;

import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Validación de identidad gateway→servicio para el tutor (`POST /api/llm/tutor/interactions`).
 * Mismo patrón que {@link GoldenSetAuthorization} (M2M: servicio + scope + usuario delegado) pero
 * con su propia property — el tutor no comparte el scope de administración del golden set. No se
 * edita `GoldenSetAuthorization` para no tocar código de otro dueño ([[no-tocar-codigo-ajeno]]). */
@Component
public class TutorGatewayAuthorization {
  private final String trustedService;
  private final String requiredScope;
  private final boolean workbench;
  private final UUID workbenchUser;

  public TutorGatewayAuthorization(@Value("${llm.tutor.trusted-service}") String trustedService,
      @Value("${llm.tutor.required-scope}") String requiredScope,
      @Value("${llm.workbench.enabled:false}") boolean workbench,
      @Value("${llm.workbench.user-id:11111111-1111-1111-1111-111111111111}") UUID workbenchUser) {
    this.trustedService = trustedService;
    this.requiredScope = requiredScope;
    this.workbench = workbench;
    this.workbenchUser = workbenchUser;
  }

  public CallerIdentity require(HttpHeaders headers) {
    if (workbench) {
      return new CallerIdentity("workbench", workbenchUser, headers.getFirst("X-Request-Id"), headers.getFirst("traceparent"));
    }
    String serviceId = headers.getFirst("X-Service-Id");
    String scopes = headers.getFirst("X-Service-Scopes");
    String delegated = headers.getFirst("X-Delegated-User");
    if (!trustedService.equals(serviceId) || scopes == null
        || Arrays.stream(scopes.split("\\s+")).noneMatch(requiredScope::equals) || delegated == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El actor no puede invocar al tutor");
    }
    try {
      return new CallerIdentity(serviceId, UUID.fromString(delegated), headers.getFirst("X-Request-Id"), headers.getFirst("traceparent"));
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidad delegada inválida");
    }
  }
}
