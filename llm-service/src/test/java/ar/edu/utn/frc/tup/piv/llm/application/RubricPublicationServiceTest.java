package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.domain.RubricValidator.DimensionDefinition;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RubricPublicationServiceTest {
  @Mock private RubricVersionRepository rubrics;
  @Mock private AuditRepository audit;

  @Test void publishesOnlyAfterTheMandatoryDimensionsAndWeightsAreValid() {
    UUID courseId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
    CallerIdentity actor = new CallerIdentity("admin-service", UUID.randomUUID(), "request", null);
    when(rubrics.dimensionsOfDraft(courseId, versionId)).thenReturn(validDimensions());
    when(rubrics.publishDraft(courseId, versionId)).thenReturn(true);

    new RubricPublicationService(rubrics, audit).publish(courseId, versionId, actor);

    verify(rubrics).publishDraft(courseId, versionId);
    verify(audit).record(eq("rubric.published"), eq("rubric-version"), eq(versionId), eq(actor), anyString());
  }

  @Test void doesNotPublishWhenWeightsDoNotTotalOneHundred() {
    UUID courseId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
    when(rubrics.dimensionsOfDraft(courseId, versionId)).thenReturn(List.of(
        dimension(Dimension.AUTONOMY, 30), dimension(Dimension.CLARITY, 25), dimension(Dimension.PROGRESSION, 20),
        dimension(Dimension.COMPLIANCE, 15), dimension(Dimension.EFFICIENCY, 9)));

    assertThatThrownBy(() -> new RubricPublicationService(rubrics, audit).publish(courseId, versionId,
        new CallerIdentity("admin-service", UUID.randomUUID(), null, null)))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Rubric weights must total 100");

    verify(rubrics, never()).publishDraft(courseId, versionId);
    verify(audit, never()).record(anyString(), anyString(), eq(versionId), org.mockito.ArgumentMatchers.any(), anyString());
  }

  @Test void doesNotPublishWhenRubricIsNotDraftOrWasModifiedConcurrently() {
    UUID courseId = UUID.randomUUID(); UUID versionId = UUID.randomUUID();
    when(rubrics.dimensionsOfDraft(courseId, versionId)).thenReturn(validDimensions());
    when(rubrics.publishDraft(courseId, versionId)).thenReturn(false);

    assertThatThrownBy(() -> new RubricPublicationService(rubrics, audit).publish(courseId, versionId,
        new CallerIdentity("admin-service", UUID.randomUUID(), null, null)))
        .isInstanceOf(IllegalStateException.class).hasMessage("La rúbrica fue modificada mientras se publicaba");

    verify(audit, never()).record(anyString(), anyString(), eq(versionId), org.mockito.ArgumentMatchers.any(), anyString());
  }

  private List<DimensionDefinition> validDimensions() {
    return List.of(dimension(Dimension.AUTONOMY, 30), dimension(Dimension.CLARITY, 25),
        dimension(Dimension.PROGRESSION, 20), dimension(Dimension.COMPLIANCE, 15), dimension(Dimension.EFFICIENCY, 10));
  }

  private DimensionDefinition dimension(Dimension key, int weight) {
    return new DimensionDefinition(key, BigDecimal.valueOf(weight));
  }
}
