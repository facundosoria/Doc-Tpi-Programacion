package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.CaseScores;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.calibration.CalibrationRun;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetCaseDetail;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationCaseResultRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseGoldenSetRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** La pieza que le faltaba a `LLM-S01-H10`/`LLM-S03-H01`: corre un run `RUNNING` de punta a
 * punta — invoca {@link ModelInvocationService} (función {@link ModelFunction#EVALUATOR}) por
 * cada caso del golden set, persiste `calibration_case_results`, calcula PAR-14 con
 * {@link CalibrationMetrics#assess} (sin tocarlo) y cierra el run con
 * {@link CalibrationWorkflowService#finish}. Antes de esta historia, `CalibrationRunWorker` solo
 * transicionaba `QUEUED → RUNNING` y ahí se detenía para siempre. */
@Component
public class CalibrationEvaluationRunner {
  private static final Logger log = LoggerFactory.getLogger(CalibrationEvaluationRunner.class);
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  private final CalibrationRunRepository runs;
  private final RubricVersionRepository rubrics;
  private final CourseGoldenSetRepository goldenSets;
  private final CalibrationCaseResultRepository caseResults;
  private final ModelInvocationService models;
  private final CalibrationWorkflowService workflow;
  private final ObjectMapper mapper;
  private final Duration timeout;

  public CalibrationEvaluationRunner(CalibrationRunRepository runs, RubricVersionRepository rubrics,
      CourseGoldenSetRepository goldenSets, CalibrationCaseResultRepository caseResults,
      ModelInvocationService models, CalibrationWorkflowService workflow, ObjectMapper mapper,
      @Value("${llm.calibrations.evaluation-timeout-ms:8000}") long timeoutMs) {
    this.runs = runs;
    this.rubrics = rubrics;
    this.goldenSets = goldenSets;
    this.caseResults = caseResults;
    this.models = models;
    this.workflow = workflow;
    this.mapper = mapper;
    this.timeout = Duration.ofMillis(timeoutMs);
  }

  @Transactional
  public void run(UUID runId) {
    var run = runs.findById(runId).orElse(null);
    if (run == null) {
      log.warn("Calibration run {} disappeared before it could be evaluated", runId);
      return;
    }
    try {
      evaluate(runId, run);
    } catch (Exception exception) {
      log.warn("Calibration run {} failed while evaluating cases: {}", runId, exception.getMessage());
      workflow.finish(runId, false);
    }
  }

  private void evaluate(UUID runId, CalibrationRun run) {
    var rendered = EvaluatorPrompt.render(rubrics.weightsAndPrompts(run.rubricVersionId()));
    Map<Dimension, Integer> weights = rendered.weights();
    String systemPrompt = rendered.systemPrompt();

    List<GoldenSetCaseDetail> cases = goldenSets.casesOf(run.goldenSetVersionId());
    List<CaseScores> caseScores = new ArrayList<>();
    for (GoldenSetCaseDetail testCase : cases) {
      caseScores.add(evaluateCase(runId, testCase, systemPrompt, weights));
    }

    CalibrationMetrics.Result result = CalibrationMetrics.assess(caseScores, weights);
    runs.recordProgress(runId, 100, result.maeFinal().doubleValue(), result.maxIndividualError());
    workflow.finish(runId, result.passed());
  }

  private CaseScores evaluateCase(UUID runId, GoldenSetCaseDetail testCase, String systemPrompt, Map<Dimension, Integer> weights) {
    String userPrompt = EvaluatorPrompt.userPrompt(testCase.challengeContext(), testCase.transcript());
    var result = models.invoke(ModelFunction.EVALUATOR, systemPrompt, userPrompt, timeout);

    Map<Dimension, Integer> humanScores = EvaluatorPrompt.scoresFrom(testCase.referenceScores());
    Map<Dimension, Integer> modelScores = EvaluatorPrompt.scoresFrom(parse(result.text()));

    BigDecimal humanFinal = BigDecimal.ZERO;
    BigDecimal modelFinal = BigDecimal.ZERO;
    Map<Dimension, Integer> dimensionErrors = new EnumMap<>(Dimension.class);
    for (Dimension dimension : Dimension.values()) {
      int human = humanScores.get(dimension);
      int model = modelScores.get(dimension);
      dimensionErrors.put(dimension, Math.abs(model - human));
      BigDecimal factor = BigDecimal.valueOf(weights.get(dimension)).divide(ONE_HUNDRED);
      humanFinal = humanFinal.add(BigDecimal.valueOf(human).multiply(factor));
      modelFinal = modelFinal.add(BigDecimal.valueOf(model).multiply(factor));
    }
    BigDecimal finalError = humanFinal.subtract(modelFinal).abs();

    caseResults.record(runId, testCase.id(), toJson(modelScores), humanFinal, modelFinal, toJson(dimensionErrors), finalError);
    return new CaseScores(humanScores, modelScores);
  }

  private JsonNode parse(String text) {
    try {
      return mapper.readTree(text);
    } catch (Exception exception) {
      throw new IllegalStateException("La respuesta del evaluador no es JSON válido", exception);
    }
  }

  private String toJson(Map<Dimension, Integer> scores) {
    ObjectNode node = mapper.createObjectNode();
    scores.forEach((dimension, value) -> node.put(dimension.name().toLowerCase(Locale.ROOT), value));
    return node.toString();
  }
}
