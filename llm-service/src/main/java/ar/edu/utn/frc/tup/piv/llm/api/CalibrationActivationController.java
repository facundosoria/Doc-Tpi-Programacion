package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.CalibrationActivationPreviewService;
import ar.edu.utn.frc.tup.piv.llm.application.CalibrationActivationService;
import ar.edu.utn.frc.tup.piv.llm.application.CalibrationMigrationConfirmation;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseEvaluationStatusRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Explicit two-step activation prevents unreviewed migration of course challenges. */
@RestController
@RequestMapping("/api/llm/courses/{courseId}/calibrations/{runId}")
public class CalibrationActivationController {
  private final CalibrationActivationPreviewService previews;
  private final CalibrationMigrationConfirmation confirmations;
  private final CalibrationActivationService activation;
  private final CourseEvaluationStatusRepository status;
  private final GoldenSetAuthorization identity;
  private final CourseAuthorization courses;

  public CalibrationActivationController(CalibrationActivationPreviewService previews,
      CalibrationMigrationConfirmation confirmations, CalibrationActivationService activation,
      CourseEvaluationStatusRepository status, GoldenSetAuthorization identity, CourseAuthorization courses) {
    this.previews = previews; this.confirmations = confirmations; this.activation = activation;
    this.status = status; this.identity = identity; this.courses = courses;
  }

  @PostMapping("/activate-preview")
  public ActivationPreview preview(@PathVariable UUID courseId, @PathVariable UUID runId,
      @RequestHeader HttpHeaders headers) {
    authorize(courseId, headers);
    var preview = previews.preview(courseId, runId);
    String token = confirmations.issue(courseId, runId, Set.copyOf(preview.migrable()));
    return new ActivationPreview(token, preview.migrable(), preview.locked());
  }

  @PostMapping("/activate")
  public ResponseEntity<CourseEvaluationStatusRepository.ActiveCalibration> activate(
      @PathVariable UUID courseId, @PathVariable UUID runId, @RequestBody ActivationInput input,
      @RequestHeader HttpHeaders headers) {
    CallerIdentity actor = authorize(courseId, headers);
    activation.activateAndMigrate(courseId, runId, actor, input.previewToken(), input.challengeIds());
    return status.activeCalibration(courseId).map(ResponseEntity::ok)
        .orElseThrow(() -> new IllegalStateException("No se pudo recuperar la calibración activada"));
  }

  private CallerIdentity authorize(UUID courseId, HttpHeaders headers) {
    var actor = identity.require(headers); courses.requireTeacher(courseId, actor, headers); return actor;
  }
  public record ActivationPreview(String previewToken, List<UUID> migrableChallengeIds, List<UUID> lockedChallengeIds) {}
  public record ActivationInput(String previewToken, Set<UUID> challengeIds) {}
}
