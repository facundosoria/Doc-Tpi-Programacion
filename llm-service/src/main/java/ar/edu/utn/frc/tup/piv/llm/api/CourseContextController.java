package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.configuration.WorkbenchDemoCatalog;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Course context is gateway-derived; it deliberately does not trust browser state. */
@RestController
@RequestMapping("/api/llm/courses")
public class CourseContextController {
  private static final String TEACHER_COURSES_HEADER = "X-Teacher-Course-Ids";
  private final GoldenSetAuthorization authorization;
  private final WorkbenchDemoCatalog workbenchCatalog;

  public CourseContextController(GoldenSetAuthorization authorization, WorkbenchDemoCatalog workbenchCatalog) {
    this.authorization = authorization; this.workbenchCatalog = workbenchCatalog;
  }

  @GetMapping
  public CoursePage list(@RequestHeader HttpHeaders headers) {
    CallerIdentity actor = authorization.require(headers);
    List<CourseSummary> demoCourses = workbenchCatalog.coursesFor(actor.delegatedUserId()).stream()
        .map(course -> new CourseSummary(course.id(), course.name(), course.subtitle())).toList();
    if (!demoCourses.isEmpty()) return new CoursePage(demoCourses);
    List<CourseSummary> items = Arrays.stream(headers.getFirst(TEACHER_COURSES_HEADER) == null ? new String[0]
            : headers.getFirst(TEACHER_COURSES_HEADER).split("[\\s,]+"))
        .filter(value -> !value.isBlank()).map(this::course).flatMap(java.util.Optional::stream).toList();
    return new CoursePage(items);
  }

  private java.util.Optional<CourseSummary> course(String value) {
    try { return java.util.Optional.of(new CourseSummary(UUID.fromString(value), "Curso autorizado", null)); }
    catch (IllegalArgumentException ignored) { return java.util.Optional.empty(); }
  }

  public record CoursePage(List<CourseSummary> items) {}
  public record CourseSummary(UUID id, String name, String subtitle) {}
}
