package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;

import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Validación de identidad gateway→servicio para RAG (`/api/llm/rag/**`). Mismo patrón M2M que
 * {@link TutorGatewayAuthorization}/{@link GoldenSetAuthorization}, pero con su propia property:
 * RAG es una épica distinta (EP-09) del tutor (EP-05) — un scope propio evita acoplar permisos de
 * una funcionalidad a la otra (decisión tomada al planificar el port de `demoLLMSpringAi`). No se
 * edita `TutorGatewayAuthorization` para no tocar código de otro dueño ([[no-tocar-codigo-ajeno]]). */
@Component
public class RagGatewayAuthorization {
  private final String trustedService;
  private final String requiredScope;
  private final boolean workbench;
  private final UUID workbenchUser;

  public RagGatewayAuthorization(@Value("${llm.rag.trusted-service}") String trustedService,
      @Value("${llm.rag.required-scope}") String requiredScope,
      @Value("${llm.workbench.enabled:false}") boolean workbench,
      @Value("${llm.workbench.user-id:11111111-1111-1111-1111-111111111111}") UUID workbenchUser) {
    this.trustedService = trustedService;
    this.requiredScope = requiredScope;
    this.workbench = workbench;
    this.workbenchUser = workbenchUser;
  }

  public CallerIdentity require(HttpHeaders headers) {
    String serviceId = headers.getFirst("X-Service-Id");
    // Defensa en profundidad: el modo workbench solo se confia para peticiones locales que NO
    // traen identidad M2M del gateway. Si la peticion si trae `X-Service-Id`, se valida como
    // cualquier otra: un workbench habilitado por error en un despliegue no abre /rag/** a un
    // servicio que no presente el scope requerido.
    if (workbench && serviceId == null) {
      return new CallerIdentity("workbench", workbenchUser, headers.getFirst("X-Request-Id"), headers.getFirst("traceparent"));
    }
    String scopes = headers.getFirst("X-Service-Scopes");
    String delegated = headers.getFirst("X-Delegated-User");
    if (!trustedService.equals(serviceId) || scopes == null
        || Arrays.stream(scopes.split("\\s+")).noneMatch(requiredScope::equals)) {
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
