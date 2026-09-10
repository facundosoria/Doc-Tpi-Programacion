package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.EligibleInteractionsService;
import ar.edu.utn.frc.tup.piv.llm.security.CourseAuthorization;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/llm/courses/{courseId}/eligible-interactions")
public class EligibleInteractionsController {
  private final EligibleInteractionsService service;
  private final GoldenSetAuthorization authorization;
  private final CourseAuthorization courseAuthorization;

  public EligibleInteractionsController(EligibleInteractionsService service,
      GoldenSetAuthorization authorization, CourseAuthorization courseAuthorization) {
    this.service = service;
    this.authorization = authorization;
    this.courseAuthorization = courseAuthorization;
  }

  @GetMapping
  public EligibleInteractionPage list(@PathVariable UUID courseId, @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    return new EligibleInteractionPage(service.listEligible(courseId));
  }

  @PostMapping("/{interactionId}/anonymize-preview")
  public ResponseEntity<JsonNode> preview(@PathVariable UUID courseId, @PathVariable UUID interactionId,
      @RequestHeader HttpHeaders headers) {
    var actor = authorization.require(headers);
    courseAuthorization.requireTeacher(courseId, actor, headers);
    return service.anonymizePreview(courseId, interactionId)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Interacción elegible no encontrada"));
  }

  public record EligibleInteractionPage(List<EligibleInteractionsService.EligibleInteractionSummary> items) {}
}
