package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseEvaluationStatusRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Status for AI-usage evaluations only; this controller never exposes academic grading. */
@RestController
@RequestMapping("/api/llm/courses/{courseId}")
public class CourseEvaluationStatusController {
  private final CourseEvaluationStatusRepository status;
  private final GoldenSetAuthorization identity;
  private final CourseAuthorization courses;
  public CourseEvaluationStatusController(CourseEvaluationStatusRepository status, GoldenSetAuthorization identity, CourseAuthorization courses) { this.status = status; this.identity = identity; this.courses = courses; }
  @GetMapping("/active-calibration") public ResponseEntity<CourseEvaluationStatusRepository.ActiveCalibration> activeCalibration(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) { authorize(courseId, headers); return status.activeCalibration(courseId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build()); }
  @GetMapping("/challenge-calibration-assignments") public AssignmentPage assignments(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) { authorize(courseId, headers); return new AssignmentPage(status.assignments(courseId)); }
  @GetMapping("/pending-evaluations") public PendingEvaluationPage pendingEvaluations(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) { authorize(courseId, headers); return new PendingEvaluationPage(status.pendingEvaluations(courseId)); }
  private void authorize(UUID courseId, HttpHeaders headers) { var actor = identity.require(headers); courses.requireTeacher(courseId, actor, headers); }
  public record AssignmentPage(List<CourseEvaluationStatusRepository.ChallengeAssignment> items) {}
  public record PendingEvaluationPage(List<CourseEvaluationStatusRepository.PendingEvaluation> items) {}
}
