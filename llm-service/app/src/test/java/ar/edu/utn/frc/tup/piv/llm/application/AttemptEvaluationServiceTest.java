package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ModelDeploymentRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelDeploymentSummary;

import ar.edu.utn.frc.tup.piv.llm.application.service.AttemptEvaluationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.ModelInvocationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.service.AttemptEvaluationService.ClosedAttempt;
import ar.edu.utn.frc.tup.piv.llm.application.service.AttemptEvaluationService.ScoreCalculated;
import ar.edu.utn.frc.tup.piv.llm.application.service.AttemptEvaluationService.ScoreDeferred;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService.DimensionInput;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ProviderUnavailableException;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.FakeModelAdapter;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.FunctionModelConfigRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.KafkaEventProducer;
import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** El evaluador de intentos cerrados: `ATTEMPT_CLOSED` entra, `SCORE_CALCULATED` o `SCORE_DEFERRED`
 * sale por el outbox. Incluye un caso de punta a punta contra el adaptador `fake` real. */
class AttemptEvaluationServiceTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final UUID rubricVersionId = UUID.randomUUID();
  private final ClosedAttempt attempt = new ClosedAttempt(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
      mapper.createArrayNode().add(mapper.createObjectNode().put("role", "student").put("content", "no entiendo mi recursión")));

  private final RubricVersionRepository rubrics = mock(RubricVersionRepository.class);
  private final ModelInvocationService models = mock(ModelInvocationService.class);
  private final KafkaEventProducer events = mock(KafkaEventProducer.class);
  private final AttemptEvaluationService service =
      new AttemptEvaluationService(rubrics, models, events, mapper, rubricVersionId, 1000, 30);

  @Test
  void aValidEvaluationPublishesTheScoreKeyedByCohortWithTheWeightedAggregate() {
    when(rubrics.weightsAndPrompts(rubricVersionId)).thenReturn(fullRubric());
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any())).thenReturn(new ModelInvocationResult(
        "{\"autonomy\":80,\"clarity\":60,\"progression\":40,\"compliance\":100,\"efficiency\":20}", "fake", "fake-evaluator-v1"));

    service.evaluate(attempt);

    var payload = ArgumentCaptor.forClass(Object.class);
    verify(events).enqueue(eq(KafkaTopics.EVALUATION_EVENTS), eq(attempt.courseCohortId().toString()),
        eq("SCORE_CALCULATED"), payload.capture());
    var score = (ScoreCalculated) payload.getValue();
    assertThat(score.attemptId()).isEqualTo(attempt.attemptId());
    assertThat(score.learnerId()).isEqualTo(attempt.learnerId());
    assertThat(score.rubricVersionId()).isEqualTo(rubricVersionId);
    assertThat(score.score()).isEqualTo(64);
    assertThat(score.dimensions()).containsEntry("autonomy", 80).containsEntry("efficiency", 20);
    assertThat(score.evaluator().provider()).isEqualTo("fake");
  }

  @Test
  void theTranscriptReachesTheModelUntruncated() {
    when(rubrics.weightsAndPrompts(rubricVersionId)).thenReturn(fullRubric());
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any())).thenReturn(new ModelInvocationResult(
        "{\"autonomy\":1,\"clarity\":1,\"progression\":1,\"compliance\":1,\"efficiency\":1}", "fake", "fake-evaluator-v1"));

    service.evaluate(attempt);

    var userPrompt = ArgumentCaptor.forClass(String.class);
    verify(models).invoke(eq(ModelFunction.EVALUATOR), anyString(), userPrompt.capture(), any());
    assertThat(userPrompt.getValue()).contains("no entiendo mi recursión");
  }

  @Test
  void aModelThatIsDownDefersTheAttemptInsteadOfFailingTheConsumer() {
    when(rubrics.weightsAndPrompts(rubricVersionId)).thenReturn(fullRubric());
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenThrow(new ProviderUnavailableException("groq caído"));

    service.evaluate(attempt);

    assertThat(deferred().reason()).isEqualTo("MODEL_UNAVAILABLE");
  }

  @Test
  void aResponseThatIsNotJsonIsDeferredAsInvalid() {
    when(rubrics.weightsAndPrompts(rubricVersionId)).thenReturn(fullRubric());
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenReturn(new ModelInvocationResult("no soy json", "fake", "fake-evaluator-v1"));

    service.evaluate(attempt);

    assertThat(deferred().reason()).isEqualTo("INVALID_MODEL_RESPONSE");
  }

  @Test
  void aResponseMissingADimensionIsDeferredAsInvalid() {
    when(rubrics.weightsAndPrompts(rubricVersionId)).thenReturn(fullRubric());
    when(models.invoke(eq(ModelFunction.EVALUATOR), anyString(), anyString(), any()))
        .thenReturn(new ModelInvocationResult("{\"autonomy\":80}", "fake", "fake-evaluator-v1"));

    service.evaluate(attempt);

    assertThat(deferred().reason()).isEqualTo("INVALID_MODEL_RESPONSE");
  }

  @Test
  void aRubricWithoutTheFiveDimensionsIsDeferredWithoutCallingTheModel() {
    when(rubrics.weightsAndPrompts(rubricVersionId)).thenReturn(List.of());

    service.evaluate(attempt);

    assertThat(deferred().reason()).isEqualTo("RUBRIC_UNAVAILABLE");
    verify(models, never()).invoke(any(), anyString(), anyString(), any());
  }

  @Test
  void theDeferralCarriesTheAttemptAndWhenToRetry() {
    when(rubrics.weightsAndPrompts(rubricVersionId)).thenReturn(List.of());

    service.evaluate(attempt);

    var deferral = deferred();
    assertThat(deferral.attemptId()).isEqualTo(attempt.attemptId());
    assertThat(deferral.courseCohortId()).isEqualTo(attempt.courseCohortId());
    assertThat(deferral.retryFrom()).isNotBlank();
  }

  @Test
  void theRealFakeAdapterProducesAScoreEndToEndWithoutAnyProvider() {
    var configs = mock(FunctionModelConfigRepository.class);
    var deployments = mock(ModelDeploymentRepository.class);
    var cfg1 = asignado(deployments, "fake", "fake-evaluator-v1", "1", true);
    when(configs.find(ModelFunction.EVALUATOR)).thenReturn(Optional.of(cfg1));
    var withFake = new AttemptEvaluationService(rubrics, new ModelInvocationService(configs, deployments, new FakeModelAdapter()),
        events, mapper, rubricVersionId, 1000, 30);
    when(rubrics.weightsAndPrompts(rubricVersionId)).thenReturn(fullRubric());

    withFake.evaluate(attempt);

    var payload = ArgumentCaptor.forClass(Object.class);
    verify(events).enqueue(eq(KafkaTopics.EVALUATION_EVENTS), eq(attempt.courseCohortId().toString()),
        eq("SCORE_CALCULATED"), payload.capture());
    var score = (ScoreCalculated) payload.getValue();
    assertThat(score.score()).isBetween(55, 95);
    assertThat(score.evaluator().model()).isEqualTo("fake-evaluator-v1");
  }

  private ScoreDeferred deferred() {
    var payload = ArgumentCaptor.forClass(Object.class);
    verify(events).enqueue(eq(KafkaTopics.EVALUATION_EVENTS), eq(attempt.courseCohortId().toString()),
        eq("SCORE_DEFERRED"), payload.capture());
    return (ScoreDeferred) payload.getValue();
  }

  private static List<DimensionInput> fullRubric() {
    return List.of(
        new DimensionInput(Dimension.AUTONOMY, "Autonomía", "razona por sí mismo", null, BigDecimal.valueOf(30)),
        new DimensionInput(Dimension.CLARITY, "Claridad", "consulta con precisión", null, BigDecimal.valueOf(25)),
        new DimensionInput(Dimension.PROGRESSION, "Progresión", "avanza entre mensajes", null, BigDecimal.valueOf(20)),
        new DimensionInput(Dimension.COMPLIANCE, "Cumplimiento", "respeta los límites", null, BigDecimal.valueOf(15)),
        new DimensionInput(Dimension.EFFICIENCY, "Eficiencia", "no repite consultas", null, BigDecimal.valueOf(10)));
  }

  /**
   * Desde la V26 la asignación función→modelo guarda el id del despliegue; proveedor y modelo se
   * leen de {@code ModelDeploymentRepository}. Registra el despliegue en el mock y devuelve el
   * Config correspondiente.
   */
  private static FunctionModelConfigRepository.Config asignado(ModelDeploymentRepository deployments,
      String provider, String modelId, String modelVersion, boolean enabled) {
    UUID deploymentId = UUID.randomUUID();
    // doReturn/when: este helper se invoca dentro de otro when(...), y Mockito no admite when() anidado.
    doReturn(Optional.of(new ModelDeploymentSummary(deploymentId, provider, modelId, modelVersion, "ENABLED")))
        .when(deployments).byId(deploymentId);
    return new FunctionModelConfigRepository.Config(deploymentId, enabled);
  }
}
