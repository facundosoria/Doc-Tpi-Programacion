package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/llm")
public class EvaluatorSkillsController {

  private final EvaluatorSkillRepository repository;
  private final GoldenSetAuthorization authorization;
  private final CourseAuthorization courseAuthorization;

  public EvaluatorSkillsController(
      EvaluatorSkillRepository repository,
      GoldenSetAuthorization authorization,
      CourseAuthorization courseAuthorization) {
    this.repository = repository;
    this.authorization = authorization;
    this.courseAuthorization = courseAuthorization;
  }

  @GetMapping("/evaluator-skills")
  public List<EvaluatorSkillResponse> listCatalog(@RequestHeader HttpHeaders headers) {
    authorization.require(headers);
    return repository.listCatalog().stream()
        .map(s -> new EvaluatorSkillResponse(
            s.skillKey(),
            s.name(),
            s.description(),
            s.toolType(),
            s.enabledByDefault()))
        .toList();
  }

  @GetMapping("/courses/{courseId}/evaluator-skills")
  public List<EvaluatorSkillResponse> listCourseSkills(
      @PathVariable UUID courseId,
      @RequestHeader HttpHeaders headers) {
    authorize(courseId, headers);
    return repository.findCourseSkills(courseId).stream()
        .map(s -> new EvaluatorSkillResponse(
            s.skillKey(),
            s.name(),
            s.description(),
            s.toolType(),
            s.isActive()))
        .toList();
  }

  @PutMapping("/courses/{courseId}/evaluator-skills")
  public List<EvaluatorSkillResponse> updateCourseSkills(
      @PathVariable UUID courseId,
      @RequestBody UpdateCourseSkillsRequest request,
      @RequestHeader HttpHeaders headers) {
    authorize(courseId, headers);
    List<String> activeKeys = request != null && request.activeSkillKeys() != null
        ? request.activeSkillKeys()
        : List.of();

    if (!repository.allKeysExist(activeKeys)) {
      throw new UnknownSkillKeyException("UNKNOWN_SKILL_KEY: Una o más skills especificadas no existen en el catálogo");
    }

    repository.updateCourseSkills(courseId, activeKeys);

    return repository.findCourseSkills(courseId).stream()
        .map(s -> new EvaluatorSkillResponse(
            s.skillKey(),
            s.name(),
            s.description(),
            s.toolType(),
            s.isActive()))
        .toList();
  }

  private CallerIdentity authorize(UUID courseId, HttpHeaders headers) {
    CallerIdentity actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    return actor;
  }

  public record EvaluatorSkillResponse(
      String skillKey,
      String name,
      String description,
      String toolType,
      boolean isActive) {}

  public record UpdateCourseSkillsRequest(
      List<String> activeSkillKeys) {}

  public static class UnknownSkillKeyException extends RuntimeException {
    public UnknownSkillKeyException(String message) {
      super(message);
    }
  }
}
