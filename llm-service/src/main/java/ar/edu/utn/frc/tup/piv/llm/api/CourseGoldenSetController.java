package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.CourseGoldenSetService;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseGoldenSetRepository.CourseGoldenSetVersion;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/llm/courses/{courseId}/golden-sets")
public class CourseGoldenSetController {
  private final CourseGoldenSetService service;
  private final GoldenSetAuthorization authorization;
  private final CourseAuthorization courseAuthorization;
  public CourseGoldenSetController(CourseGoldenSetService service, GoldenSetAuthorization authorization, CourseAuthorization courseAuthorization) {
    this.service = service; this.authorization = authorization; this.courseAuthorization = courseAuthorization;
  }

  @PostMapping("/copy-from-base/{baseVersionId}")
  public ResponseEntity<CourseGoldenSetVersion> copyFromBase(@PathVariable UUID courseId, @PathVariable UUID baseVersionId,
      @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    var copy = service.copyFromPublishedBase(courseId, baseVersionId, actor);
    return ResponseEntity.created(URI.create("/api/llm/courses/" + courseId + "/golden-sets/" + copy.id())).body(copy);
  }

  @GetMapping
  public GoldenSetPage list(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    return new GoldenSetPage(service.list(courseId));
  }

  @PostMapping
  public ResponseEntity<CourseGoldenSetVersion> create(@PathVariable UUID courseId, @RequestBody CreateGoldenSetDraft request,
      @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    var created = service.createDraft(courseId, request.name(), actor);
    return ResponseEntity.created(URI.create("/api/llm/courses/" + courseId + "/golden-sets/" + created.id())).body(created);
  }

  @PostMapping("/{versionId}/cases")
  public ResponseEntity<ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseGoldenSetRepository.GoldenSetCaseSummary> addCase(@PathVariable UUID courseId, @PathVariable UUID versionId, @RequestBody ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseGoldenSetRepository.GoldenSetCaseInput input, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers); courseAuthorization.requireTeacher(courseId, actor, headers);
    var created = service.addCase(courseId, versionId, input, actor);
    return ResponseEntity.created(URI.create("/api/llm/courses/" + courseId + "/golden-sets/" + versionId + "/cases/" + created.id())).body(created);
  }

  @GetMapping("/{versionId}")
  public ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseGoldenSetRepository.GoldenSetDetail get(
      @PathVariable UUID courseId, @PathVariable UUID versionId, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers); courseAuthorization.requireTeacher(courseId, actor, headers);
    return service.get(courseId, versionId);
  }

  @PatchMapping("/{versionId}/cases/{caseId}")
  public ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseGoldenSetRepository.GoldenSetCaseSummary updateCase(
      @PathVariable UUID courseId, @PathVariable UUID versionId, @PathVariable UUID caseId,
      @RequestBody ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseGoldenSetRepository.GoldenSetCaseInput input,
      @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers); courseAuthorization.requireTeacher(courseId, actor, headers);
    return service.updateCase(courseId, versionId, caseId, input, actor);
  }

  @PostMapping("/{versionId}/publish")
  public ResponseEntity<Void> publish(@PathVariable UUID courseId, @PathVariable UUID versionId, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers); courseAuthorization.requireTeacher(courseId, actor, headers);
    service.publish(courseId, versionId, actor);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{versionId}/next-version")
  public ResponseEntity<CourseGoldenSetVersion> createNextVersion(@PathVariable UUID courseId, @PathVariable UUID versionId, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers); courseAuthorization.requireTeacher(courseId, actor, headers);
    var created = service.createNextVersion(courseId, versionId, actor);
    return ResponseEntity.created(URI.create("/api/llm/courses/" + courseId + "/golden-sets/" + created.id())).body(created);
  }

  @DeleteMapping("/{versionId}")
  public ResponseEntity<Void> deleteUnusedDraft(@PathVariable UUID courseId, @PathVariable UUID versionId, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers); courseAuthorization.requireTeacher(courseId, actor, headers);
    service.deleteUnusedDraft(courseId, versionId, actor);
    return ResponseEntity.noContent().build();
  }

  public record GoldenSetPage(List<ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CourseGoldenSetRepository.CourseGoldenSetView> items) {}
  public record CreateGoldenSetDraft(String name) {}
}
