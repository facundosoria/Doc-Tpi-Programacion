package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ModelDeploymentRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ModelDeploymentRepository.ModelDeploymentSummary;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModelDeploymentController {
  private final ModelDeploymentRepository repository;
  private final GoldenSetAuthorization authorization;
  private final CourseAuthorization courseAuthorization;

  public ModelDeploymentController(ModelDeploymentRepository repository,
      GoldenSetAuthorization authorization, CourseAuthorization courseAuthorization) {
    this.repository = repository;
    this.authorization = authorization;
    this.courseAuthorization = courseAuthorization;
  }

  @GetMapping("/api/llm/courses/{courseId}/model-deployments")
  public ModelDeploymentPage listForCourse(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    return new ModelDeploymentPage(repository.listEnabledDeployments());
  }

  @GetMapping("/api/llm/admin/model-adapters")
  public ModelAdapterPage listAdapters(@RequestHeader HttpHeaders headers) {
    authorization.require(headers);
    var deployments = repository.listEnabledDeployments();
    return new ModelAdapterPage(List.of(
        new ModelAdapterSummary(UUID.randomUUID(), "openai", deployments.stream().filter(d -> "openai".equals(d.provider())).toList()),
        new ModelAdapterSummary(UUID.randomUUID(), "anthropic", deployments.stream().filter(d -> "anthropic".equals(d.provider())).toList())
    ));
  }

  public record ModelDeploymentPage(List<ModelDeploymentSummary> items) {}
  public record ModelAdapterPage(List<ModelAdapterSummary> items) {}
  public record ModelAdapterSummary(UUID id, String provider, List<ModelDeploymentSummary> deployments) {}
}
