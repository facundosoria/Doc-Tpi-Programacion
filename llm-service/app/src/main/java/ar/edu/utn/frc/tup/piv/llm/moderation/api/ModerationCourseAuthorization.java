package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.configuration.IdentityHeaders;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validador de autorización por curso para la supervisión docente de moderación (LLM-S12-H02 / T3).
 * Exige rol docente y asignación explícita al course_id (403 Forbidden ante cursos ajenos).
 */
@Component
public class ModerationCourseAuthorization {

    private static final String TEACHER_COURSES_HEADER = "X-Teacher-Course-Ids";

    @Value("${llm.workbench.enabled:false}")
    private boolean workbench;

    /** Mismo flag que GatewayIdentityFilter: solo dev/tests. Con false, un Bearer sin firma nunca aporta identidad, roles ni cursos. */
    @Value("${app.security.trust-unsigned-bearer:false}")
    private boolean trustUnsignedBearer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void setTrustUnsignedBearer(boolean trustUnsignedBearer) {
        this.trustUnsignedBearer = trustUnsignedBearer;
    }

    private boolean isTrustedBearer(String authHeader) {
        return trustUnsignedBearer && authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7);
    }

    /**
     * Valida que el llamador autenticado sea docente del curso especificado.
     * Si no cumple, lanza ResponseStatusException con 403 FORBIDDEN y detalle específico.
     */
    public void requireTeacherCourse(String courseId, HttpHeaders headers) {
        if (courseId == null || courseId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "course_id es obligatorio");
        }

        if (workbench) {
            return;
        }

        String userId = resolveUserId(headers);
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        Set<String> roles = extractRoles(headers);
        boolean isTeacher = roles.stream().anyMatch(this::isTeacherRole);
        if (!isTeacher) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No sos docente del curso " + courseId);
        }

        Set<String> assignedCourses = extractAssignedCourses(headers);
        if (!assignedCourses.contains(courseId.trim())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No sos docente del curso " + courseId);
        }
    }

    public String resolveUserId(HttpHeaders headers) {
        if (headers != null) {
            String headerUserId = headers.getFirst(IdentityHeaders.USER_ID);
            if (headerUserId != null && !headerUserId.isBlank()) {
                return headerUserId.trim();
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getName() != null
                && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            return auth.getName().trim();
        }

        if (headers != null) {
            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (isTrustedBearer(authHeader)) {
                String sub = extractClaimFromJwt(authHeader.substring(7).trim(), "sub");
                if (sub != null && !sub.isBlank()) {
                    return sub.trim();
                }
            }
        }

        return null;
    }

    private boolean isTeacherRole(String role) {
        if (role == null) return false;
        String normalized = role.toUpperCase().trim();
        return normalized.equals("TEACHER")
                || normalized.equals("DOCENTE")
                || normalized.equals("PROFESSOR")
                || normalized.equals("ROLE_TEACHER")
                || normalized.equals("ROLE_DOCENTE")
                || normalized.equals("ROLE_PROFESSOR");
    }

    private Set<String> extractRoles(HttpHeaders headers) {
        Set<String> roles = new HashSet<>();

        if (headers != null) {
            String userRoles = headers.getFirst(IdentityHeaders.USER_ROLES);
            if (userRoles != null && !userRoles.isBlank()) {
                Arrays.stream(userRoles.split("[,\\s]+"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(roles::add);
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                roles.add(authority.getAuthority());
            }
        }

        if (headers != null) {
            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (isTrustedBearer(authHeader)) {
                roles.addAll(extractListFromJwt(authHeader.substring(7).trim(), "roles"));
            }
        }

        return roles;
    }

    private Set<String> extractAssignedCourses(HttpHeaders headers) {
        Set<String> courses = new HashSet<>();

        if (headers != null) {
            String coursesHeader = headers.getFirst(TEACHER_COURSES_HEADER);
            if (coursesHeader != null && !coursesHeader.isBlank()) {
                Arrays.stream(coursesHeader.split("[,\\s]+"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(courses::add);
            }
        }

        if (headers != null) {
            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (isTrustedBearer(authHeader)) {
                String token = authHeader.substring(7).trim();
                courses.addAll(extractListFromJwt(token, "teacher_course_ids"));
                courses.addAll(extractListFromJwt(token, "courses"));
                courses.addAll(extractListFromJwt(token, "course_ids"));
                String singleCourse = extractClaimFromJwt(token, "course_id");
                if (singleCourse != null && !singleCourse.isBlank()) {
                    courses.add(singleCourse.trim());
                }
            }
        }

        return courses;
    }

    private String extractClaimFromJwt(String token, String claim) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
                JsonNode node = objectMapper.readTree(decoded);
                if (node.has(claim)) {
                    return node.get(claim).asText();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Set<String> extractListFromJwt(String token, String claim) {
        Set<String> result = new HashSet<>();
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
                JsonNode node = objectMapper.readTree(decoded);
                if (node.has(claim)) {
                    JsonNode claimNode = node.get(claim);
                    if (claimNode.isArray()) {
                        for (JsonNode item : claimNode) {
                            result.add(item.asText().trim());
                        }
                    } else {
                        Arrays.stream(claimNode.asText().split("[,\\s]+"))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .forEach(result::add);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
