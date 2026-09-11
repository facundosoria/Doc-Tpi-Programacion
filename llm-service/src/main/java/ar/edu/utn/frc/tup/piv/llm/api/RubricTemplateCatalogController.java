package ar.edu.utn.frc.tup.piv.llm.api;

import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService;
import ar.edu.utn.frc.tup.piv.llm.application.RubricTemplateService;
import ar.edu.utn.frc.tup.piv.llm.security.GoldenSetAuthorization;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only catalogue teachers use when starting a course rubric. */
@RestController
@RequestMapping("/api/llm/rubric-templates")
public class RubricTemplateCatalogController {
  private final RubricTemplateService templates; private final GoldenSetAuthorization authorization;
  public RubricTemplateCatalogController(RubricTemplateService templates, GoldenSetAuthorization authorization) { this.templates = templates; this.authorization = authorization; }
  @GetMapping public TemplatePage list(@RequestHeader HttpHeaders headers) { authorization.require(headers); return new TemplatePage(templates.list().stream().filter(item -> "PUBLISHED".equals(item.state())).toList()); }
  public record TemplatePage(List<RubricDraftService.RubricVersion> items) {}
}
