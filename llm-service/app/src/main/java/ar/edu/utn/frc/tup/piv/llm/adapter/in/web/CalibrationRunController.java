package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationRunService;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository.Run;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.private-path}/courses/{courseId}/calibrations")
public class CalibrationRunController {
  private final CalibrationRunService service;
  private final GoldenSetAuthorization authorization;
  private final CourseAuthorization courseAuthorization;
  private final ProviderCredentialRepository deployments;

  public CalibrationRunController(CalibrationRunService service, GoldenSetAuthorization authorization,
      CourseAuthorization courseAuthorization, ProviderCredentialRepository deployments) {
    this.service = service;
    this.authorization = authorization;
    this.courseAuthorization = courseAuthorization;
    this.deployments = deployments;
  }

  @GetMapping
  public CalibrationPage list(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) {
    authorize(courseId, headers);
    return new CalibrationPage(service.list(courseId));
  }

  @GetMapping("/stability-groups")
  public StabilityPage groups(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) {
    authorize(courseId, headers);
    return new StabilityPage(service.stabilityGroups(courseId));
  }

  @PostMapping
  public ResponseEntity<Run> create(@PathVariable UUID courseId, @RequestBody Request request,
      @RequestHeader("Idempotency-Key") UUID idempotencyKey, @RequestHeader HttpHeaders headers) {
    var actor = authorize(courseId, headers);
    UUID target = deployments.calibrationTarget().map(ProviderCredentialRepository.Deployment::id)
        .orElseThrow(() -> new IllegalStateException(
            "El administrador debe seleccionar un modelo candidato para calibrar"));
    return ResponseEntity.accepted().body(service.enqueue(courseId, request.rubricVersionId(),
        request.goldenSetVersionId(), target, idempotencyKey, actor));
  }

  @GetMapping("/{runId}")
  public Run get(@PathVariable UUID courseId, @PathVariable UUID runId,
      @RequestHeader HttpHeaders headers) {
    authorize(courseId, headers);
    return service.get(courseId, runId);
  }

  private CallerIdentity authorize(UUID courseId, HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    return actor;
  }

  /** The candidate deployment is server-selected; callers cannot override it. */
  public record Request(UUID rubricVersionId, UUID goldenSetVersionId) {}
  public record CalibrationPage(List<Run> items) {}
  public record StabilityPage(List<ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository.StabilityGroup> items) {}
}
