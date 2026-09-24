package ar.edu.utn.frc.tup.piv.llm.shadow.application;

import ar.edu.utn.frc.tup.piv.llm.application.service.EvaluatorPrompt;
import ar.edu.utn.frc.tup.piv.llm.application.service.ModelInvocationService;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.application.service.RealCaseAnonymizer;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowMetrics;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowRun;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Ejecuta una corrida de shadow: por cada transcripción evalúa con la rúbrica baseline y con la
 * candidata (misma función EVALUATOR, mismo modelo, así la diferencia es atribuible a la rúbrica),
 * guarda ambos puntajes y al final resume la comparación.
 *
 * <p>La salida se DESCARTA por construcción: el único destino de escritura es {@link ShadowRunStore}.
 * Este runner no depende de outbox, Kafka ni de las tablas de calibración/evaluación (lo verifica
 * {@code ArchitectureTest}). Antes de salir hacia el proveedor cada transcripción se anonimiza. */
@Component
public class ShadowEvaluationRunner {
  private static final Logger log = LoggerFactory.getLogger(ShadowEvaluationRunner.class);

  private final ShadowRunStore store;
  private final RubricVersionRepository rubrics;
  private final ModelInvocationService models;
  private final Map<ShadowRun.Source, ShadowSampleSource> sources;
  private final ObjectMapper mapper;
  private final Duration timeout;
  private final ShadowMetrics.Criteria criteriaTemplate;
  private final double maxFailedShare;

  public ShadowEvaluationRunner(ShadowRunStore store, RubricVersionRepository rubrics, ModelInvocationService models,
      List<ShadowSampleSource> sources, ObjectMapper mapper,
      @Value("${llm.shadow.evaluation-timeout-ms:8000}") long timeoutMs,
      @Value("${llm.shadow.max-mae:5}") double maxMae,
      @Value("${llm.shadow.max-abs-bias:3}") double maxAbsBias,
      @Value("${llm.shadow.max-failed-share:0.2}") double maxFailedShare) {
    this.store = store;
    this.rubrics = rubrics;
    this.models = models;
    this.sources = sources.stream().collect(Collectors.toMap(ShadowSampleSource::source, Function.identity()));
    this.mapper = mapper;
    this.timeout = Duration.ofMillis(timeoutMs);
    this.criteriaTemplate = new ShadowMetrics.Criteria(0, maxMae, maxAbsBias, maxFailedShare);
    this.maxFailedShare = maxFailedShare;
  }

  public void execute(UUID runId) {
    var run = store.findById(runId).orElse(null);
    if (run == null) {
      log.warn("Shadow run {} desapareció antes de ejecutarse", runId);
      return;
    }
    try {
      evaluate(run);
    } catch (Exception failure) {
      log.warn("Shadow run {} falló: {}", runId, failure.getMessage());
      store.fail(runId, "SHADOW_EXECUTION_FAILED");
    }
  }

  private void evaluate(ShadowRun run) {
    var source = sources.get(run.source());
    if (source == null) {
      throw new IllegalStateException("No hay fuente para " + run.source());
    }
    var baseline = EvaluatorPrompt.render(rubrics.weightsAndPrompts(run.baselineRubricVersionId()));
    var candidate = EvaluatorPrompt.render(rubrics.weightsAndPrompts(run.candidateRubricVersionId()));
    if (baseline.weights().size() != Dimension.values().length || candidate.weights().size() != Dimension.values().length) {
      store.fail(run.id(), "INVALID_RUBRIC");
      return;
    }
    List<ShadowSampleSource.Sample> samples = source.load(run);
    if (samples.isEmpty()) {
      store.fail(run.id(), "NO_SAMPLES");
      return;
    }

    List<ShadowMetrics.CaseOutcome> outcomes = new ArrayList<>();
    int done = 0;
    for (var sample : samples) {
      var outcome = evaluateSample(sample, baseline, candidate);
      store.recordCase(run.id(), outcome);
      outcomes.add(outcome);
      store.recordProgress(run.id(), ++done * 100 / samples.size());
    }

    var criteria = new ShadowMetrics.Criteria(run.divergenceThreshold(), criteriaTemplate.maxMae(),
        criteriaTemplate.maxAbsBias(), maxFailedShare);
    store.complete(run.id(), ShadowMetrics.summarize(outcomes, baseline.weights(), candidate.weights(), criteria));
  }

  private ShadowMetrics.CaseOutcome evaluateSample(ShadowSampleSource.Sample sample, EvaluatorPrompt.Rendered baseline,
      EvaluatorPrompt.Rendered candidate) {
    Map<Dimension, Integer> human = sample.humanScores() == null ? null : safeScores(sample.humanScores());
    try {
      JsonNode transcript = RealCaseAnonymizer.anonymize(sample.transcript());
      JsonNode context = RealCaseAnonymizer.anonymize(sample.challengeContext());
      String userPrompt = EvaluatorPrompt.userPrompt(context, transcript);
      var base = EvaluatorPrompt.scoresFrom(parse(models.invoke(ModelFunction.EVALUATOR, baseline.systemPrompt(), userPrompt, timeout).text()));
      var cand = EvaluatorPrompt.scoresFrom(parse(models.invoke(ModelFunction.EVALUATOR, candidate.systemPrompt(), userPrompt, timeout).text()));
      return new ShadowMetrics.CaseOutcome(sample.sourceRef(), base, cand, human, null);
    } catch (Exception failure) {
      // Un caso fallido no tumba la corrida: queda contado y excluido de las métricas.
      log.info("Shadow: el caso {} falló: {}", sample.sourceRef(), failure.getMessage());
      return new ShadowMetrics.CaseOutcome(sample.sourceRef(), null, null, human, "EVALUATION_FAILED");
    }
  }

  private Map<Dimension, Integer> safeScores(JsonNode node) {
    try {
      return EvaluatorPrompt.scoresFrom(node);
    } catch (RuntimeException invalid) {
      return null;
    }
  }

  private JsonNode parse(String text) {
    try {
      return mapper.readTree(text);
    } catch (Exception invalid) {
      throw new IllegalStateException("La respuesta del evaluador no es JSON válido", invalid);
    }
  }
}
