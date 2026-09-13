package ar.edu.utn.frc.tup.piv.llm.security;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import ar.edu.utn.frc.tup.piv.llm.configuration.WorkbenchDemoCatalog;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

/** Authorizes course-scoped teacher operations from gateway-propagated claims. */
@Component
public class CourseAuthorization {
  private static final String ROLES_HEADER = "X-User-Roles";
  private static final String TEACHER_COURSES_HEADER = "X-Teacher-Course-Ids";
  @Value("${llm.workbench.enabled:false}") private boolean workbench;
  @Value("${llm.workbench.user-id:11111111-1111-1111-1111-111111111111}") private UUID workbenchUser;
  private final WorkbenchDemoCatalog workbenchCatalog;

  public CourseAuthorization(WorkbenchDemoCatalog workbenchCatalog) { this.workbenchCatalog = workbenchCatalog; }

  public void requireTeacher(UUID courseId, CallerIdentity actor, HttpHeaders headers) {
    if (workbench && actor != null && "workbench".equals(actor.serviceId()) && workbenchUser.equals(actor.delegatedUserId())
        && workbenchCatalog.canManage(courseId, actor.delegatedUserId())) return;
    requireTeacher(courseId, actor == null ? null : actor.delegatedUserId(), roles(headers), teacherCourses(headers));
  }

  public void requireTeacher(UUID courseId, UUID actorId, Set<String> roles, Set<UUID> teacherCourses) {
    boolean isTeacher = roles != null && roles.stream().map(String::toUpperCase)
        .anyMatch(role -> role.equals("TEACHER") || role.equals("DOCENTE") || role.equals("ROLE_TEACHER"));
    if (courseId == null || actorId == null || teacherCourses == null || !isTeacher || !teacherCourses.contains(courseId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El actor no puede administrar este curso");
    }
  }

  /** Kept for callers that already resolved the teacher role upstream. */
  public void requireTeacher(UUID courseId, UUID actorId, Set<UUID> teacherCourses) {
    requireTeacher(courseId, actorId, Set.of("TEACHER"), teacherCourses);
  }

  private Set<String> roles(HttpHeaders headers) { return split(headers == null ? null : headers.getFirst(ROLES_HEADER)); }

  private Set<UUID> teacherCourses(HttpHeaders headers) {
    try {
      return split(headers == null ? null : headers.getFirst(TEACHER_COURSES_HEADER)).stream()
          .map(UUID::fromString).collect(Collectors.toUnmodifiableSet());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Asignaciones de curso inválidas");
    }
  }

  private Set<String> split(String value) {
    if (value == null || value.isBlank()) return Set.of();
    return Arrays.stream(value.split("[\\s,]+", -1)).filter(token -> !token.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }
}
