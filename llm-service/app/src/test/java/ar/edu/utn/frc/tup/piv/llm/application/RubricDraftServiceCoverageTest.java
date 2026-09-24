package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.DimensionInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricVersion;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RubricDraftServiceCoverageTest {
  private final CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), null, null);

  @Test void listsCourseRubrics() {
    var repository = mock(RubricVersionRepository.class);
    var service = new RubricDraftService(repository);
    UUID course = UUID.randomUUID();
    var version = new RubricVersion(UUID.randomUUID(), UUID.randomUUID(), 1, "Rúbrica", "DRAFT", 1, null, List.of());
    when(repository.list(course)).thenReturn(List.of(version));
    assertThat(service.list(course)).containsExactly(version);
  }

  @Test void getsExistingVersionAndRejectsMissingOne() {
    var repository = mock(RubricVersionRepository.class);
    var service = new RubricDraftService(repository);
    UUID course = UUID.randomUUID(), versionId = UUID.randomUUID();
    var version = new RubricVersion(versionId, UUID.randomUUID(), 1, "Rúbrica", "DRAFT", 1, null, List.of());
    when(repository.find(course, versionId)).thenReturn(Optional.of(version));
    assertThat(service.get(course, versionId)).isEqualTo(version);
    when(repository.find(course, versionId)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.get(course, versionId)).isInstanceOf(ResourceNotFoundException.class).hasMessage("La rúbrica no existe en el curso");
  }

  @Test void rejectsBlankTemplateNames() {
    var repository = mock(RubricVersionRepository.class);
    var service = new RubricDraftService(repository);
    UUID course = UUID.randomUUID(), template = UUID.randomUUID();
    assertThatThrownBy(() -> service.createFromTemplate(course, template, " ", actor)).isInstanceOf(IllegalArgumentException.class).hasMessage("El nombre de la rúbrica es obligatorio");
    assertThatThrownBy(() -> service.createFromTemplate(course, template, null, actor)).isInstanceOf(IllegalArgumentException.class);
    verify(repository, never()).createDraftFromPublishedTemplate(any(), any(), any(), any());
  }

  @Test void rejectsMissingPublishedTemplate() {
    var repository = mock(RubricVersionRepository.class);
    var service = new RubricDraftService(repository);
    UUID course = UUID.randomUUID(), template = UUID.randomUUID();
    when(repository.createDraftFromPublishedTemplate(course, template, "Rúbrica", actor.delegatedUserId())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.createFromTemplate(course, template, "Rúbrica", actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("La plantilla institucional publicada no existe");
  }

  @Test void autosaveRejectsMalformedInputs() {
    var repository = mock(RubricVersionRepository.class);
    var service = new RubricDraftService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    assertThatThrownBy(() -> service.autosave(course, version, 1, null, actor)).isInstanceOf(IllegalArgumentException.class).hasMessage("El borrador debe incluir nombre y exactamente las cinco dimensiones");
    assertThatThrownBy(() -> service.autosave(course, version, 1, new RubricInput(null, inputDimensions()), actor)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.autosave(course, version, 1, new RubricInput(" ", inputDimensions()), actor)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.autosave(course, version, 1, new RubricInput("Rúbrica", null), actor)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.autosave(course, version, 1, new RubricInput("Rúbrica", inputDimensions().subList(0, 4)), actor)).isInstanceOf(IllegalArgumentException.class);
    verify(repository, never()).advanceRevision(any(), any(), anyLong());
  }

  @Test void autosaveRejectsDuplicateDimensions() {
    var repository = mock(RubricVersionRepository.class);
    var service = new RubricDraftService(repository);
    var duplicated = List.of(dimension(Dimension.AUTONOMY), dimension(Dimension.AUTONOMY),
        dimension(Dimension.CLARITY), dimension(Dimension.PROGRESSION), dimension(Dimension.COMPLIANCE));
    assertThatThrownBy(() -> service.autosave(UUID.randomUUID(), UUID.randomUUID(), 1, new RubricInput("Rúbrica", duplicated), actor))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void nextVersionFailureIsRejected() {
    var repository = mock(RubricVersionRepository.class);
    var service = new RubricDraftService(repository);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    when(repository.createNextDraft(course, version, actor.delegatedUserId())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.createNextVersion(course, version, actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("Solo una versión publicada del curso puede originar una nueva versión");
  }

  @Test void autosaveSavesAndReloadsTrimmedName() {
    var repository = mock(RubricVersionRepository.class);
    var service = new RubricDraftService(repository);
    UUID course = UUID.randomUUID(), versionId = UUID.randomUUID();
    var input = new RubricInput("  Rúbrica Guardada  ", inputDimensions());
    when(repository.advanceRevision(course, versionId, 4)).thenReturn(true);
    var saved = new RubricVersion(versionId, UUID.randomUUID(), 1, "Rúbrica Guardada", "DRAFT", 5, null, inputDimensions());
    when(repository.find(course, versionId)).thenReturn(Optional.of(saved));
    assertThat(service.autosave(course, versionId, 4, input, actor)).isEqualTo(saved);
    verify(repository).updateVersionName(course, versionId, "Rúbrica Guardada");
    verify(repository).replaceDimensions(versionId, inputDimensions());
  }

  @Test void rejectsSavingWhenRevisionIsStale() {
    var repository = mock(RubricVersionRepository.class);
    var service = new RubricDraftService(repository);
    UUID course = UUID.randomUUID(), versionId = UUID.randomUUID();
    when(repository.advanceRevision(course, versionId, 4)).thenReturn(false);
    assertThatThrownBy(() -> service.autosave(course, versionId, 4, new RubricInput("Rúbrica", inputDimensions()), actor))
        .isInstanceOf(RubricDraftService.OptimisticLockException.class)
        .hasMessage("El borrador fue actualizado en otro dispositivo; recargá antes de guardar");
  }

  @Test void optimisticLockExceptionCarriesItsMessage() {
    assertThat(new RubricDraftService.OptimisticLockException("stale").getMessage()).isEqualTo("stale");
  }

  private List<DimensionInput> inputDimensions() {
    return List.of(dimension(Dimension.AUTONOMY), dimension(Dimension.CLARITY), dimension(Dimension.PROGRESSION),
        dimension(Dimension.COMPLIANCE), dimension(Dimension.EFFICIENCY));
  }

  private DimensionInput dimension(Dimension key) {
    return new DimensionInput(key, key.name(), "criterio", new RubricDraftService.Anchors(
        new RubricDraftService.Anchor("bajo", 25, "ejemplo bajo"),
        new RubricDraftService.Anchor("medio", 60, "ejemplo medio"),
        new RubricDraftService.Anchor("alto", 90, "ejemplo alto")), BigDecimal.valueOf(20));
  }
}