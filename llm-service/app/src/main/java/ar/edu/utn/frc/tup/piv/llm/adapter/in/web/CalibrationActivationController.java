package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationActivationPreviewService;
import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationActivationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationMigrationConfirmation;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseEvaluationStatusRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.application.service.CourseEvaluationStatusService;
import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.ActiveCalibration;
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
@RequestMapping("${app.api.private-path}/courses/{courseId}/calibrations/{runId}")
public class CalibrationActivationController {
  private final CalibrationActivationPreviewService previews;
  private final CalibrationMigrationConfirmation confirmations;
  private final CalibrationActivationService activation;
  private final CourseEvaluationStatusService status;
  private final GoldenSetAuthorization identity;
  private final CourseAuthorization courses;

  public CalibrationActivationController(CalibrationActivationPreviewService previews,
      CalibrationMigrationConfirmation confirmations, CalibrationActivationService activation,
      CourseEvaluationStatusService status, GoldenSetAuthorization identity, CourseAuthorization courses) {
    this.previews = previews;
    this.confirmations = confirmations;
    this.activation = activation;
    this.status = status;
    this.identity = identity;
    this.courses = courses;
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
  public ResponseEntity<ActiveCalibration> activate(
      @PathVariable UUID courseId, @PathVariable UUID runId, @RequestBody ActivationInput input,
      @RequestHeader HttpHeaders headers) {
    CallerIdentity actor = authorize(courseId, headers);
    activation.activateAndMigrate(courseId, runId, actor, input.previewToken(), input.challengeIds());
    return status.activeCalibration(courseId).map(ResponseEntity::ok)
        .orElseThrow(() -> new IllegalStateException("No se pudo recuperar la calibración activada"));
  }

  private CallerIdentity authorize(UUID courseId, HttpHeaders headers) {
    var actor = identity.require(headers);
    courses.requireTeacher(courseId, actor, headers);
    return actor;
  }

  public record ActivationPreview(String previewToken, List<UUID> migrableChallengeIds, List<UUID> lockedChallengeIds) {}
  public record ActivationInput(String previewToken, Set<UUID> challengeIds) {}
}
