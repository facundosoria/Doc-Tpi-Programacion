package ar.edu.utn.frc.tup.piv.llm.shadow.application;

import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ShadowRunServiceTest {
  private final ShadowRunStore store = mock(ShadowRunStore.class);
  private final AuditRepository audit = mock(AuditRepository.class);
  private final ShadowRunService service = new ShadowRunService(store, audit, 100, 10);
  private final UUID course = UUID.randomUUID();
  private final UUID baseline = UUID.randomUUID();
  private final UUID candidate = UUID.randomUUID();
  private final UUID key = UUID.randomUUID();
  private final CallerIdentity actor = new CallerIdentity("admin-service", UUID.randomUUID(), "req", null);

  private ShadowRunService.Command command(ShadowRun.Source source, UUID golden, Integer size, Double threshold) {
    return new ShadowRunService.Command(candidate, source, golden, size, threshold);
  }

  private void healthyCourse() {
    when(store.activeRubricVersion(course)).thenReturn(Optional.of(baseline));
    when(store.rubricVersionVisibleToCourse(course, candidate)).thenReturn(true);
    when(store.goldenSetVersionVisibleToCourse(any(), any())).thenReturn(true);
    when(store.create(any())).thenAnswer(inv -> {
      var n = (ShadowRunStore.NewRun) inv.getArgument(0);
      return new ShadowRun(UUID.randomUUID(), n.courseId(), n.baselineRubricVersionId(), n.candidateRubricVersionId(),
          n.source(), n.goldenSetVersionId(), n.sampleSize(), n.divergenceThreshold(), ShadowRun.State.QUEUED, 0, null,
          null, n.createdByUserId(), OffsetDateTime.now(), null, null);
    });
  }

  @Test
  void queuesARunAgainstTheCourseActiveRubricAndAuditsIt() {
    healthyCourse();

    var run = service.enqueue(course, command(ShadowRun.Source.TUTOR_CONVERSATIONS, null, 20, 7.5), key, actor);

    assertThat(run.state()).isEqualTo(ShadowRun.State.QUEUED);
    var created = ArgumentCaptor.forClass(ShadowRunStore.NewRun.class);
    verify(store).create(created.capture());
    assertThat(created.getValue().baselineRubricVersionId()).isEqualTo(baseline);
    assertThat(created.getValue().candidateRubricVersionId()).isEqualTo(candidate);
    assertThat(created.getValue().sampleSize()).isEqualTo(20);
    assertThat(created.getValue().divergenceThreshold()).isEqualTo(7.5);
    assertThat(created.getValue().createdByUserId()).isEqualTo(actor.delegatedUserId());
    verify(audit).record(org.mockito.ArgumentMatchers.eq("shadow.queued"), org.mockito.ArgumentMatchers.eq("shadow-run"),
        org.mockito.ArgumentMatchers.eq(run.id()), org.mockito.ArgumentMatchers.eq(actor), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void usesTheConfiguredDefaultsWhenTheRequestOmitsThem() {
    healthyCourse();

    service.enqueue(course, command(ShadowRun.Source.TUTOR_CONVERSATIONS, null, null, null), key, actor);

    var created = ArgumentCaptor.forClass(ShadowRunStore.NewRun.class);
    verify(store).create(created.capture());
    assertThat(created.getValue().sampleSize()).isEqualTo(100);
    assertThat(created.getValue().divergenceThreshold()).isEqualTo(10.0);
  }

  @Test
  void aCourseWithoutActiveCalibrationHasNoBaselineToCompareAgainst() {
    when(store.activeRubricVersion(course)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.enqueue(course, command(ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5, null), key, actor))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("calibración activa");
    verify(store, never()).create(any());
  }

  @Test
  void theCandidateCannotBeTheActiveRubric() {
    when(store.activeRubricVersion(course)).thenReturn(Optional.of(candidate));

    assertThatThrownBy(() -> service.enqueue(course, command(ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5, null), key, actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("la misma");
  }

  @Test
  void aCandidateFromAnotherCourseIsRejected() {
    healthyCourse();
    when(store.rubricVersionVisibleToCourse(course, candidate)).thenReturn(false);

    assertThatThrownBy(() -> service.enqueue(course, command(ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5, null), key, actor))
        .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("candidata no existe");
    verify(store, never()).create(any());
  }

  @Test
  void aGoldenSetFromAnotherCourseIsRejected() {
    healthyCourse();
    var golden = UUID.randomUUID();
    when(store.goldenSetVersionVisibleToCourse(course, golden)).thenReturn(false);

    assertThatThrownBy(() -> service.enqueue(course, command(ShadowRun.Source.GOLDEN_SET, golden, 5, null), key, actor))
        .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("golden set");
  }

  @Test
  void validatesTheRequestShape() {
    healthyCourse();

    assertThatThrownBy(() -> service.enqueue(course, command(ShadowRun.Source.GOLDEN_SET, null, 5, null), key, actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("goldenSetVersionId");
    assertThatThrownBy(() -> service.enqueue(course, command(ShadowRun.Source.TUTOR_CONVERSATIONS, UUID.randomUUID(), 5, null), key, actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("solo aplica");
    assertThatThrownBy(() -> service.enqueue(course, command(ShadowRun.Source.TUTOR_CONVERSATIONS, null, 101, null), key, actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("entre 1 y 100");
    assertThatThrownBy(() -> service.enqueue(course, command(ShadowRun.Source.TUTOR_CONVERSATIONS, null, 0, null), key, actor))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.enqueue(course, command(ShadowRun.Source.TUTOR_CONVERSATIONS, null, 5, 0.0), key, actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("divergenceThreshold");
    assertThatThrownBy(() -> service.enqueue(course, new ShadowRunService.Command(null, null, null, 5, null), key, actor))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("obligatorios");
    verify(store, never()).create(any());
  }

  @Test
  void aRunOfAnotherCourseIsNotVisible() {
    var other = UUID.randomUUID();
    when(store.find(other, UUID.randomUUID())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(other, UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("no existe en el curso");
  }
}
