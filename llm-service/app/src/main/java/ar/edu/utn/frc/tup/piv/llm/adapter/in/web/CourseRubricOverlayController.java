package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.service.ChallengeRubricOverlayService;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Vista global de los overlays de rúbrica por desafío de un curso. */
@RestController
@RequestMapping("${app.api.private-path}/courses/{courseId}")
public class CourseRubricOverlayController {
  private final ChallengeRubricOverlayService overlayService;
  private final GoldenSetAuthorization authorization;
  private final CourseAuthorization courseAuthorization;

  public CourseRubricOverlayController(ChallengeRubricOverlayService overlayService,
      GoldenSetAuthorization authorization, CourseAuthorization courseAuthorization) {
    this.overlayService = overlayService;
    this.authorization = authorization;
    this.courseAuthorization = courseAuthorization;
  }

  @GetMapping("/rubric-overlays")
  public CourseOverlayPage list(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    return new CourseOverlayPage(overlayService.listAllByCourse(courseId));
  }

  public record CourseOverlayPage(List<ChallengeRubricOverlayService.CourseOverlayItem> items) {}
}