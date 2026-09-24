package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.Anchor;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.Anchors;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.DimensionInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricVersion;
import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RubricTemplateServiceCoverageTest {
  private final RubricVersionRepository repository = mock(RubricVersionRepository.class);
  private final RubricTemplateService service = new RubricTemplateService(repository);
  private final CallerIdentity actor = new CallerIdentity("admin-service", UUID.randomUUID(), null, null);

  @Test void listsInstitutionalRubricTemplates() {
    when(repository.listTemplates()).thenReturn(List.of(version("Plantilla")));
    assertThat(service.list()).extracting(RubricVersion::name).containsExactly("Plantilla");
  }

  @Test void getsExistingTemplateAndRejectsMissingOne() {
    UUID id = UUID.randomUUID();
    when(repository.findTemplate(id)).thenReturn(Optional.of(
        new RubricVersion(id, UUID.randomUUID(), 1, "Plantilla", "DRAFT", 1, null, templates())));
    assertThat(service.get(id).id()).isEqualTo(id);
    when(repository.findTemplate(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.get(id)).isInstanceOf(ResourceNotFoundException.class).hasMessage("La plantilla no existe");
  }

  @Test void rejectsBlankTemplateNames() {
    assertThatThrownBy(() -> service.create(new RubricInput(null, templates()), actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("La plantilla debe incluir nombre y las cinco dimensiones");
    assertThatThrownBy(() -> service.create(new RubricInput("  ", templates()), actor))
        .isInstanceOf(IllegalArgumentException.class);
    verify(repository, never()).createTemplateDraft(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test void rejectsMalformedTemplateDimensions() {
    assertThatThrownBy(() -> service.create(new RubricInput("Plantilla", null), actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("La plantilla debe incluir nombre y las cinco dimensiones");
    assertThatThrownBy(() -> service.create(new RubricInput("Plantilla", templates().subList(0, 4)), actor))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void createsTemplateDraft() {
    var input = new RubricInput("Plantilla Institucional", templates());
    when(repository.createTemplateDraft(input, actor.delegatedUserId())).thenReturn(version("Plantilla Institucional"));
    assertThat(service.create(input, actor).name()).isEqualTo("Plantilla Institucional");
    verify(repository).createTemplateDraft(input, actor.delegatedUserId());
  }

  @Test void updatesTemplateRevision() {
    UUID id = UUID.randomUUID();
    when(repository.advanceTemplateRevision(id, 3)).thenReturn(true);
    when(repository.findTemplate(id)).thenReturn(Optional.of(version("Plantilla")));
    var input = new RubricInput("  Plantilla Actualizada  ", templates());
    var updated = service.update(id, 3, input);
    assertThat(updated.name()).isEqualTo("Plantilla");
    verify(repository).updateTemplateVersionName(id, "Plantilla Actualizada");
    verify(repository).replaceDimensions(id, templates());
  }

  @Test void rejectsUpdateWhenStale() {
    when(repository.advanceTemplateRevision(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(false);
    assertThatThrownBy(() -> service.update(UUID.randomUUID(), 3, new RubricInput("Plantilla", templates())))
        .isInstanceOf(RubricDraftService.OptimisticLockException.class)
        .hasMessage("El borrador cambió; recargá antes de guardar");
  }

  @Test void rejectsMissingTemplateAfterRevisionAdvance() {
    UUID id = UUID.randomUUID();
    when(repository.advanceTemplateRevision(id, 3)).thenReturn(true);
    when(repository.findTemplate(id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.update(id, 3, new RubricInput("Plantilla", templates())))
        .isInstanceOf(ResourceNotFoundException.class).hasMessage("La plantilla no existe");
  }

  @Test void refusesPublishingNonDraftTemplates() {
    UUID id = UUID.randomUUID();
    when(repository.findTemplate(id)).thenReturn(Optional.of(version("Plantilla", "PUBLISHED")));
    assertThatThrownBy(() -> service.publish(id)).isInstanceOf(IllegalStateException.class).hasMessage("Sólo se publica un borrador");
  }

  @Test void refusesPublishingMissingTemplate() {
    when(repository.findTemplate(UUID.randomUUID())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.publish(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class).hasMessage("La plantilla no existe");
  }

  @Test void rejectsPublishWhenWeightsDoNotTotalOneHundred() {
    UUID id = UUID.randomUUID();
    var input = new RubricInput("Plantilla", List.of(
        dimension(Dimension.AUTONOMY, 30), dimension(Dimension.CLARITY, 25), dimension(Dimension.PROGRESSION, 20),
        dimension(Dimension.COMPLIANCE, 15), dimension(Dimension.EFFICIENCY, 5)));
    when(repository.findTemplate(id)).thenReturn(Optional.of(new RubricVersion(id, UUID.randomUUID(), 1, input.name(), "DRAFT", 1, null, input.dimensions())));
    assertThatThrownBy(() -> service.publish(id)).isInstanceOf(IllegalArgumentException.class).hasMessage("Rubric weights must total 100");
  }

  @Test void publishesTemplateDraft() {
    UUID id = UUID.randomUUID();
    when(repository.findTemplate(id)).thenReturn(Optional.of(version("Plantilla", "DRAFT")));
    when(repository.publishTemplateDraft(id)).thenReturn(true);
    service.publish(id);
    verify(repository).publishTemplateDraft(id);
  }

  @Test void rejectsPublishWhenTemplateChangedConcurrently() {
    UUID id = UUID.randomUUID();
    when(repository.findTemplate(id)).thenReturn(Optional.of(version("Plantilla", "DRAFT")));
    when(repository.publishTemplateDraft(id)).thenReturn(false);
    assertThatThrownBy(() -> service.publish(id)).isInstanceOf(IllegalStateException.class).hasMessage("La plantilla fue modificada durante la publicación");
  }

  @Test void createsNextTemplateDraft() {
    UUID id = UUID.randomUUID();
    when(repository.createNextTemplateDraft(id, actor.delegatedUserId())).thenReturn(Optional.of(new RubricVersion(UUID.randomUUID(), UUID.randomUUID(), 2, "Plantilla", "DRAFT", 1, null, List.of())));
    assertThat(service.next(id, actor).version()).isEqualTo(2);
  }

  @Test void rejectsNextVersionWhenTemplateIsNotPublished() {
    when(repository.createNextTemplateDraft(UUID.randomUUID(), actor.delegatedUserId())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.next(UUID.randomUUID(), actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("Sólo una plantilla publicada puede versionarse");
  }

  private RubricVersion version(String name) { return version(name, "DRAFT"); }

  private RubricVersion version(String name, String state) {
    return new RubricVersion(UUID.randomUUID(), UUID.randomUUID(), 1, name, state, 1, null, templates());
  }

  private List<DimensionInput> templates() {
    return List.of(dimension(Dimension.AUTONOMY, 30), dimension(Dimension.CLARITY, 25), dimension(Dimension.PROGRESSION, 20),
        dimension(Dimension.COMPLIANCE, 15), dimension(Dimension.EFFICIENCY, 10));
  }

  private DimensionInput dimension(Dimension key, int weight) {
    return new DimensionInput(key, key.name(), "criterio", new Anchors(new Anchor("bajo", 25, "ejemplo"), new Anchor("medio", 60, "ejemplo"), new Anchor("alto", 90, "ejemplo")), BigDecimal.valueOf(weight));
  }
}