package ar.edu.utn.frc.tup.piv.llm.application.service;
import ar.edu.utn.frc.tup.piv.llm.application.CalibrationInferencePolicy;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.ProviderInvocationGateway;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.ai.ProviderRegistry;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ProviderCredentialRepository;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayExecutor;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayUsageLog;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.InferenceSettings;
import ar.edu.utn.frc.tup.piv.llm.provider.spi.ProviderReply;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Duration;
import org.springframework.stereotype.Service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.EvaluatorSkillRepository.EvaluatorSkill;

/** Executes a claimed run against the provider through the sole internal AI gateway. */
@Service
public class RealCalibrationExecutor {
  private final CalibrationRunRepository runs; private final ProviderCredentialRepository usage;
  private final ProviderInvocationGateway gateway; private final ProviderRegistry registry; private final ObjectMapper json; private final CalibrationInferencePolicy policy;
  private final EvaluatorSkillRepository skillRepository;
  private final GatewayExecutor executor;

  @org.springframework.beans.factory.annotation.Autowired
  public RealCalibrationExecutor(CalibrationRunRepository runs, ProviderCredentialRepository usage, ProviderInvocationGateway gateway, ProviderRegistry registry, ObjectMapper json, CalibrationInferencePolicy policy, EvaluatorSkillRepository skillRepository, GatewayExecutor executor) {
    this.runs=runs;this.usage=usage;this.gateway=gateway;this.registry=registry;this.json=json;this.policy=policy;this.skillRepository=skillRepository;this.executor=executor;
  }

  public RealCalibrationExecutor(CalibrationRunRepository runs, ProviderCredentialRepository usage, ProviderInvocationGateway gateway, ProviderRegistry registry, ObjectMapper json, CalibrationInferencePolicy policy, EvaluatorSkillRepository skillRepository) {
    this(runs, usage, gateway, registry, json, policy, skillRepository, GatewayExecutor.disabled());
  }

  public RealCalibrationExecutor(CalibrationRunRepository runs, ProviderCredentialRepository usage, ProviderInvocationGateway gateway, ProviderRegistry registry, ObjectMapper json, CalibrationInferencePolicy policy) {
    this(runs, usage, gateway, registry, json, policy, null);
  }

  /** Cada caso de calibración pasa por el GatewayExecutor: reintentos, breaker por proveedor, presupuesto y registro de uso. */
  private ProviderReply viaGateway(ProviderCredentialRepository.Credential credential, String modelId, String prompt, InferenceSettings settings) {
    try {
      return executor.run(new GatewayExecutor.Spec<>(ModelFunction.EVALUATOR, credential.providerKey(), modelId,
        GatewayUsageLog.estimateTokens(prompt), Duration.ofSeconds(90),
        () -> gateway.invoke(credential, modelId, prompt, settings, Duration.ofSeconds(90)),
        reply -> new int[] {reply.inputTokens(), reply.outputTokens()}, reply -> { },
        "El proveedor no respondió a tiempo", "El proveedor no pudo evaluar el caso",
          IllegalStateException::new, true));
    } catch (IllegalStateException failure) {
      // El diagnóstico de la corrida se arma con el mensaje del error del proveedor, no con el envoltorio del gateway.
      if (failure.getCause() instanceof RuntimeException original) throw original;
      throw failure;
    }
  }

  public void execute(UUID runId) {
    CalibrationRunRepository.Execution execution = null;
    try {
      execution=runs.execution(runId); int total=execution.cases().size(); int completed=0;
      var credential=usage.get(execution.deployment().credentialId()).filter(value->"ACTIVE".equals(value.state())).orElseThrow(()->new IllegalStateException("La credencial del deployment no estÃ¡ activa"));
      var settings=policy.resolve(registry.required(execution.deployment().providerKey()).descriptor().capabilities(), execution.seed() == null ? 0L : execution.seed());
      String fingerprint=null;

      boolean passed;
      if ("DEFAULT_INSTITUTIONAL".equalsIgnoreCase(execution.rubricKind()) || execution.rubricKind() == null) {
        List<CalibrationMetrics.CaseScores> all=new ArrayList<>();
        for(var item:execution.cases()) {
          var reply=viaGateway(credential,execution.deployment().modelId(),prompt(execution.rubric(),item),settings.settings());
          Map<Dimension,Integer> model=parseScores(reply.text());
          if (reply.providerFingerprint()!=null) fingerprint=reply.providerFingerprint();
          runs.saveCase(runId,item,model,execution.weights()); usage.recordUsage(execution.deployment().id(),"EVALUATION",reply.inputTokens(),reply.outputTokens());
          all.add(new CalibrationMetrics.CaseScores(item.humanScores(),model)); runs.progress(runId,++completed*100/total);
        }
        var metrics=CalibrationMetrics.assess(all,execution.weights()); runs.recordInference(runId, json.writeValueAsString(settings.auditView()), fingerprint); runs.finish(runId,metrics.passed(),metrics.maeFinal(),metrics.maxIndividualError()); runs.refreshStability(runId);
        passed = metrics.passed();
      } else {
        var activeSkills = skillRepository != null && execution.courseId() != null
            ? skillRepository.findActiveByCourse(execution.courseId())
            : List.<EvaluatorSkill>of();
        List<Map<String, Integer>> allHuman = new ArrayList<>();
        List<Map<String, Integer>> allModel = new ArrayList<>();
        for (var item : execution.cases()) {
          String prompt = promptModular(execution.rubric(), item, activeSkills, execution.userPrompt());
          var reply = viaGateway(credential, execution.deployment().modelId(), prompt, settings.settings());
          Map<String, Integer> dynamicScores = parseScoresModular(reply.text(), execution.dimensionKeys());
          if (reply.providerFingerprint() != null) fingerprint = reply.providerFingerprint();
          runs.saveCaseModular(runId, item, dynamicScores, execution.dynamicWeights());
          usage.recordUsage(execution.deployment().id(), "EVALUATION", reply.inputTokens(), reply.outputTokens());
          allHuman.add(item.dynamicHumanScores());
          allModel.add(dynamicScores);
          runs.progress(runId, ++completed * 100 / total);
        }
        var metrics = CalibrationMetrics.assessDynamic(allHuman, allModel, execution.dynamicWeights());
        runs.recordInference(runId, json.writeValueAsString(settings.auditView()), fingerprint);
        runs.finish(runId, metrics.passed(), metrics.maeFinal(), metrics.maxIndividualError());
        runs.refreshStability(runId);
        passed = metrics.passed();
      }
      settlePlatformOutcome(execution, passed);
    } catch (Exception failure) { 
      System.out.println("CALIBRATION FAILED EXCEPTION:");
      failure.printStackTrace();
      var diagnostic=diagnostic(failure); 
      runs.fail(runId,diagnostic.code(),diagnostic.detail()); 
      runs.refreshStability(runId);
      if (execution != null && "PLATFORM".equals(execution.run().stage())) usage.excludeFromCalibrationTarget(execution.deployment().id());
    }
  }
  /** T-658: una corrida institucional habilita (o vuelve a dejar en candidato) el deployment. */
  private void settlePlatformOutcome(CalibrationRunRepository.Execution execution, boolean passed) {
    if ("PLATFORM".equals(execution.run().stage())) {
      if (passed) usage.activate(execution.deployment().id());
      else usage.excludeFromCalibrationTarget(execution.deployment().id());
    }
  }
  private String prompt(String rubric, CalibrationRunRepository.Case item) {
    return "ActuÃ¡s como evaluador pedagÃ³gico. EvaluÃ¡ la conversaciÃ³n y el contexto con esta rÃºbrica:\n%s\nConversaciÃ³n: %s\nContexto: %s\nRespondÃ© exclusivamente JSON, sin Markdown, con las cinco claves AUTONOMY, CLARITY, PROGRESSION, COMPLIANCE y EFFICIENCY. Cada valor debe ser un entero de 0 a 100.".formatted(rubric,item.transcript(),item.challengeContext());
  }

  String promptModular(String rubric, CalibrationRunRepository.Case item, List<EvaluatorSkill> skills, String userPrompt) {
    StringBuilder sb = new StringBuilder();
    sb.append("ActuÃ¡s como evaluador pedagÃ³gico. EvaluÃ¡ la conversaciÃ³n con esta rÃºbrica personalizada:\n").append(rubric).append("\n");
    if (userPrompt != null && !userPrompt.isBlank()) {
      sb.append("\nINSTRUCCIONES DOCENTE:\n").append(userPrompt).append("\n");
    }
    if (skills != null && !skills.isEmpty()) {
      sb.append("\nHABILIDADES TÃ‰CNICAS ACTIVADAS PARA EL ANÃLISIS:\n");
      for (var s : skills) {
        sb.append("- [").append(s.name()).append("]: ").append(s.systemInstruction()).append("\n");
      }
    }
    sb.append("\nConversaciÃ³n: ").append(item.transcript());
    sb.append("\nContexto: ").append(item.challengeContext());
    sb.append("\nRespondÃ© exclusivamente JSON, sin Markdown, con las claves de las dimensiones evaluadas. Cada valor entero de 0 a 100.");
    return sb.toString();
  }

  private Map<Dimension,Integer> parseScores(String value) {
    System.out.println("LLM RESPONSE WAS: " + value);
    try {
      String clean = value;
      int start = clean.indexOf("{");
      int end = clean.lastIndexOf("}");
      if (start != -1 && end != -1 && end >= start) clean = clean.substring(start, end + 1);
      JsonNode root=json.readTree(clean); Map<Dimension,Integer> scores=new EnumMap<>(Dimension.class);
      for(var dimension:Dimension.values()) { 
          JsonNode score=root.get(dimension.name()); 
          if (score == null) score = root.get(dimension.name().toLowerCase());
          if(score==null||!score.isIntegralNumber()||score.intValue()<0||score.intValue()>100) throw new IllegalArgumentException("Puntaje de proveedor invÃ¡lido"); 
          scores.put(dimension,score.intValue()); 
      }
      return Map.copyOf(scores);
    } catch (Exception error) { throw new IllegalArgumentException("La respuesta del proveedor no tiene el formato de evaluaciÃ³n requerido",error); }
  }

  Map<String, Integer> parseScoresModular(String value, List<String> dimensionKeys) {
    try {
      String clean = stripMarkdown(value);
      JsonNode root = json.readTree(clean);
      Map<String, Integer> scores = new java.util.LinkedHashMap<>();
      for (String key : dimensionKeys) {
        JsonNode score = root.get(key);
        if (score == null || !score.isIntegralNumber() || score.intValue() < 0 || score.intValue() > 100) {
          throw new IllegalArgumentException("Puntaje de proveedor invÃ¡lido");
        }
        scores.put(key, score.intValue());
      }
      return Map.copyOf(scores);
    } catch (Exception error) {
      throw new IllegalArgumentException("La respuesta del proveedor no tiene el formato de evaluaciÃ³n requerido", error);
    }
  }

  private String stripMarkdown(String text) {
    if (text == null) return "";
    String trimmed = text.trim();
    if (trimmed.startsWith("```json")) {
      trimmed = trimmed.substring(7);
    } else if (trimmed.startsWith("```")) {
      trimmed = trimmed.substring(3);
    }
    if (trimmed.endsWith("```")) {
      trimmed = trimmed.substring(0, trimmed.length() - 3);
    }
    return trimmed.trim();
  }

  private Diagnostic diagnostic(Exception failure) {
    String message = failure.getMessage() == null ? "" : failure.getMessage();
    if (message.contains("formato de evaluaciÃ³n")) return new Diagnostic("MALFORMED_MODEL_RESPONSE", "El modelo respondiÃ³, pero no devolviÃ³ los cinco puntajes enteros requeridos.");
    if (message.contains("HTTP 429")) return new Diagnostic("PROVIDER_RATE_LIMIT", "El proveedor limitÃ³ las solicitudes. EsperÃ¡ un momento y reintentÃ¡.");
    if (message.contains("HTTP 5")) return new Diagnostic("PROVIDER_UNAVAILABLE", "El proveedor tuvo un error temporal. ReintentÃ¡ mÃ¡s tarde.");
    if (message.contains("No se pudo conectar")) return new Diagnostic("PROVIDER_CONNECTION_FAILED", "No se pudo conectar con el proveedor. VerificÃ¡ la credencial, el modelo y la conectividad.");
    return new Diagnostic("CALIBRATION_EXECUTION_FAILED", "La calibraciÃ³n no pudo completarse. RevisÃ¡ la configuraciÃ³n del modelo e intentÃ¡ nuevamente.");
  }
  private record Diagnostic(String code, String detail) {}
}
