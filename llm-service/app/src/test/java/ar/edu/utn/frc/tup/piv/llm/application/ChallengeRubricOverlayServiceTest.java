package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.application.service.ChallengeRubricOverlayService;
import ar.edu.utn.frc.tup.piv.llm.application.service.ChallengeRubricOverlayService.ChallengeOverlayVersion;
import ar.edu.utn.frc.tup.piv.llm.application.service.EffectiveRubricResolver;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.Anchor;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.Anchors;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.DimensionCustomInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricVersion;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService;
import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChallengeRubricOverlayServiceTest {
  private final CallerIdentity actor = new CallerIdentity("gateway", UUID.randomUUID(), null, null);
  private final RubricVersionRepository rubrics = mock(RubricVersionRepository.class);
  private final EffectiveRubricResolver resolver = mock(EffectiveRubricResolver.class);
  private final ChallengeRubricOverlayService service = new ChallengeRubricOverlayService(rubrics, resolver);

  @Test void createDraftRequiresAPublishedBaseline() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), baseline = UUID.randomUUID();
    when(rubrics.find(course, baseline)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.createDraft(course, challenge, "Overlay", baseline, actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("La rúbrica base del curso no existe");
  }

  @Test void createDraftRejectsUnpublishedBaseline() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), baseline = UUID.randomUUID();
    when(rubrics.find(course, baseline)).thenReturn(Optional.of(version("Rúbrica", "DRAFT", baseline)));
    assertThatThrownBy(() -> service.createDraft(course, challenge, "Overlay", baseline, actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("La rúbrica base debe estar publicada");
  }

  @Test void autosaveRejectsCustomDimensionsNotTotallingOneHundred() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), overlay = UUID.randomUUID();
    var input = new ChallengeRubricOverlayService.OverlayInput("Overlay", "Guía",
        List.of(custom("algoritmos", 60)));
    assertThatThrownBy(() -> service.autosave(course, challenge, overlay, 1, input, actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Rubric weights must total 100");
    verify(rubrics, never()).advanceChallengeRevision(any(), any(), any(), anyLong());
  }

  @Test void autosavePersistsCustomDimensionsAndPrompt() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), overlay = UUID.randomUUID();
    var dims = List.of(custom("algoritmos", 60), custom("pruebas", 40));
    var input = new ChallengeRubricOverlayService.OverlayInput("Overlay", "Guía", dims);
    when(rubrics.advanceChallengeRevision(course, challenge, overlay, 2)).thenReturn(true);
    var saved = overlayVersion(overlay, "Overlay", "DRAFT", 3, "Guía", dims);
    when(rubrics.findChallenge(course, challenge, overlay)).thenReturn(Optional.of(versionWithId(overlay, "Overlay", "DRAFT", overlay)));
    when(rubrics.challengeCustomDimensions(overlay)).thenReturn(dims);
    when(rubrics.rubricKindOf(overlay)).thenReturn("MODULAR_CUSTOM");
    when(rubrics.userPromptOf(overlay)).thenReturn("Guía");
    when(rubrics.baselineVersionIdOf(overlay)).thenReturn(UUID.randomUUID());

    var result = service.autosave(course, challenge, overlay, 2, input, actor);

    assertThat(result.name()).isEqualTo("Overlay");
    assertThat(result.customDimensions()).hasSize(2);
    verify(rubrics).replaceChallengeCustomDimensions(overlay, dims);
    verify(rubrics).updateChallengePrompt(course, challenge, overlay, "Guía");
  }

  @Test void publishRejectsEmptyCustomDimensions() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), overlay = UUID.randomUUID();
    when(rubrics.findChallenge(course, challenge, overlay)).thenReturn(Optional.of(versionWithId(overlay, "Overlay", "DRAFT", overlay)));
    when(rubrics.challengeCustomDimensions(overlay)).thenReturn(List.of());
    when(rubrics.rubricKindOf(overlay)).thenReturn("MODULAR_CUSTOM");
    when(rubrics.userPromptOf(overlay)).thenReturn("");
    when(rubrics.baselineVersionIdOf(overlay)).thenReturn(UUID.randomUUID());
    assertThatThrownBy(() -> service.publish(course, challenge, overlay, actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("El overlay debe tener al menos una dimensión custom para publicarse");
  }

  @Test void publishRejectsWeightsNotTotallingOneHundred() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), overlay = UUID.randomUUID();
    when(rubrics.findChallenge(course, challenge, overlay)).thenReturn(Optional.of(versionWithId(overlay, "Overlay", "DRAFT", overlay)));
    when(rubrics.challengeCustomDimensions(overlay)).thenReturn(List.of(custom("algoritmos", 60)));
    when(rubrics.rubricKindOf(overlay)).thenReturn("MODULAR_CUSTOM");
    when(rubrics.userPromptOf(overlay)).thenReturn("");
    when(rubrics.baselineVersionIdOf(overlay)).thenReturn(UUID.randomUUID());
    assertThatThrownBy(() -> service.publish(course, challenge, overlay, actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Rubric weights must total 100");
  }

  @Test void getRejectsMissingOverlay() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), overlay = UUID.randomUUID();
    when(rubrics.findChallenge(course, challenge, overlay)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.get(course, challenge, overlay))
        .isInstanceOf(ResourceNotFoundException.class).hasMessage("El overlay no existe para este desafío");
  }

  @Test void listByChallengeReturnsOverlayVersions() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID();
    var v = version("Overlay", "DRAFT", UUID.randomUUID());
    when(rubrics.listByChallenge(course, challenge)).thenReturn(List.of(v));
    when(rubrics.challengeCustomDimensions(v.id())).thenReturn(List.of());
    when(rubrics.rubricKindOf(v.id())).thenReturn("MODULAR_CUSTOM");
    when(rubrics.userPromptOf(v.id())).thenReturn("");
    when(rubrics.baselineVersionIdOf(v.id())).thenReturn(UUID.randomUUID());

    var result = service.listByChallenge(course, challenge);

    assertThat(result).hasSize(1);
  }

  @Test void listByChallengeWorksWithoutAssignment() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID();
    when(rubrics.listByChallenge(course, challenge)).thenReturn(List.of());

    var result = service.listByChallenge(course, challenge);

    assertThat(result).isEmpty();
    verify(rubrics).listByChallenge(course, challenge);
  }

  @Test void listAllByCourseMapsChallengeOverlays() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID();
    var v = version("Overlay A", "PUBLISHED", UUID.randomUUID());
    when(rubrics.listOverlaysByCourse(course)).thenReturn(
        List.of(new RubricVersionRepository.ChallengeRow(challenge, v)));
    when(rubrics.rubricKindOf(v.id())).thenReturn("MODULAR_CUSTOM");
    when(rubrics.baselineVersionIdOf(v.id())).thenReturn(UUID.randomUUID());

    var result = service.listAllByCourse(course);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).challengeId()).isEqualTo(challenge);
    assertThat(result.get(0).name()).isEqualTo("Overlay A");
    assertThat(result.get(0).state()).isEqualTo("PUBLISHED");
  }

  @Test void createDraftCreatesOverlayWhenBaselineIsPublished() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), baseline = UUID.randomUUID();
    var created = version("Overlay", "DRAFT", baseline);
    when(rubrics.find(course, baseline)).thenReturn(Optional.of(version("Rúbrica", "PUBLISHED", baseline)));
    when(rubrics.createDraftForChallenge(course, challenge, "Overlay", baseline, actor.delegatedUserId()))
        .thenReturn(Optional.of(created));
    when(rubrics.challengeCustomDimensions(created.id())).thenReturn(List.of());
    when(rubrics.rubricKindOf(created.id())).thenReturn("MODULAR_CUSTOM");
    when(rubrics.userPromptOf(created.id())).thenReturn("");
    when(rubrics.baselineVersionIdOf(created.id())).thenReturn(baseline);

    var result = service.createDraft(course, challenge, "Overlay", baseline, actor);

    assertThat(result.name()).isEqualTo("Overlay");
  }

  @Test void createDraftRejectsMissingTemplateDraft() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), baseline = UUID.randomUUID();
    when(rubrics.find(course, baseline)).thenReturn(Optional.of(version("Rúbrica", "PUBLISHED", baseline)));
    when(rubrics.createDraftForChallenge(course, challenge, "Overlay", baseline, actor.delegatedUserId()))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.createDraft(course, challenge, "Overlay", baseline, actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("No se pudo crear el borrador del overlay");
  }

  @Test void createNextVersionReturnsTheNewDraft() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), published = UUID.randomUUID();
    var next = version("Overlay v2", "DRAFT", UUID.randomUUID());
    when(rubrics.createNextChallengeDraft(course, challenge, published, actor.delegatedUserId()))
        .thenReturn(Optional.of(next));
    when(rubrics.challengeCustomDimensions(next.id())).thenReturn(List.of());
    when(rubrics.rubricKindOf(next.id())).thenReturn("MODULAR_CUSTOM");
    when(rubrics.userPromptOf(next.id())).thenReturn("");
    when(rubrics.baselineVersionIdOf(next.id())).thenReturn(UUID.randomUUID());

    var result = service.createNextVersion(course, challenge, published, actor);

    assertThat(result.name()).isEqualTo("Overlay v2");
  }

  @Test void createNextVersionRejectsUnpublishedSource() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), published = UUID.randomUUID();
    when(rubrics.createNextChallengeDraft(course, challenge, published, actor.delegatedUserId()))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.createNextVersion(course, challenge, published, actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("Solo una versión publicada del overlay puede originar una nueva versión");
  }

  @Test void getEffectiveProfileResolvesTheOverlay() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), overlay = UUID.randomUUID(), baseline = UUID.randomUUID();
    var dim = new EffectiveRubricResolver.EffectiveDimension("a", "A", "c", null, BigDecimal.valueOf(100),
        EffectiveRubricResolver.EffectiveDimension.Origin.OVERLAY);
    when(rubrics.findChallenge(course, challenge, overlay)).thenReturn(Optional.of(versionWithId(overlay, "Overlay", "PUBLISHED", baseline)));
    when(rubrics.challengeCustomDimensions(overlay)).thenReturn(List.of(custom("a", 100)));
    when(rubrics.rubricKindOf(overlay)).thenReturn("MODULAR_CUSTOM");
    when(rubrics.userPromptOf(overlay)).thenReturn("");
    when(rubrics.baselineVersionIdOf(overlay)).thenReturn(baseline);
    when(resolver.resolve(baseline, overlay)).thenReturn(List.of(dim));

    var result = service.getEffectiveProfile(course, challenge, overlay);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).key()).isEqualTo("a");
  }

  @Test void publishPublishesWhenWeightsTotalOneHundred() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), overlay = UUID.randomUUID();
    when(rubrics.findChallenge(course, challenge, overlay)).thenReturn(Optional.of(versionWithId(overlay, "Overlay", "DRAFT", UUID.randomUUID())));
    when(rubrics.challengeCustomDimensions(overlay)).thenReturn(List.of(custom("a", 60), custom("b", 40)));
    when(rubrics.rubricKindOf(overlay)).thenReturn("MODULAR_CUSTOM");
    when(rubrics.userPromptOf(overlay)).thenReturn("");
    when(rubrics.baselineVersionIdOf(overlay)).thenReturn(UUID.randomUUID());
    when(rubrics.publishChallengeDraft(course, challenge, overlay)).thenReturn(true);

    service.publish(course, challenge, overlay, actor);

    verify(rubrics).publishChallengeDraft(course, challenge, overlay);
  }

  @Test void publishRejectsConcurrentModification() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), overlay = UUID.randomUUID();
    when(rubrics.findChallenge(course, challenge, overlay)).thenReturn(Optional.of(versionWithId(overlay, "Overlay", "DRAFT", UUID.randomUUID())));
    when(rubrics.challengeCustomDimensions(overlay)).thenReturn(List.of(custom("a", 100)));
    when(rubrics.rubricKindOf(overlay)).thenReturn("MODULAR_CUSTOM");
    when(rubrics.userPromptOf(overlay)).thenReturn("");
    when(rubrics.baselineVersionIdOf(overlay)).thenReturn(UUID.randomUUID());
    when(rubrics.publishChallengeDraft(course, challenge, overlay)).thenReturn(false);
    assertThatThrownBy(() -> service.publish(course, challenge, overlay, actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("El overlay fue modificado mientras se publicaba");
  }

  @Test void autosaveRejectsStaleRevision() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), overlay = UUID.randomUUID();
    var input = new ChallengeRubricOverlayService.OverlayInput("Overlay", "Guía",
        List.of(custom("a", 100)));
    when(rubrics.advanceChallengeRevision(course, challenge, overlay, 1)).thenReturn(false);
    assertThatThrownBy(() -> service.autosave(course, challenge, overlay, 1, input, actor))
        .isInstanceOf(RubricDraftService.OptimisticLockException.class)
        .hasMessage("El overlay fue actualizado en otro dispositivo; recargá antes de guardar");
  }

  @Test void validateRejectsIncompleteCustomDimension() {
    UUID course = UUID.randomUUID(), challenge = UUID.randomUUID(), overlay = UUID.randomUUID();
    var input = new ChallengeRubricOverlayService.OverlayInput("Overlay", "Guía",
        List.of(new DimensionCustomInput("", "Sin clave", "criterio", null, BigDecimal.valueOf(100))));
    assertThatThrownBy(() -> service.autosave(course, challenge, overlay, 1, input, actor))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cada dimensión custom debe incluir clave, título y criterio");
  }

  private DimensionCustomInput custom(String key, int weight) {
    return new DimensionCustomInput(key, key, "criterio",
        new Anchors(new Anchor("bajo", 25, "ej"), new Anchor("medio", 60, "ej"), new Anchor("alto", 90, "ej")),
        BigDecimal.valueOf(weight));
  }

  private RubricVersion version(String name, String state, UUID baseline) {
    return new RubricVersion(UUID.randomUUID(), UUID.randomUUID(), 1, name, state, 1, baseline, List.of());
  }

  private RubricVersion versionWithId(UUID id, String name, String state, UUID baseline) {
    return new RubricVersion(id, UUID.randomUUID(), 1, name, state, 1, baseline, List.of());
  }

  private ChallengeOverlayVersion overlayVersion(UUID id, String name, String state, long revision,
      String prompt, List<DimensionCustomInput> dims) {
    return new ChallengeOverlayVersion(id, UUID.randomUUID(), 1, name, state, revision, "MODULAR_CUSTOM", prompt, dims, UUID.randomUUID());
  }
}