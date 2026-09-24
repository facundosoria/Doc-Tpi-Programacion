package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.ModerationRetentionPolicyService;
import java.util.Arrays;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador REST para configurar y consultar políticas de retención de evidencia de moderación (LLM-S13-H02 / CA4).
 * Expone PUT /api/v1/operations/retention-policy/moderation restringido a rol ADMIN.
 */
@RestController
public class ModerationRetentionPolicyController {

    @Value("${llm.workbench.enabled:false}")
    private boolean workbench;

    private final ModerationRetentionPolicyService policyService;

    public ModerationRetentionPolicyController(ModerationRetentionPolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping({
            "/api/v1/operations/retention-policy/moderation",
            "/operations/retention-policy/moderation",
            "${app.api.private-path:/api/llm}/operations/retention-policy/moderation"
    })
    public ResponseEntity<Map<String, Integer>> getRetentionPolicies() {
        return ResponseEntity.ok(policyService.getPolicies());
    }

    @PutMapping({
            "/api/v1/operations/retention-policy/moderation",
            "/operations/retention-policy/moderation",
            "${app.api.private-path:/api/llm}/operations/retention-policy/moderation"
    })
    public ResponseEntity<Map<String, Integer>> updateRetentionPolicies(
            @RequestBody Map<String, Object> body,
            @RequestHeader(required = false) HttpHeaders headers) {

        requireAdminRole(headers);
        String actor = resolveActor(headers);

        Map<String, Integer> updated = policyService.updatePolicies(body, actor);
        return ResponseEntity.ok(updated);
    }

    private void requireAdminRole(HttpHeaders headers) {
        if (workbench) {
            return;
        }

        if (headers != null) {
            String roles = headers.getFirst(IdentityHeaders.USER_ROLES);
            if (roles != null && hasAdminRole(roles)) {
                return;
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if (hasAdminRole(ga.getAuthority())) {
                    return;
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere rol ADMIN para configurar políticas de retención");
    }

    private boolean hasAdminRole(String roleStr) {
        if (roleStr == null) return false;
        return Arrays.stream(roleStr.split("[,\\s]+"))
                .anyMatch(r -> r.equalsIgnoreCase("ADMIN")
                        || r.equalsIgnoreCase("ROLE_ADMIN")
                        || r.equalsIgnoreCase("ADMINISTRATOR")
                        || r.equalsIgnoreCase("moderation:admin"));
    }

    private String resolveActor(HttpHeaders headers) {
        if (headers != null) {
            String userId = headers.getFirst(IdentityHeaders.USER_ID);
            if (userId != null && !userId.isBlank()) {
                return userId.trim();
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return auth.getName();
        }
        return "ADMIN";
    }
}
