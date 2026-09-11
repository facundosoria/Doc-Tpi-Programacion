package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService;
import ar.edu.utn.frc.tup.piv.llm.application.RubricPublicationService;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/llm/courses/{courseId}/rubrics")
public class RubricController {
  private final RubricPublicationService publicationService;
  private final RubricDraftService draftService;
  private final GoldenSetAuthorization authorization;
  private final CourseAuthorization courseAuthorization;

  public RubricController(RubricPublicationService publicationService, RubricDraftService draftService,
      GoldenSetAuthorization authorization, CourseAuthorization courseAuthorization) {
    this.publicationService = publicationService;
    this.draftService = draftService;
    this.authorization = authorization;
    this.courseAuthorization = courseAuthorization;
  }

  @GetMapping
  public RubricPage list(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) {
    authorize(courseId, headers);
    return new RubricPage(draftService.list(courseId));
  }

  @GetMapping("/{versionId}")
  public RubricDraftService.RubricVersion get(@PathVariable UUID courseId, @PathVariable UUID versionId,
      @RequestHeader HttpHeaders headers) {
    authorize(courseId, headers);
    return draftService.get(courseId, versionId);
  }

  @PostMapping
  public ResponseEntity<RubricDraftService.RubricVersion> create(@PathVariable UUID courseId,
      @Valid @RequestBody CreateFromTemplateRequest input, @RequestHeader HttpHeaders headers) {
    var actor = authorize(courseId, headers);
    var created = draftService.createFromTemplate(courseId, input.templateVersionId(), input.name(), actor);
    return ResponseEntity.created(java.net.URI.create("/api/llm/courses/" + courseId + "/rubrics/" + created.id())).body(created);
  }

  public record CreateFromTemplateRequest(
      @NotNull UUID templateVersionId,
      @NotBlank @Size(max = 160) String name) {}

  @PatchMapping("/{versionId}")
  public RubricDraftService.RubricVersion autosave(@PathVariable UUID courseId, @PathVariable UUID versionId,
      @RequestHeader("If-Match") long revision, @Valid @RequestBody RubricDraftService.RubricInput input,
      @RequestHeader HttpHeaders headers) {
    var actor = authorize(courseId, headers);
    return draftService.autosave(courseId, versionId, revision, input, actor);
  }

  @PostMapping("/{versionId}/next-version")
  public ResponseEntity<RubricDraftService.RubricVersion> createNextVersion(@PathVariable UUID courseId, @PathVariable UUID versionId,
      @RequestHeader HttpHeaders headers) {
    var actor = authorize(courseId, headers);
    var created = draftService.createNextVersion(courseId, versionId, actor);
    return ResponseEntity.created(java.net.URI.create("/api/llm/courses/" + courseId + "/rubrics/" + created.id())).body(created);
  }

  @PostMapping("/{versionId}/publish")
  public ResponseEntity<Void> publish(@PathVariable UUID courseId, @PathVariable UUID versionId,
      @RequestHeader HttpHeaders headers) {
    var actor = authorize(courseId, headers);
    publicationService.publish(courseId, versionId, actor);
    return ResponseEntity.noContent().build();
  }

  private ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity authorize(UUID courseId, HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    return actor;
  }

  public record RubricPage(List<RubricDraftService.RubricVersion> items) {}
}
