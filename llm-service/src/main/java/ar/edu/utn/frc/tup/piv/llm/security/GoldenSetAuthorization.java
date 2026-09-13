package ar.edu.utn.frc.tup.piv.llm.security;

import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Gateway identity validation shared by the course-scoped V2 evaluator APIs. */
@Component
public class GoldenSetAuthorization {
  private final String trustedService;
  private final String requiredScope;
  private final String templateRequiredScope;
  private final boolean workbench;
  private final UUID workbenchUser;

  public GoldenSetAuthorization(@Value("${llm.gateway.trusted-service}") String trustedService,
      @Value("${llm.gateway.required-scope}") String requiredScope,
      @Value("${llm.gateway.template-required-scope}") String templateRequiredScope,
      @Value("${llm.workbench.enabled:false}") boolean workbench,
      @Value("${llm.workbench.user-id:11111111-1111-1111-1111-111111111111}") UUID workbenchUser) {
    this.trustedService = trustedService; this.requiredScope = requiredScope; this.templateRequiredScope = templateRequiredScope;
    this.workbench = workbench; this.workbenchUser = workbenchUser;
  }

  public CallerIdentity require(HttpHeaders headers) {
    return requireScope(headers, requiredScope);
  }

  /** Platform templates affect every future course and therefore need their own ADMIN scope. */
  public CallerIdentity requireTemplateManager(HttpHeaders headers) {
    return requireScope(headers, templateRequiredScope);
  }

  private CallerIdentity requireScope(HttpHeaders headers, String scope) {
    if (workbench) return new CallerIdentity("workbench", workbenchUser, headers.getFirst("X-Request-Id"), headers.getFirst("traceparent"));
    String serviceId = headers.getFirst("X-Service-Id");
    String scopes = headers.getFirst("X-Service-Scopes");
    String delegated = headers.getFirst("X-Delegated-User");
    if (!trustedService.equals(serviceId) || scopes == null || Arrays.stream(scopes.split("\\s+")).noneMatch(scope::equals) || delegated == null)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El actor no puede administrar el evaluador");
    try { return new CallerIdentity(serviceId, UUID.fromString(delegated), headers.getFirst("X-Request-Id"), headers.getFirst("traceparent")); }
    catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidad delegada inválida"); }
  }
}
