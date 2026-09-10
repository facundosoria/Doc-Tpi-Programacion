package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.SyntheticGoldenSetProposalService;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/llm/courses/{courseId}/synthetic-golden-set-cases")
public class SyntheticGoldenSetController {
  private final SyntheticGoldenSetProposalService service;
  private final GoldenSetAuthorization authorization;
  private final CourseAuthorization courseAuthorization;

  public SyntheticGoldenSetController(SyntheticGoldenSetProposalService service,
      GoldenSetAuthorization authorization, CourseAuthorization courseAuthorization) {
    this.service = service;
    this.authorization = authorization;
    this.courseAuthorization = courseAuthorization;
  }

  @PostMapping
  public ResponseEntity<SyntheticGoldenSetProposalService.SyntheticProposalResult> propose(
      @PathVariable UUID courseId,
      @RequestBody(required = false) ProposeRequest request,
      @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    int count = request != null && request.count() != null ? request.count() : 2;
    var result = service.propose(courseId, count);
    return ResponseEntity.accepted().body(result);
  }

  public record ProposeRequest(Integer count) {}
}
