package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.InvalidModelResponseException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.domain.evaluation.AttemptScore;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.KafkaEventProducer;
import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Evalúa un intento cerrado (`ATTEMPT_CLOSED`, Tema 05) con la función EVALUATOR del gateway y
 * publica el resultado en `evaluation-events`: `SCORE_CALCULATED` con el score 0-100 y su desglose,
 * o `SCORE_DEFERRED` si no se pudo evaluar. Contra el adaptador `fake` funciona sin proveedor real.
 *
 * <p><b># fixture provisorio</b> — el payload de ambos eventos es una propuesta nuestra, todavía sin
 * validar con Tema 05 (ver `docs/.../tema-05-desafios-practicos/pendientes.md`). Tampoco hay
 * forma de saber a qué curso pertenece una cohorte, así que la rúbrica sale de la configuración
 * (`llm.evaluation.rubric-version-id`), no de la calibración activa del curso.
 *
 * <p>El evento se encola en el outbox: hay que llamarlo dentro de la transacción del consumidor. */
@Service
public class AttemptEvaluationService {
  public static final String SCORE_CALCULATED = "SCORE_CALCULATED";
  public static final String SCORE_DEFERRED = "SCORE_DEFERRED";
  static final String RUBRIC_UNAVAILABLE = "RUBRIC_UNAVAILABLE";
  static final String INVALID_MODEL_RESPONSE = "INVALID_MODEL_RESPONSE";
  static final String MODEL_UNAVAILABLE = "MODEL_UNAVAILABLE";
  private static final Logger log = LoggerFactory.getLogger(AttemptEvaluationService.class);

  /** Lo que necesitamos de `ATTEMPT_CLOSED`; la transcripción viaja tal cual llegó, sin truncar. */
  public record ClosedAttempt(UUID attemptId, UUID courseCohortId, UUID learnerId, JsonNode transcript) {}

  public record EvaluatorInfo(String provider, String model) {}

  public record ScoreCalculated(UUID attemptId, UUID courseCohortId, UUID learnerId, UUID rubricVersionId, int score,
      Map<String, Integer> dimensions, EvaluatorInfo evaluator) {}

  public record ScoreDeferred(UUID attemptId, UUID courseCohortId, UUID learnerId, String reason, String retryFrom) {}

  private final RubricVersionRepository rubrics;
  private final ModelInvocationService models;
  private final KafkaEventProducer events;
  private final ObjectMapper mapper;
  private final UUID rubricVersionId;
  private final Duration timeout;
  private final Duration retryAfter;

  public AttemptEvaluationService(RubricVersionRepository rubrics, ModelInvocationService models,
      KafkaEventProducer events, ObjectMapper mapper,
      @Value("${llm.evaluation.rubric-version-id:10000000-0000-0000-0000-000000000002}") UUID rubricVersionId,
      @Value("${llm.evaluation.timeout-ms:8000}") long timeoutMs,
      @Value("${llm.evaluation.retry-after-minutes:30}") long retryAfterMinutes) {
    this.rubrics = rubrics;
    this.models = models;
    this.events = events;
    this.mapper = mapper;
    this.rubricVersionId = rubricVersionId;
    this.timeout = Duration.ofMillis(timeoutMs);
    this.retryAfter = Duration.ofMinutes(retryAfterMinutes);
  }

  public void evaluate(ClosedAttempt attempt) {
    var rubric = EvaluatorPrompt.render(rubrics.weightsAndPrompts(rubricVersionId));
    try {
      if (rubric.weights().size() != Dimension.values().length) {
        throw new Deferral(RUBRIC_UNAVAILABLE, "la rúbrica " + rubricVersionId + " no define las cinco dimensiones");
      }
      var result = invokeModel(rubric, attempt);
      publishScore(attempt, result, scoreFrom(result, rubric));
    } catch (Deferral deferral) {
      log.warn("Intento {} diferido [reason={}]: {}", attempt.attemptId(), deferral.reason, deferral.getMessage());
      events.enqueue(KafkaTopics.EVALUATION_EVENTS, attempt.courseCohortId().toString(), SCORE_DEFERRED,
          new ScoreDeferred(attempt.attemptId(), attempt.courseCohortId(), attempt.learnerId(), deferral.reason,
              Instant.now().plus(retryAfter).toString()));
    }
  }

  private ModelInvocationResult invokeModel(EvaluatorPrompt.Rendered rubric, ClosedAttempt attempt) {
    var userPrompt = EvaluatorPrompt.userPrompt(mapper.createObjectNode(), attempt.transcript());
    try {
      return models.invoke(ModelFunction.EVALUATOR, rubric.systemPrompt(), userPrompt, timeout);
    } catch (InvalidModelResponseException invalid) {
      throw new Deferral(INVALID_MODEL_RESPONSE, invalid.getMessage());
    } catch (RuntimeException unavailable) {
      throw new Deferral(MODEL_UNAVAILABLE, unavailable.getMessage());
    }
  }

  private AttemptScore scoreFrom(ModelInvocationResult result, EvaluatorPrompt.Rendered rubric) {
    try {
      return AttemptScore.of(EvaluatorPrompt.scoresFrom(mapper.readTree(result.text())), rubric.weights());
    } catch (Exception invalid) {
      throw new Deferral(INVALID_MODEL_RESPONSE, "la respuesta del evaluador no tiene las cinco dimensiones: " + invalid.getMessage());
    }
  }

  private void publishScore(ClosedAttempt attempt, ModelInvocationResult result, AttemptScore score) {
    Map<String, Integer> dimensions = new LinkedHashMap<>();
    for (Dimension dimension : Dimension.values()) {
      dimensions.put(dimension.name().toLowerCase(Locale.ROOT), score.dimensions().get(dimension));
    }
    events.enqueue(KafkaTopics.EVALUATION_EVENTS, attempt.courseCohortId().toString(), SCORE_CALCULATED,
        new ScoreCalculated(attempt.attemptId(), attempt.courseCohortId(), attempt.learnerId(), rubricVersionId,
            score.overall(), dimensions, new EvaluatorInfo(result.provider(), result.model())));
    log.info("Intento {} evaluado [score={}, proveedor={}]", attempt.attemptId(), score.overall(), result.provider());
  }

  /** Motivo por el que el intento no se pudo evaluar ahora; se traduce en `SCORE_DEFERRED`. */
  private static final class Deferral extends RuntimeException {
    private final String reason;

    Deferral(String reason, String detail) {
      super(detail);
      this.reason = reason;
    }
  }
}
