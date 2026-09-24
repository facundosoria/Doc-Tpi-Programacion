package ar.edu.utn.frc.tup.piv.llm.adapter.in.web;

import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricTemplateService;
import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.security.GoldenSetAuthorization;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only catalogue teachers use when starting a course rubric. */
@RestController
@RequestMapping("${app.api.private-path}/rubric-templates")
public class RubricTemplateCatalogController {
  private final RubricTemplateService templates; private final GoldenSetAuthorization authorization;
  public RubricTemplateCatalogController(RubricTemplateService templates, GoldenSetAuthorization authorization) { this.templates = templates; this.authorization = authorization; }
  @GetMapping public TemplatePage list(@RequestHeader HttpHeaders headers) { authorization.require(headers); return new TemplatePage(templates.list().stream().filter(item -> "PUBLISHED".equals(item.state())).toList()); }
  public record TemplatePage(List<RubricDraftService.RubricVersion> items) {}
}
