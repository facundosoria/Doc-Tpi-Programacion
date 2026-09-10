package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.GoldenSetUpdateProposalService;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetUpdateProposalRepository.GoldenSetUpdateProposal;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/llm/courses/{courseId}/golden-set-update-proposals")
public class GoldenSetUpdateProposalController {
  private final GoldenSetUpdateProposalService service;
  private final GoldenSetAuthorization authorization;
  private final CourseAuthorization courseAuthorization;
  public GoldenSetUpdateProposalController(GoldenSetUpdateProposalService service, GoldenSetAuthorization authorization, CourseAuthorization courseAuthorization) {
    this.service = service; this.authorization = authorization; this.courseAuthorization = courseAuthorization;
  }

  @GetMapping
  public List<GoldenSetUpdateProposal> pending(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    return service.pendingForCourse(courseId);
  }
}
