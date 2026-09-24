package ar.edu.utn.frc.tup.piv.llm.shadow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.service.ModelInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowMetrics;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ShadowEvaluationRunnerTest {
  private static final String SCORES = "{\"autonomy\":70,\"clarity\":70,\"progression\":70,\"compliance\":70,\"efficiency\":70}";

  private final ObjectMapper mapper = new ObjectMapper();
  private final ShadowRunStore store = mock(ShadowRunStore.class);
  private final RubricVersionRepository rubrics = mock(RubricVersionRepository.class);
  private final ModelInvocationService models = mock(ModelInvocationService.class);
  private final ShadowSampleSource source = mock(ShadowSampleSource.class);
  private final UUID baselineId = UUID.randomUUID();
  private final UUID candidateId = UUID.randomUUID();
  private final ShadowRun run = new ShadowRun(UUID.randomUUID(), UUID.randomUUID(), baselineId, candidateId,
      ShadowRun.Source.TUTOR_CONVERSATIONS, null, 10, 10, ShadowRun.State.RUNNING, 0, null, null, UUID.randomUUID(),
      OffsetDateTime.now(), OffsetDateTime.now(), null);

  private ShadowEvaluationRunner runner() {
    when(source.source()).thenReturn(ShadowRun.Source.TUTOR_CONVERSATIONS);
    when(store.findById(run.id())).thenReturn(Optional.of(run));
    when(rubrics.weightsAndPrompts(baselineId)).thenReturn(rubric("criterio BASELINE", 30, 25, 20, 15, 10));
    when(rubrics.weightsAndPrompts(candidateId)).thenReturn(rubric("criterio CANDIDATA", 30, 25, 20, 15, 10));
    return new ShadowEvaluationRunner(store, rubrics, models, List.of(source), mapper, 1000, 5, 3, 0.2);
  }

  @Test
  void evaluatesEverySampleWithBothRubricsAndStoresTheComparison() throws Exception {
    var runner = runner();
    when(source.load(run)).thenReturn(List.of(sample("c1"), sample("c2")));
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenReturn(new ModelInvocationResult(SCORES, "fake", "m"));

    runner.execute(run.id());

    var prompts = ArgumentCaptor.forClass(String.class);
    verify(models, times(4)).invoke(eq(ModelFunction.EVALUATOR), prompts.capture(), anyString(), any());
    assertThat(prompts.getAllValues()).as("por muestra: una vez con cada rúbrica")
        .containsExactly(prompts.getAllValues().get(0), prompts.getAllValues().get(1),
            prompts.getAllValues().get(0), prompts.getAllValues().get(1));
    assertThat(prompts.getAllValues().get(0)).contains("criterio BASELINE");
    assertThat(prompts.getAllValues().get(1)).contains("criterio CANDIDATA");
    verify(store, times(2)).recordCase(eq(run.id()), any());
    var summary = ArgumentCaptor.forClass(ShadowMetrics.Summary.class);
    verify(store).complete(eq(run.id()), summary.capture());
    assertThat(summary.getValue().comparedCases()).isEqualTo(2);
    assertThat(summary.getValue().mae()).isZero();
    verify(store, never()).fail(any(), anyString());
  }

  @Test
  void neverSendsIdentifiableDataToTheProvider() throws Exception {
    var runner = runner();
    var transcript = mapper.readTree("[{\"role\":\"STUDENT\",\"content\":\"soy Ana, escribime a ana.perez@alumno.edu.ar\",\"position\":0}]");
    when(source.load(run)).thenReturn(List.of(new ShadowSampleSource.Sample("c1", transcript, mapper.readTree("{\"statement\":\"x\"}"), null)));
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenReturn(new ModelInvocationResult(SCORES, "fake", "m"));

    runner.execute(run.id());

    var userPrompts = ArgumentCaptor.forClass(String.class);
    verify(models, times(2)).invoke(eq(ModelFunction.EVALUATOR), anyString(), userPrompts.capture(), any());
    assertThat(userPrompts.getAllValues()).allSatisfy(p -> assertThat(p).doesNotContain("ana.perez@alumno.edu.ar")
        .contains("[REDACTED_EMAIL]"));
  }

  @Test
  void aFailingSampleDoesNotSinkTheRunAndIsCountedAsFailed() throws Exception {
    var runner = runner();
    when(source.load(run)).thenReturn(List.of(sample("bad"), sample("good")));
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenReturn(new ModelInvocationResult("no es json", "fake", "m"))
        .thenReturn(new ModelInvocationResult(SCORES, "fake", "m"))
        .thenReturn(new ModelInvocationResult(SCORES, "fake", "m"));

    runner.execute(run.id());

    var outcomes = ArgumentCaptor.forClass(ShadowMetrics.CaseOutcome.class);
    verify(store, times(2)).recordCase(eq(run.id()), outcomes.capture());
    assertThat(outcomes.getAllValues().get(0).errorCode()).isEqualTo("EVALUATION_FAILED");
    assertThat(outcomes.getAllValues().get(1).failed()).isFalse();
    var summary = ArgumentCaptor.forClass(ShadowMetrics.Summary.class);
    verify(store).complete(eq(run.id()), summary.capture());
    assertThat(summary.getValue().failedCases()).isEqualTo(1);
    assertThat(summary.getValue().comparedCases()).isEqualTo(1);
  }

  @Test
  void aRunWithoutSamplesFailsWithAClearCode() {
    var runner = runner();
    when(source.load(run)).thenReturn(List.of());

    runner.execute(run.id());

    verify(store).fail(run.id(), "NO_SAMPLES");
    verify(models, never()).invoke(any(), anyString(), anyString(), any());
  }

  @Test
  void anIncompleteCandidateRubricIsRejectedBeforeCallingTheProvider() {
    var runner = runner();
    when(rubrics.weightsAndPrompts(candidateId)).thenReturn(List.of(dimension(Dimension.AUTONOMY, "solo una", 100)));

    runner.execute(run.id());

    verify(store).fail(run.id(), "INVALID_RUBRIC");
    verify(models, never()).invoke(any(), anyString(), anyString(), any());
    verify(source, never()).load(any());
  }

  @Test
  void anUnexpectedErrorEndsTheRunFailedInsteadOfLeavingItRunning() {
    var runner = runner();
    when(source.load(run)).thenThrow(new IllegalStateException("base caída"));

    runner.execute(run.id());

    verify(store).fail(run.id(), "SHADOW_EXECUTION_FAILED");
  }

  @Test
  void aRunThatDisappearedIsSkipped() {
    var runner = runner();
    var unknown = UUID.randomUUID();
    when(store.findById(unknown)).thenReturn(Optional.empty());

    runner.execute(unknown);

    verify(store, never()).fail(any(), anyString());
    verify(store, never()).recordProgress(any(), anyInt());
  }

  @Test
  void reportsProgressAsSamplesAreProcessed() {
    var runner = runner();
    when(source.load(run)).thenReturn(List.of(sample("c1"), sample("c2"), sample("c3"), sample("c4")));
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenReturn(new ModelInvocationResult(SCORES, "fake", "m"));

    runner.execute(run.id());

    verify(store).recordProgress(run.id(), 25);
    verify(store).recordProgress(run.id(), 50);
    verify(store).recordProgress(run.id(), 75);
    verify(store).recordProgress(run.id(), 100);
  }

  private ShadowSampleSource.Sample sample(String ref) {
    try {
      return new ShadowSampleSource.Sample(ref, mapper.readTree("[{\"role\":\"STUDENT\",\"content\":\"hola\",\"position\":0}]"),
          mapper.readTree("{\"statement\":\"desafío\"}"), null);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private List<RubricDraftService.DimensionInput> rubric(String criterion, int a, int c, int p, int co, int e) {
    return List.of(dimension(Dimension.AUTONOMY, criterion, a), dimension(Dimension.CLARITY, criterion, c),
        dimension(Dimension.PROGRESSION, criterion, p), dimension(Dimension.COMPLIANCE, criterion, co),
        dimension(Dimension.EFFICIENCY, criterion, e));
  }

  private RubricDraftService.DimensionInput dimension(Dimension key, String criterion, int weight) {
    return new RubricDraftService.DimensionInput(key, key.name(), criterion,
        new RubricDraftService.Anchors(new RubricDraftService.Anchor("bajo", 25, "b"),
            new RubricDraftService.Anchor("medio", 60, "m"), new RubricDraftService.Anchor("alto", 90, "a")),
        BigDecimal.valueOf(weight));
  }
}
