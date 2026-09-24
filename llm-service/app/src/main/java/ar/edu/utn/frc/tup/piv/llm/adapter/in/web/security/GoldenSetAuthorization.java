package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;

import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final String institutionalRequiredScope;

  @Autowired
  public GoldenSetAuthorization(@Value("${llm.gateway.trusted-service}") String trustedService,
      @Value("${llm.gateway.required-scope}") String requiredScope,
      @Value("${llm.gateway.template-required-scope}") String templateRequiredScope,
      @Value("${llm.gateway.institutional-required-scope}") String institutionalRequiredScope) {
    this.trustedService = trustedService; this.requiredScope = requiredScope; this.templateRequiredScope = templateRequiredScope; this.institutionalRequiredScope = institutionalRequiredScope;
  }

  public GoldenSetAuthorization(String trustedService, String requiredScope, String templateRequiredScope) {
    this(trustedService, requiredScope, templateRequiredScope, "llm.institutional-calibration.manage");
  }

  /**
   * Conveniencia para tests. El modo workbench que traía `dev` (identidad simulada sin headers)
   * se retiró en la integración de 2026-09-21: el laboratorio ahora pasa por `gateway-mock`, que
   * inyecta headers de identidad reales, así que el servicio ya no tiene un bypass de autorización.
   */
  public GoldenSetAuthorization(String trustedService, String requiredScope) {
    this(trustedService, requiredScope, "llm:evaluator:template:manage");
  }

  public CallerIdentity requireInstitutionalManager(HttpHeaders headers) {
    String roles = headers.getFirst("X-User-Roles");
    if (roles == null || Arrays.stream(roles.split(",")).map(String::trim).noneMatch("ADMIN"::equals)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Requiere rol ADMIN");
    }
    return requireScope(headers, institutionalRequiredScope);
  }

  public CallerIdentity require(HttpHeaders headers) {
    return requireScope(headers, requiredScope);
  }

  /** Platform templates affect every future course and therefore need their own ADMIN scope. */
  public CallerIdentity requireTemplateManager(HttpHeaders headers) {
    return requireScope(headers, templateRequiredScope);
  }

  private CallerIdentity requireScope(HttpHeaders headers, String scope) {
    String serviceId = headers.getFirst("X-Service-Id");
    String scopes = headers.getFirst("X-Service-Scopes");
    String delegated = headers.getFirst("X-Delegated-User");
    if (!trustedService.equals(serviceId) || scopes == null
        || Arrays.stream(scopes.split("\\s+")).noneMatch(scope::equals)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Falta el permiso requerido");
    }
    if (delegated == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidad delegada ausente");
    }
    try {
      return new CallerIdentity(serviceId, UUID.fromString(delegated), headers.getFirst("X-Request-Id"), headers.getFirst("traceparent"));
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidad delegada inválida");
    }
  }
}
