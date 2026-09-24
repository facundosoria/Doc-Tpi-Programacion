package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationEvaluationRunner;
import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationWorkflowService;
import ar.edu.utn.frc.tup.piv.llm.application.service.ModelInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.InvalidModelResponseException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.domain.calibration.CalibrationRun;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseDetail;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationCaseResultRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseGoldenSetRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Los escenarios de `LLM-S03-H01` que hasta esta historia estaban marcados "no demostrable hoy":
 * una calibración corrida de punta a punta termina PASSED o FAILED según PAR-14, y un error de
 * invocación no deja el run colgado en RUNNING. */
class CalibrationEvaluationRunnerTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final UUID runId = UUID.randomUUID();
  private final UUID rubricVersionId = UUID.randomUUID();
  private final UUID goldenSetVersionId = UUID.randomUUID();

  @Test
  void aRunWhoseModelScoresAreCloseToHumanScoresEndsPassed() throws Exception {
    var caseResults = mock(CalibrationCaseResultRepository.class);
    var models = mock(ModelInvocationService.class);
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenReturn(new ModelInvocationResult(
            "{\"autonomy\":71,\"clarity\":71,\"progression\":71,\"compliance\":71,\"efficiency\":71}", "fake", "fake-evaluator-v1"));
    var workflow = mock(CalibrationWorkflowService.class);
    var runner = runnerWith(caseResults, models, workflow, humanScoresAllSeventy());

    runner.run(runId);

    verify(workflow).finish(runId, true);
    verify(caseResults).record(eq(runId), any(), anyString(), any(), any(), anyString(), any());
  }

  @Test
  void aRunWhoseModelScoresDeviateFarFromHumanScoresEndsFailed() {
    var caseResults = mock(CalibrationCaseResultRepository.class);
    var models = mock(ModelInvocationService.class);
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenReturn(new ModelInvocationResult(
            "{\"autonomy\":10,\"clarity\":10,\"progression\":10,\"compliance\":10,\"efficiency\":10}", "fake", "fake-evaluator-v1"));
    var workflow = mock(CalibrationWorkflowService.class);
    var runner = runnerWith(caseResults, models, workflow, humanScoresAllSeventy());

    runner.run(runId);

    verify(workflow).finish(runId, false);
  }

  @Test
  void aModelInvocationFailureEndsTheRunFailedInsteadOfHangingInRunning() {
    var caseResults = mock(CalibrationCaseResultRepository.class);
    var models = mock(ModelInvocationService.class);
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenThrow(new InvalidModelResponseException("boom"));
    var workflow = mock(CalibrationWorkflowService.class);
    var runner = runnerWith(caseResults, models, workflow, humanScoresAllSeventy());

    runner.run(runId);

    verify(workflow).finish(runId, false);
    verify(caseResults, never()).record(any(), any(), anyString(), any(), any(), anyString(), any());
  }

  @Test
  void aRunThatDisappearedBeforeEvaluationIsSkippedSilently() {
    var runs = mock(CalibrationRunRepository.class);
    when(runs.findById(runId)).thenReturn(Optional.empty());
    var workflow = mock(CalibrationWorkflowService.class);
    var runner = new CalibrationEvaluationRunner(runs, mock(RubricVersionRepository.class),
        mock(CourseGoldenSetRepository.class), mock(CalibrationCaseResultRepository.class),
        mock(ModelInvocationService.class), workflow, mapper, 1000);

    runner.run(runId);

    verify(workflow, never()).finish(any(), org.mockito.ArgumentMatchers.anyBoolean());
  }

  private CalibrationEvaluationRunner runnerWith(CalibrationCaseResultRepository caseResults, ModelInvocationService models,
      CalibrationWorkflowService workflow, String humanScoresJson) {
    var runs = mock(CalibrationRunRepository.class);
    when(runs.findById(runId)).thenReturn(Optional.of(
        new CalibrationRun(runId, "RUNNING", 0, rubricVersionId, goldenSetVersionId, UUID.randomUUID(), null, null, "MANUAL", null, null)));

    var rubrics = mock(RubricVersionRepository.class);
    when(rubrics.weightsAndPrompts(rubricVersionId)).thenReturn(List.of(
        dimension(Dimension.AUTONOMY, 30), dimension(Dimension.CLARITY, 25), dimension(Dimension.PROGRESSION, 20),
        dimension(Dimension.COMPLIANCE, 15), dimension(Dimension.EFFICIENCY, 10)));

    var goldenSets = mock(CourseGoldenSetRepository.class);
    try {
      var testCase = new GoldenSetCaseDetail(UUID.randomUUID(), mapper.readTree("[{\"role\":\"alumno\",\"content\":\"hola\"}]"),
          mapper.readTree("{\"desafioId\":\"d1\"}"), mapper.readTree(humanScoresJson));
      when(goldenSets.casesOf(goldenSetVersionId)).thenReturn(List.of(testCase));
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }

    return new CalibrationEvaluationRunner(runs, rubrics, goldenSets, caseResults, models, workflow, mapper, 1000);
  }

  private String humanScoresAllSeventy() {
    return "{\"autonomy\":70,\"clarity\":70,\"progression\":70,\"compliance\":70,\"efficiency\":70}";
  }

  private RubricDraftService.DimensionInput dimension(Dimension key, int weight) {
    return new RubricDraftService.DimensionInput(key, key.name(), "criterio de " + key,
        new RubricDraftService.Anchors(
            new RubricDraftService.Anchor("bajo", 25, "ejemplo bajo"),
            new RubricDraftService.Anchor("medio", 60, "ejemplo medio"),
            new RubricDraftService.Anchor("alto", 90, "ejemplo alto")),
        BigDecimal.valueOf(weight));
  }
}
