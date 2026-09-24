package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.Anchor;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.Anchors;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.DimensionInput;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.RubricVersion;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.domain.RubricValidator.DimensionDefinition;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RubricPublicationServiceCoverageTest {
  private final CallerIdentity actor = new CallerIdentity("admin-service", UUID.randomUUID(), null, null);

  @Test void refusesPublishWithoutDimensions() {
    var rubrics = mock(RubricVersionRepository.class);
    var service = new RubricPublicationService(rubrics, mock(AuditRepository.class), mock(CalibrationExpirationService.class));
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    when(rubrics.dimensionsOfDraft(course, version)).thenReturn(List.of());
    assertThatThrownBy(() -> service.publish(course, version, actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("La rúbrica no existe en el curso o ya no es un borrador");
  }

  @Test void refusesPublishWhenVersionIsMissing() {
    var rubrics = mock(RubricVersionRepository.class);
    var service = new RubricPublicationService(rubrics, mock(AuditRepository.class), mock(CalibrationExpirationService.class));
    UUID course = UUID.randomUUID(), version = UUID.randomUUID();
    when(rubrics.dimensionsOfDraft(course, version)).thenReturn(validDefinitions());
    when(rubrics.find(course, version)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.publish(course, version, actor)).isInstanceOf(ResourceNotFoundException.class).hasMessage("La rúbrica no existe en el curso");
  }

  @Test void publishesDraftExpiringCalibrationsAndAuditing() {
    var rubrics = mock(RubricVersionRepository.class);
    var audit = mock(AuditRepository.class);
    var expirations = mock(CalibrationExpirationService.class);
    var service = new RubricPublicationService(rubrics, audit, expirations);
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), family = UUID.randomUUID();
    when(rubrics.dimensionsOfDraft(course, version)).thenReturn(validDefinitions());
    when(rubrics.find(course, version)).thenReturn(Optional.of(versionWith(family, version)));
    when(rubrics.publishDraft(course, version)).thenReturn(true);
    service.publish(course, version, actor);
    org.mockito.Mockito.verify(expirations).expireByRubric(family);
    org.mockito.Mockito.verify(audit).record(org.mockito.ArgumentMatchers.eq("rubric.published"),
        org.mockito.ArgumentMatchers.eq("rubric-version"), org.mockito.ArgumentMatchers.eq(version),
        org.mockito.ArgumentMatchers.eq(actor), org.mockito.ArgumentMatchers.any());
  }

  @Test void rejectsPublishWhenDraftChangedConcurrently() {
    var rubrics = mock(RubricVersionRepository.class);
    var service = new RubricPublicationService(rubrics, mock(AuditRepository.class), mock(CalibrationExpirationService.class));
    UUID course = UUID.randomUUID(), version = UUID.randomUUID(), family = UUID.randomUUID();
    when(rubrics.dimensionsOfDraft(course, version)).thenReturn(validDefinitions());
    when(rubrics.find(course, version)).thenReturn(Optional.of(versionWith(family, version)));
    when(rubrics.publishDraft(course, version)).thenReturn(false);
    assertThatThrownBy(() -> service.publish(course, version, actor))
        .isInstanceOf(IllegalStateException.class).hasMessage("La rúbrica fue modificada mientras se publicaba");
  }

  @Test void requiresAnchorsForEveryDimension() {
    assertThatThrownBy(() -> RubricPublicationService.validateAnchors(List.of(dimensionWith(null))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Cada dimensión debe definir anclas baja, media y alta completas");
  }

  @Test void rejectsMissingLowAnchor() {
    assertThatThrownBy(() -> RubricPublicationService.validateAnchors(List.of(dimensionWith(anchs(null, med(), high())))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void rejectsAnchorWithoutBehavior() {
    assertThatThrownBy(() -> RubricPublicationService.validateAnchors(List.of(dimensionWith(anchs(new Anchor(null, 25, "ejemplo"), med(), high())))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void rejectsAnchorWithBlankBehavior() {
    assertThatThrownBy(() -> RubricPublicationService.validateAnchors(List.of(dimensionWith(anchs(new Anchor(" ", 25, "ejemplo"), med(), high())))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void rejectsAnchorWithoutExample() {
    assertThatThrownBy(() -> RubricPublicationService.validateAnchors(List.of(dimensionWith(anchs(new Anchor("bajo", 25, null), med(), high())))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void rejectsAnchorWithBlankExample() {
    assertThatThrownBy(() -> RubricPublicationService.validateAnchors(List.of(dimensionWith(anchs(new Anchor("bajo", 25, " "), med(), high())))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void rejectsAnchorWithoutReferenceScore() {
    assertThatThrownBy(() -> RubricPublicationService.validateAnchors(List.of(dimensionWith(anchs(new Anchor("bajo", null, "ejemplo"), med(), high())))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void rejectsNegativeReferenceScore() {
    assertThatThrownBy(() -> RubricPublicationService.validateAnchors(List.of(dimensionWith(anchs(new Anchor("bajo", -1, "ejemplo"), med(), high())))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void rejectsReferenceScoreAboveOneHundred() {
    assertThatThrownBy(() -> RubricPublicationService.validateAnchors(List.of(dimensionWith(anchs(new Anchor("bajo", 101, "ejemplo"), med(), high())))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test void rejectsNonIncreasingReferenceScores() {
    assertThatThrownBy(() -> RubricPublicationService.validateAnchors(List.of(dimensionWith(anchs(new Anchor("bajo", 90, "ejemplo"), med(), high())))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Los puntajes de ancla deben ser crecientes: bajo, medio y alto");
  }

  @Test void acceptsCompleteIncreasingAnchors() {
    assertThatCode(() -> RubricPublicationService.validateAnchors(List.of(dimensionWith(anchs(low(), med(), high()))))).doesNotThrowAnyException();
  }

  private Anchors anchs(Anchor low, Anchor medium, Anchor high) { return new Anchors(low, medium, high); }
  private Anchor low() { return new Anchor("bajo", 25, "ejemplo bajo"); }
  private Anchor med() { return new Anchor("medio", 60, "ejemplo medio"); }
  private Anchor high() { return new Anchor("alto", 90, "ejemplo alto"); }
  private DimensionInput dimensionWith(Anchors anchors) { return new DimensionInput(Dimension.AUTONOMY, "Autonomía", "criterio", anchors, BigDecimal.valueOf(20)); }

  private List<DimensionDefinition> validDefinitions() {
    return List.of(def(Dimension.AUTONOMY, 30), def(Dimension.CLARITY, 25), def(Dimension.PROGRESSION, 20),
        def(Dimension.COMPLIANCE, 15), def(Dimension.EFFICIENCY, 10));
  }

  private RubricVersion versionWith(UUID family, UUID id) {
    return new RubricVersion(id, family, 1, "Rúbrica", "DRAFT", 1, null, List.of(
        dimensionWith(anchs(low(), med(), high())), dim(Dimension.CLARITY), dim(Dimension.PROGRESSION),
        dim(Dimension.COMPLIANCE), dim(Dimension.EFFICIENCY)));
  }

  private DimensionInput dim(Dimension key) {
    return new DimensionInput(key, key.name(), "criterio", anchs(low(), med(), high()), BigDecimal.valueOf(20));
  }

  private DimensionDefinition def(Dimension key, int weight) { return new DimensionDefinition(key, BigDecimal.valueOf(weight)); }
}