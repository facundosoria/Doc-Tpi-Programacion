package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.RubricValidator;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ADMIN-owned, versioned source for every course rubric. */
@Service
public class RubricTemplateService {
  private final RubricVersionRepository rubrics;
  public RubricTemplateService(RubricVersionRepository rubrics) { this.rubrics = rubrics; }

  @Transactional(readOnly = true)
  public List<RubricDraftService.RubricVersion> list() { return rubrics.listTemplates(); }

  @Transactional(readOnly = true)
  public RubricDraftService.RubricVersion get(UUID id) { return rubrics.findTemplate(id)
      .orElseThrow(() -> new IllegalStateException("La plantilla no existe")); }

  @Transactional
  public RubricDraftService.RubricVersion create(RubricDraftService.RubricInput input, CallerIdentity actor) {
    validate(input);
    return rubrics.createTemplateDraft(input, actor.delegatedUserId());
  }

  @Transactional
  public RubricDraftService.RubricVersion update(UUID id, long revision, RubricDraftService.RubricInput input) {
    validate(input);
    if (!rubrics.advanceTemplateRevision(id, revision)) throw new RubricDraftService.OptimisticLockException("El borrador cambió; recargá antes de guardar");
    rubrics.updateTemplateVersionName(id, input.name().trim());
    rubrics.replaceDimensions(id, input.dimensions());
    return get(id);
  }

  @Transactional
  public void publish(UUID id) {
    var template = get(id);
    if (!"DRAFT".equals(template.state())) throw new IllegalStateException("Sólo se publica un borrador");
    RubricValidator.validateForPublication(template.dimensions().stream()
        .map(d -> new RubricValidator.DimensionDefinition(d.key(), d.weight())).toList());
    RubricPublicationService.validateAnchors(template.dimensions());
    if (!rubrics.publishTemplateDraft(id)) throw new IllegalStateException("La plantilla fue modificada durante la publicación");
  }

  @Transactional
  public RubricDraftService.RubricVersion next(UUID id, CallerIdentity actor) {
    return rubrics.createNextTemplateDraft(id, actor.delegatedUserId())
        .orElseThrow(() -> new IllegalStateException("Sólo una plantilla publicada puede versionarse"));
  }

  private void validate(RubricDraftService.RubricInput input) {
    if (input == null || input.name() == null || input.name().isBlank() || input.dimensions() == null || input.dimensions().size() != 5) {
      throw new IllegalArgumentException("La plantilla debe incluir nombre y las cinco dimensiones");
    }
  }
}
