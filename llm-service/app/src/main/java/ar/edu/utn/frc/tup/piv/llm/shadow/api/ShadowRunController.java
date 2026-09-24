package ar.edu.utn.frc.tup.piv.llm.shadow.api;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.shadow.application.ShadowRunService;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
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

/** E-31: pedir y consultar el shadow del evaluador de un curso. Solo docentes del curso. El resultado
 * es informativo: nada de acá activa una rúbrica ni emite un score. */
@RestController
@RequestMapping("${app.api.private-path}/courses/{courseId}/shadow-runs")
public class ShadowRunController {
  private final ShadowRunService service;
  private final GoldenSetAuthorization authorization;
  private final CourseAuthorization courses;

  public ShadowRunController(ShadowRunService service, GoldenSetAuthorization authorization, CourseAuthorization courses) {
    this.service = service;
    this.authorization = authorization;
    this.courses = courses;
  }

  @GetMapping
  public Page list(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) {
    auth(courseId, headers);
    return new Page(service.list(courseId));
  }

  @PostMapping
  public ResponseEntity<ShadowRun> create(@PathVariable UUID courseId, @RequestBody Request request,
      @RequestHeader("Idempotency-Key") UUID key, @RequestHeader HttpHeaders headers) {
    var actor = auth(courseId, headers);
    return ResponseEntity.accepted().body(service.enqueue(courseId, new ShadowRunService.Command(
        request.candidateRubricVersionId(), request.source(), request.goldenSetVersionId(), request.sampleSize(),
        request.divergenceThreshold()), key, actor));
  }

  @GetMapping("/{runId}")
  public ShadowRun get(@PathVariable UUID courseId, @PathVariable UUID runId, @RequestHeader HttpHeaders headers) {
    auth(courseId, headers);
    return service.get(courseId, runId);
  }

  private CallerIdentity auth(UUID courseId, HttpHeaders headers) {
    var actor = authorization.require(headers);
    courses.requireTeacher(courseId, actor, headers);
    return actor;
  }

  public record Request(UUID candidateRubricVersionId, ShadowRun.Source source, UUID goldenSetVersionId,
      Integer sampleSize, Double divergenceThreshold) {}

  public record Page(List<ShadowRun> items) {}
}
