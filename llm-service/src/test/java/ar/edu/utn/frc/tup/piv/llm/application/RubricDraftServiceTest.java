package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService.DimensionInput;
import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService.RubricInput;
import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService.RubricVersion;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RubricDraftServiceTest {
  @Test void autosaveAdvancesRevisionBeforeReplacingDraftDimensions() {
    var repository = mock(RubricVersionRepository.class); var service = new RubricDraftService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), family = UUID.randomUUID(); RubricInput input = input();
    when(repository.advanceRevision(course, version, 3)).thenReturn(true);
    RubricVersion saved = new RubricVersion(version, family, 1, "Rúbrica", "DRAFT", 4, null, input.dimensions());
    when(repository.find(course, version)).thenReturn(java.util.Optional.of(saved));

    assertThat(service.autosave(course, version, 3, input, new CallerIdentity("gateway", UUID.randomUUID(), null, null)).revision()).isEqualTo(4);
    var order = org.mockito.Mockito.inOrder(repository);
    order.verify(repository).advanceRevision(course, version, 3);
    order.verify(repository).updateVersionName(course, version, input.name());
    order.verify(repository).replaceDimensions(version, input.dimensions());
  }
  @Test void rejectsStaleAutosaveWithoutModifyingDimensions() {
    var repository = mock(RubricVersionRepository.class); UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    when(repository.advanceRevision(course, version, 2)).thenReturn(false);
    assertThatThrownBy(() -> new RubricDraftService(repository).autosave(course, version, 2, input(), new CallerIdentity("gateway", UUID.randomUUID(), null, null)))
        .isInstanceOf(RubricDraftService.OptimisticLockException.class);
    verify(repository, never()).replaceDimensions(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }
  @Test void createsCourseRubricWithTheTitleChosenByTheTeacher() {
    var repository = mock(RubricVersionRepository.class); var service = new RubricDraftService(repository);
    UUID course = UUID.randomUUID(), template = UUID.randomUUID(), version = UUID.randomUUID(), family = UUID.randomUUID();
    var actor = new CallerIdentity("gateway", UUID.randomUUID(), null, null);
    var created = new RubricVersion(version, family, 1, "Evaluación de depuración", "DRAFT", 1, template, input().dimensions());
    when(repository.createDraftFromPublishedTemplate(course, template, "Evaluación de depuración", actor.delegatedUserId())).thenReturn(java.util.Optional.of(created));

    assertThat(service.createFromTemplate(course, template, "  Evaluación de depuración  ", actor).name()).isEqualTo("Evaluación de depuración");
    verify(repository).createDraftFromPublishedTemplate(course, template, "Evaluación de depuración", actor.delegatedUserId());
  }
  private RubricInput input() {
    return new RubricInput("Rúbrica", List.of(
        dimension(Dimension.AUTONOMY), dimension(Dimension.CLARITY), dimension(Dimension.PROGRESSION), dimension(Dimension.COMPLIANCE), dimension(Dimension.EFFICIENCY)));
  }
  private DimensionInput dimension(Dimension key) { return new DimensionInput(key, key.name(), "criterio", anchors(), BigDecimal.valueOf(20)); }
  private RubricDraftService.Anchors anchors() { return new RubricDraftService.Anchors(
      new RubricDraftService.Anchor("bajo", 25, "ejemplo bajo"),
      new RubricDraftService.Anchor("medio", 60, "ejemplo medio"),
      new RubricDraftService.Anchor("alto", 90, "ejemplo alto")); }
}
