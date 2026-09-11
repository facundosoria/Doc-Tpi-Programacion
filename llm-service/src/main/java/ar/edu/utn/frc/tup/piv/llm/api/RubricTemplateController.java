package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService;
import ar.edu.utn.frc.tup.piv.llm.application.RubricTemplateService;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Global academic configuration. Authorization is intentionally separate from teacher endpoints. */
@RestController
@RequestMapping("/api/llm/admin/rubric-templates")
public class RubricTemplateController {
  private final RubricTemplateService templates;
  private final GoldenSetAuthorization authorization;
  public RubricTemplateController(RubricTemplateService templates, GoldenSetAuthorization authorization) { this.templates = templates; this.authorization = authorization; }

  @GetMapping public TemplatePage list(@RequestHeader HttpHeaders headers) { authorization.requireTemplateManager(headers); return new TemplatePage(templates.list()); }
  @GetMapping("/{id}") public RubricDraftService.RubricVersion get(@PathVariable UUID id, @RequestHeader HttpHeaders headers) { authorization.requireTemplateManager(headers); return templates.get(id); }
  @PostMapping public ResponseEntity<RubricDraftService.RubricVersion> create(@RequestBody RubricDraftService.RubricInput input, @RequestHeader HttpHeaders headers) {
    var created = templates.create(input, authorization.requireTemplateManager(headers));
    return ResponseEntity.created(URI.create("/api/llm/admin/rubric-templates/" + created.id())).body(created);
  }
  @PatchMapping("/{id}") public RubricDraftService.RubricVersion update(@PathVariable UUID id, @RequestHeader("If-Match") long revision, @RequestBody RubricDraftService.RubricInput input, @RequestHeader HttpHeaders headers) { authorization.requireTemplateManager(headers); return templates.update(id, revision, input); }
  @PostMapping("/{id}/publish") public void publish(@PathVariable UUID id, @RequestHeader HttpHeaders headers) { authorization.requireTemplateManager(headers); templates.publish(id); }
  @PostMapping("/{id}/next-version") public ResponseEntity<RubricDraftService.RubricVersion> next(@PathVariable UUID id, @RequestHeader HttpHeaders headers) {
    var created = templates.next(id, authorization.requireTemplateManager(headers));
    return ResponseEntity.created(URI.create("/api/llm/admin/rubric-templates/" + created.id())).body(created);
  }
  public record TemplatePage(List<RubricDraftService.RubricVersion> items) {}
}
