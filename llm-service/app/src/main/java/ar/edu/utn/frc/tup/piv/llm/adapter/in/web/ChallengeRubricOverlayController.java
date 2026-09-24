package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.service.ChallengeRubricOverlayService;
import ar.edu.utn.frc.tup.piv.llm.application.service.EffectiveRubricResolver;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.private-path}/courses/{courseId}/challenges/{challengeId}/rubric-overlay")
public class ChallengeRubricOverlayController {
  private final ChallengeRubricOverlayService overlayService;
  private final GoldenSetAuthorization authorization;
  private final CourseAuthorization courseAuthorization;

  public ChallengeRubricOverlayController(ChallengeRubricOverlayService overlayService,
      GoldenSetAuthorization authorization, CourseAuthorization courseAuthorization) {
    this.overlayService = overlayService;
    this.authorization = authorization;
    this.courseAuthorization = courseAuthorization;
  }

  @GetMapping
  public ChallengeOverlayPage list(@PathVariable UUID courseId, @PathVariable UUID challengeId,
      @RequestHeader HttpHeaders headers) {
    authorize(courseId, headers);
    return new ChallengeOverlayPage(overlayService.listByChallenge(courseId, challengeId));
  }

  @GetMapping("/{versionId}")
  public ChallengeRubricOverlayService.ChallengeOverlayVersion get(@PathVariable UUID courseId,
      @PathVariable UUID challengeId, @PathVariable UUID versionId, @RequestHeader HttpHeaders headers) {
    authorize(courseId, headers);
    return overlayService.get(courseId, challengeId, versionId);
  }

  @PostMapping
  public ResponseEntity<ChallengeRubricOverlayService.ChallengeOverlayVersion> create(@PathVariable UUID courseId,
      @PathVariable UUID challengeId, @Valid @RequestBody CreateOverlayRequest input,
      @RequestHeader HttpHeaders headers) {
    var actor = authorize(courseId, headers);
    var created = overlayService.createDraft(courseId, challengeId, input.name(), input.baselineVersionId(), actor);
    return ResponseEntity.created(java.net.URI.create("/api/llm/courses/" + courseId + "/challenges/" + challengeId
        + "/rubric-overlay/" + created.id())).body(created);
  }

  public record CreateOverlayRequest(
      @NotBlank @Size(max = 160) String name,
      @NotNull UUID baselineVersionId) {}

  @PatchMapping("/{versionId}")
  public ChallengeRubricOverlayService.ChallengeOverlayVersion autosave(@PathVariable UUID courseId,
      @PathVariable UUID challengeId, @PathVariable UUID versionId,
      @RequestHeader("If-Match") long revision,
      @Valid @RequestBody ChallengeRubricOverlayService.OverlayInput input,
      @RequestHeader HttpHeaders headers) {
    var actor = authorize(courseId, headers);
    return overlayService.autosave(courseId, challengeId, versionId, revision, input, actor);
  }

  @PostMapping("/{versionId}/publish")
  public ResponseEntity<Void> publish(@PathVariable UUID courseId, @PathVariable UUID challengeId,
      @PathVariable UUID versionId, @RequestHeader HttpHeaders headers) {
    var actor = authorize(courseId, headers);
    overlayService.publish(courseId, challengeId, versionId, actor);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{versionId}/next-version")
  public ResponseEntity<ChallengeRubricOverlayService.ChallengeOverlayVersion> createNextVersion(
      @PathVariable UUID courseId, @PathVariable UUID challengeId, @PathVariable UUID versionId,
      @RequestHeader HttpHeaders headers) {
    var actor = authorize(courseId, headers);
    var created = overlayService.createNextVersion(courseId, challengeId, versionId, actor);
    return ResponseEntity.created(java.net.URI.create("/api/llm/courses/" + courseId + "/challenges/" + challengeId
        + "/rubric-overlay/" + created.id())).body(created);
  }

  @GetMapping("/{versionId}/effective")
  public EffectiveProfileResponse effective(@PathVariable UUID courseId, @PathVariable UUID challengeId,
      @PathVariable UUID versionId, @RequestHeader HttpHeaders headers) {
    authorize(courseId, headers);
    return new EffectiveProfileResponse(overlayService.getEffectiveProfile(courseId, challengeId, versionId));
  }

  private CallerIdentity authorize(UUID courseId, HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    return actor;
  }

  public record ChallengeOverlayPage(List<ChallengeRubricOverlayService.ChallengeOverlayVersion> items) {}

  public record EffectiveProfileResponse(List<EffectiveRubricResolver.EffectiveDimension> dimensions) {}
}