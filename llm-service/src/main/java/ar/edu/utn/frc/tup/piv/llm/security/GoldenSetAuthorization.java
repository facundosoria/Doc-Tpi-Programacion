package ar.edu.utn.frc.tup.piv.llm.security;

import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Component
public class GoldenSetAuthorization {
  private final String trustedService;
  private final String requiredScope;
  private final boolean workbench;
  private final UUID workbenchUser;

  public GoldenSetAuthorization(
      @Value("${llm.gateway.trusted-service}") String trustedService,
      @Value("${llm.gateway.required-scope}") String requiredScope,
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
    if (!trustedService.equals(serviceId) || scopes == null || Arrays.stream(scopes.split("\\s+")).noneMatch(requiredScope::equals) || delegated == null) {
      throw new ResponseStatusException(FORBIDDEN, "El actor no puede administrar golden sets");
    }
    try {
      return new CallerIdentity(serviceId, UUID.fromString(delegated), headers.getFirst("X-Request-Id"), headers.getFirst("traceparent"));
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(FORBIDDEN, "Identidad delegada inválida");
    }
  }
}
