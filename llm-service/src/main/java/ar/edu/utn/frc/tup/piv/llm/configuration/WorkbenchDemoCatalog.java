package ar.edu.utn.frc.tup.piv.llm.configuration;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Deliberately small substitute for the identity/course services in the local workbench profile.
 * It is disabled in every non-demo deployment; no Golden Set data belongs here.
 */
@Component
public class WorkbenchDemoCatalog {
  public record Course(UUID id, String name, String subtitle) {}

  private static final List<Course> DEMO_COURSES = List.of(
      new Course(UUID.fromString("00000000-0000-0000-0000-000000000010"), "Programación III", "Comisión A · 2026"),
      new Course(UUID.fromString("00000000-0000-0000-0000-000000000020"), "Paradigmas de Programación", "Comisión B · 2026"));

  private final boolean enabled;
  private final UUID teacherId;

  public WorkbenchDemoCatalog(@Value("${llm.workbench.enabled:false}") boolean enabled,
      @Value("${llm.workbench.user-id:11111111-1111-1111-1111-111111111111}") UUID teacherId) {
    this.enabled = enabled;
    this.teacherId = teacherId;
  }

  public List<Course> coursesFor(UUID actorId) { return enabled && teacherId.equals(actorId) ? DEMO_COURSES : List.of(); }
  public boolean canManage(UUID courseId, UUID actorId) {
    return coursesFor(actorId).stream().anyMatch(course -> course.id().equals(courseId));
  }
}
