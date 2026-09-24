package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.ProviderInvocationGateway;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.ProviderRegistry;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.application.CalibrationInferencePolicy;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.AiProviderAdapter;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.InferenceSettings;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderCapabilities;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderDescriptor;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderReply;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealCalibrationExecutorTest {
  private final ObjectMapper json = new ObjectMapper();

  private Map<Dimension, Integer> uniformWeights() {
    var weights = new EnumMap<Dimension, Integer>(Dimension.class);
    for (var dimension : Dimension.values()) weights.put(dimension, 20);
    return Map.copyOf(weights);
  }

  private Map<Dimension, Integer> uniformScores(int value) {
    var scores = new EnumMap<Dimension, Integer>(Dimension.class);
    for (var dimension : Dimension.values()) scores.put(dimension, value);
    return Map.copyOf(scores);
  }

  private CalibrationRunRepository.Case aCase() {
    return new CalibrationRunRepository.Case(UUID.randomUUID(),
        json.createObjectNode().put("role", "STUDENT"),
        json.createObjectNode().put("topic", "colas"), uniformScores(85));
  }

  private String scoresJson(int value) {
    return "{\"AUTONOMY\":" + value + ",\"CLARITY\":" + value + ",\"PROGRESSION\":" + value
        + ",\"COMPLIANCE\":" + value + ",\"EFFICIENCY\":" + value + "}";
  }

  private CalibrationRunRepository.Execution execution(UUID runId, Long seed) {
    return new CalibrationRunRepository.Execution(
        new CalibrationRunRepository.Run(runId, "RUNNING", 0),
        new CalibrationRunRepository.Deployment(UUID.randomUUID(), UUID.randomUUID(),
            "openai-compatible", "gpt-4o-mini"),
        uniformWeights(), "Rubrica de prueba",
        List.of(aCase(), aCase()), seed);
  }

  private AiProviderAdapter adapter() {
    var adapter = mock(AiProviderAdapter.class);
    when(adapter.descriptor()).thenReturn(new ProviderDescriptor("openai-compatible", "OpenAI",
        "1", List.of(), new ProviderCapabilities(false, false, true, true, true, true, false, true)));
    return adapter;
  }

  private ProviderCredentialRepository.Credential activeCredential(UUID id) {
    return new ProviderCredentialRepository.Credential(id, "openai-compatible", "OpenAI",
        Map.of(), new byte[0], new byte[0], "sk-***", "ACTIVE", Instant.now());
  }

  @Test void executesCasesAndRecordsMetricsAndArtifacts() {
    var runs = mock(CalibrationRunRepository.class);
    var usage = mock(ProviderCredentialRepository.class);
    var gateway = mock(ProviderInvocationGateway.class);
    var registry = mock(ProviderRegistry.class);
    UUID runId = UUID.randomUUID();
    var execution = execution(runId, 42L);
    when(runs.execution(runId)).thenReturn(execution);
    var credential = activeCredential(execution.deployment().credentialId());
    when(usage.get(execution.deployment().credentialId())).thenReturn(Optional.of(credential));
    var aiAdapter = adapter(); when(registry.required("openai-compatible")).thenReturn(aiAdapter);
    when(gateway.invoke(any(), anyString(), anyString(), any(InferenceSettings.class), any(Duration.class)))
        .thenReturn(new ProviderReply(scoresJson(85), 100, 50, "fp-xyz"));

    new RealCalibrationExecutor(runs, usage, gateway, registry, json, new CalibrationInferencePolicy())
        .execute(runId);

    verify(runs, times(2)).saveCase(eq(runId), any(), any(), any());
    verify(usage, times(2)).recordUsage(execution.deployment().id(), "EVALUATION", 100, 50);
    verify(runs, times(2)).progress(eq(runId), any(Integer.class));
    verify(runs).recordInference(eq(runId), anyString(), eq("fp-xyz"));
    verify(runs).finish(eq(runId), eq(true), any(), anyInt());
    verify(runs).refreshStability(runId);
  }

  @Test void parsesLowercaseDimensionsInsideMarkdownWrappers() {
    var runs = mock(CalibrationRunRepository.class);
    var usage = mock(ProviderCredentialRepository.class);
    var gateway = mock(ProviderInvocationGateway.class);
    var registry = mock(ProviderRegistry.class);
    UUID runId = UUID.randomUUID();
    var execution = execution(runId, null);
    when(runs.execution(runId)).thenReturn(execution);
    var credential = activeCredential(execution.deployment().credentialId());
    when(usage.get(execution.deployment().credentialId())).thenReturn(Optional.of(credential));
    var aiAdapter = adapter(); when(registry.required("openai-compatible")).thenReturn(aiAdapter);
    String wrapped = "```json\n{\"autonomy\":85,\"clarity\":85,\"progression\":85,\"compliance\":85,\"efficiency\":85}\n```";
    when(gateway.invoke(any(), anyString(), anyString(), any(InferenceSettings.class), any(Duration.class)))
        .thenReturn(new ProviderReply(wrapped, 5, 2, null));

    new RealCalibrationExecutor(runs, usage, gateway, registry, json, new CalibrationInferencePolicy())
        .execute(runId);

    verify(runs).recordInference(eq(runId), anyString(), isNull());
    verify(runs).finish(eq(runId), eq(true), any(), anyInt());
  }

  @Test void mapsAMalformedModelResponseToMalformedDiagnostic() {
    var runs = mock(CalibrationRunRepository.class);
    var usage = mock(ProviderCredentialRepository.class);
    var gateway = mock(ProviderInvocationGateway.class);
    var registry = mock(ProviderRegistry.class);
    UUID runId = UUID.randomUUID();
    var execution = execution(runId, 42L);
    when(runs.execution(runId)).thenReturn(execution);
    var credential = activeCredential(execution.deployment().credentialId());
    when(usage.get(execution.deployment().credentialId())).thenReturn(Optional.of(credential));
    var aiAdapter = adapter(); when(registry.required("openai-compatible")).thenReturn(aiAdapter);
    when(gateway.invoke(any(), anyString(), anyString(), any(InferenceSettings.class), any(Duration.class)))
        .thenReturn(new ProviderReply("no es json", 0, 0, null));

    new RealCalibrationExecutor(runs, usage, gateway, registry, json, new CalibrationInferencePolicy())
        .execute(runId);

    verify(runs).fail(eq(runId), eq("MALFORMED_MODEL_RESPONSE"), anyString());
    verify(runs).refreshStability(runId);
  }

  @Test void rejectsAnOutOfRangeProviderScore() {
    var runs = mock(CalibrationRunRepository.class);
    var usage = mock(ProviderCredentialRepository.class);
    var gateway = mock(ProviderInvocationGateway.class);
    var registry = mock(ProviderRegistry.class);
    UUID runId = UUID.randomUUID();
    var execution = execution(runId, 1L);
    when(runs.execution(runId)).thenReturn(execution);
    var credential = activeCredential(execution.deployment().credentialId());
    when(usage.get(execution.deployment().credentialId())).thenReturn(Optional.of(credential));
    var aiAdapter = adapter(); when(registry.required("openai-compatible")).thenReturn(aiAdapter);
    String outOfRange = "{\"AUTONOMY\":85,\"CLARITY\":85,\"PROGRESSION\":85,\"COMPLIANCE\":999,\"EFFICIENCY\":85}";
    when(gateway.invoke(any(), anyString(), anyString(), any(InferenceSettings.class), any(Duration.class)))
        .thenReturn(new ProviderReply(outOfRange, 0, 0, null));

    new RealCalibrationExecutor(runs, usage, gateway, registry, json, new CalibrationInferencePolicy())
        .execute(runId);

    verify(runs).fail(eq(runId), eq("MALFORMED_MODEL_RESPONSE"), anyString());
  }

  @Test void mapsProviderRateLimitToItsDiagnostic() {
    var runs = mock(CalibrationRunRepository.class);
    var usage = mock(ProviderCredentialRepository.class);
    var gateway = mock(ProviderInvocationGateway.class);
    var registry = mock(ProviderRegistry.class);
    UUID runId = UUID.randomUUID();
    var execution = execution(runId, 1L);
    when(runs.execution(runId)).thenReturn(execution);
    var credential = activeCredential(execution.deployment().credentialId());
    when(usage.get(execution.deployment().credentialId())).thenReturn(Optional.of(credential));
    var aiAdapter = adapter(); when(registry.required("openai-compatible")).thenReturn(aiAdapter);
    when(gateway.invoke(any(), anyString(), anyString(), any(InferenceSettings.class), any(Duration.class)))
        .thenThrow(new IllegalStateException("HTTP 429 Too Many Requests"));

    new RealCalibrationExecutor(runs, usage, gateway, registry, json, new CalibrationInferencePolicy())
        .execute(runId);

    verify(runs).fail(eq(runId), eq("PROVIDER_RATE_LIMIT"), anyString());
  }

  @Test void mapsProviderHttpErrorToUnavailableDiagnostic() {
    var gateway = mock(ProviderInvocationGateway.class);
    var runs = mock(CalibrationRunRepository.class);
    var usage = mock(ProviderCredentialRepository.class);
    var registry = mock(ProviderRegistry.class);
    UUID runId = UUID.randomUUID();
    var execution = execution(runId, 1L);
    when(runs.execution(runId)).thenReturn(execution);
    var credential = activeCredential(execution.deployment().credentialId());
    when(usage.get(execution.deployment().credentialId())).thenReturn(Optional.of(credential));
    var aiAdapter = adapter(); when(registry.required("openai-compatible")).thenReturn(aiAdapter);
    when(gateway.invoke(any(), anyString(), anyString(), any(InferenceSettings.class), any(Duration.class)))
        .thenThrow(new IllegalStateException("HTTP 503 Service Unavailable"));

    new RealCalibrationExecutor(runs, usage, gateway, registry, json, new CalibrationInferencePolicy())
        .execute(runId);

    verify(runs).fail(eq(runId), eq("PROVIDER_UNAVAILABLE"), anyString());
  }

  @Test void mapsConnectionFailureToItsDiagnostic() {
    var gateway = mock(ProviderInvocationGateway.class);
    var runs = mock(CalibrationRunRepository.class);
    var usage = mock(ProviderCredentialRepository.class);
    var registry = mock(ProviderRegistry.class);
    UUID runId = UUID.randomUUID();
    var execution = execution(runId, 1L);
    when(runs.execution(runId)).thenReturn(execution);
    var credential = activeCredential(execution.deployment().credentialId());
    when(usage.get(execution.deployment().credentialId())).thenReturn(Optional.of(credential));
    var aiAdapter = adapter(); when(registry.required("openai-compatible")).thenReturn(aiAdapter);
    when(gateway.invoke(any(), anyString(), anyString(), any(InferenceSettings.class), any(Duration.class)))
        .thenThrow(new IllegalStateException("No se pudo conectar con el proveedor"));

    new RealCalibrationExecutor(runs, usage, gateway, registry, json, new CalibrationInferencePolicy())
        .execute(runId);

    verify(runs).fail(eq(runId), eq("PROVIDER_CONNECTION_FAILED"), anyString());
  }

  @Test void fallsBackToTheGenericDiagnosticForUnknownFailures() {
    var gateway = mock(ProviderInvocationGateway.class);
    var runs = mock(CalibrationRunRepository.class);
    var usage = mock(ProviderCredentialRepository.class);
    var registry = mock(ProviderRegistry.class);
    UUID runId = UUID.randomUUID();
    var execution = execution(runId, 1L);
    when(runs.execution(runId)).thenReturn(execution);
    var credential = activeCredential(execution.deployment().credentialId());
    when(usage.get(execution.deployment().credentialId())).thenReturn(Optional.of(credential));
    var aiAdapter = adapter(); when(registry.required("openai-compatible")).thenReturn(aiAdapter);
    when(gateway.invoke(any(), anyString(), anyString(), any(InferenceSettings.class), any(Duration.class)))
        .thenThrow(new IllegalStateException("boom"));

    new RealCalibrationExecutor(runs, usage, gateway, registry, json, new CalibrationInferencePolicy())
        .execute(runId);

    verify(runs).fail(eq(runId), eq("CALIBRATION_EXECUTION_FAILED"), anyString());
  }

  @Test void failsWhenTheDeploymentCredentialIsNotActive() {
    var gateway = mock(ProviderInvocationGateway.class);
    var runs = mock(CalibrationRunRepository.class);
    var usage = mock(ProviderCredentialRepository.class);
    var registry = mock(ProviderRegistry.class);
    UUID runId = UUID.randomUUID();
    var execution = execution(runId, 1L);
    when(runs.execution(runId)).thenReturn(execution);
    when(usage.get(execution.deployment().credentialId())).thenReturn(Optional.empty());

    new RealCalibrationExecutor(runs, usage, gateway, registry, json, new CalibrationInferencePolicy())
        .execute(runId);

    verify(runs).fail(eq(runId), eq("CALIBRATION_EXECUTION_FAILED"), anyString());
    verify(runs).refreshStability(runId);
  }

  @Test void failsWhenAnEmptyGoldenSetIsProvided() {
    var gateway = mock(ProviderInvocationGateway.class);
    var runs = mock(CalibrationRunRepository.class);
    var usage = mock(ProviderCredentialRepository.class);
    var registry = mock(ProviderRegistry.class);
    UUID runId = UUID.randomUUID();
    var execution = new CalibrationRunRepository.Execution(
        new CalibrationRunRepository.Run(runId, "RUNNING", 0),
        new CalibrationRunRepository.Deployment(UUID.randomUUID(), UUID.randomUUID(),
            "openai-compatible", "gpt-4o-mini"),
        uniformWeights(), "Rubrica", List.of(), 1L);
    when(runs.execution(runId)).thenReturn(execution);
    var credential = activeCredential(execution.deployment().credentialId());
    when(usage.get(execution.deployment().credentialId())).thenReturn(Optional.of(credential));

    new RealCalibrationExecutor(runs, usage, gateway, registry, json, new CalibrationInferencePolicy())
        .execute(runId);

    verify(runs).fail(eq(runId), eq("CALIBRATION_EXECUTION_FAILED"), anyString());
  }

  @Test
  void execute_modularCustomFlow_injectsActiveSkillsAndUserPromptAndParsesDynamicScores() {
    var runs = mock(CalibrationRunRepository.class);
    var usage = mock(ProviderCredentialRepository.class);
    var gateway = mock(ProviderInvocationGateway.class);
    var registry = mock(ProviderRegistry.class);
    var skillRepository = mock(ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository.class);
    UUID runId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    UUID deploymentId = UUID.randomUUID();
    UUID credentialId = UUID.randomUUID();
    UUID caseId = UUID.randomUUID();
    var executor = new RealCalibrationExecutor(runs, usage, gateway, registry, json, new CalibrationInferencePolicy(), skillRepository);

    CalibrationRunRepository.Run run = new CalibrationRunRepository.Run(runId, courseId, "COURSE", "RUNNING", 0, UUID.randomUUID(), UUID.randomUUID(), deploymentId, null, null, "MANUAL", java.time.Instant.now(), null, null, null, null);
    CalibrationRunRepository.Deployment deployment = new CalibrationRunRepository.Deployment(deploymentId, credentialId, "openai-compatible", "gpt-4o-mini");

    var aiAdapter = adapter(); when(registry.required("openai-compatible")).thenReturn(aiAdapter);
    var credential = activeCredential(credentialId);
    when(usage.get(credentialId)).thenReturn(Optional.of(credential));

    com.fasterxml.jackson.databind.node.ArrayNode transcript = json.createArrayNode();
    com.fasterxml.jackson.databind.node.ObjectNode context = json.createObjectNode();
    Map<String, Integer> dynamicHuman = Map.of("code_quality", 80, "test_runner", 90);
    CalibrationRunRepository.Case c = new CalibrationRunRepository.Case(caseId, transcript, context, Map.of(), dynamicHuman);

    List<String> dimensionKeys = List.of("code_quality", "test_runner");
    Map<String, Integer> dynamicWeights = Map.of("code_quality", 60, "test_runner", 40);

    CalibrationRunRepository.Execution execution = new CalibrationRunRepository.Execution(
        run, deployment, Map.of(), "Rúbrica modular", List.of(c), 0L,
        "MODULAR_CUSTOM", "Enfocarse en buenas prácticas de clean code",
        dimensionKeys, dynamicWeights
    );
    when(runs.execution(runId)).thenReturn(execution);

    ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository.EvaluatorSkill activeSkill = new ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository.EvaluatorSkill(
        "code_quality", "Revisor de Calidad", "Inspecciona código", "STATIC_ANALYSIS",
        "Evaluar legibilidad y modularidad estricta.", true
    );
    when(skillRepository.findActiveByCourse(courseId)).thenReturn(List.of(activeSkill));

    when(gateway.invoke(any(), eq("gpt-4o-mini"), anyString(), any(), any()))
        .thenReturn(new ProviderReply("```json\n{\"code_quality\": 85, \"test_runner\": 90}\n```", 120, 60, "fp-2"));

    executor.execute(runId);

    verify(skillRepository).findActiveByCourse(courseId);

    org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(gateway).invoke(any(), eq("gpt-4o-mini"), promptCaptor.capture(), any(), any());
    String sentPrompt = promptCaptor.getValue();
    assertThat(sentPrompt).contains("INSTRUCCIONES DOCENTE:\nEnfocarse en buenas prácticas de clean code");
    assertThat(sentPrompt).contains("HABILIDADES TÉCNICAS ACTIVADAS PARA EL ANÁLISIS:");
    assertThat(sentPrompt).contains("[Revisor de Calidad]: Evaluar legibilidad y modularidad estricta.");
    assertThat(sentPrompt).contains("Rúbrica modular");

    verify(runs).saveCaseModular(eq(runId), eq(c), eq(Map.of("code_quality", 85, "test_runner", 90)), eq(dynamicWeights));
    verify(runs).finish(eq(runId), eq(true), any(java.math.BigDecimal.class), anyInt());
    verify(runs).refreshStability(runId);
  }

  @Test
  void parseScoresModular_validatesKeysAndNumberRanges() {
    var executor = new RealCalibrationExecutor(null, null, null, null, json, null, null);
    List<String> keys = List.of("dim1", "dim2");

    Map<String, Integer> scores = executor.parseScoresModular("{\"dim1\": 75, \"dim2\": 100}", keys);
    assertThat(scores).containsEntry("dim1", 75).containsEntry("dim2", 100);

    assertThatThrownBy(() -> executor.parseScoresModular("{\"dim1\": 75}", keys))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> executor.parseScoresModular("{\"dim1\": -1, \"dim2\": 50}", keys))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> executor.parseScoresModular("{\"dim1\": 101, \"dim2\": 50}", keys))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> executor.parseScoresModular("{\"dim1\": \"not-a-number\", \"dim2\": 50}", keys))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
