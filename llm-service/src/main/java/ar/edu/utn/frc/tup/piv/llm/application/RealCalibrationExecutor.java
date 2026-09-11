package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.EncryptedSecretService;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.gateway.ProviderLlmGateway;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ProviderCredentialRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Executes a claimed run against the provider through the sole internal AI gateway. */
@Service
public class RealCalibrationExecutor {
  private final CalibrationRunRepository runs; private final ProviderCredentialRepository usage;
  private final ProviderLlmGateway gateway; private final EncryptedSecretService crypto; private final ObjectMapper json;
  public RealCalibrationExecutor(CalibrationRunRepository runs, ProviderCredentialRepository usage, ProviderLlmGateway gateway, EncryptedSecretService crypto, ObjectMapper json) { this.runs=runs;this.usage=usage;this.gateway=gateway;this.crypto=crypto;this.json=json; }
  public void execute(UUID runId) {
    try {
      var execution=runs.execution(runId); List<CalibrationMetrics.CaseScores> all=new ArrayList<>(); int total=execution.cases().size(); int completed=0;
      String secret=crypto.decrypt(execution.deployment().encryptedSecret(),execution.deployment().nonce());
      for(var item:execution.cases()) {
        var reply=gateway.chat(ProviderLlmGateway.Provider.valueOf(execution.deployment().provider()),execution.deployment().baseUrl(),secret,execution.deployment().modelId(),prompt(execution.rubric(),item));
        Map<Dimension,Integer> model=parseScores(reply.text());
        runs.saveCase(runId,item,model,execution.weights()); usage.recordUsage(execution.deployment().id(),"EVALUATION",reply.inputTokens(),reply.outputTokens());
        all.add(new CalibrationMetrics.CaseScores(item.humanScores(),model)); runs.progress(runId,++completed*100/total);
      }
      var metrics=CalibrationMetrics.assess(all,execution.weights()); runs.finish(runId,metrics.passed(),metrics.maeFinal(),metrics.maxIndividualError());
    } catch (Exception failure) { var diagnostic=diagnostic(failure); runs.fail(runId,diagnostic.code(),diagnostic.detail()); }
  }
  private String prompt(String rubric, CalibrationRunRepository.Case item) {
    return "Actuás como evaluador pedagógico. Evaluá la conversación y el contexto con esta rúbrica:\n%s\nConversación: %s\nContexto: %s\nRespondé exclusivamente JSON, sin Markdown, con las cinco claves AUTONOMY, CLARITY, PROGRESSION, COMPLIANCE y EFFICIENCY. Cada valor debe ser un entero de 0 a 100.".formatted(rubric,item.transcript(),item.challengeContext());
  }
  private Map<Dimension,Integer> parseScores(String value) {
    try {
      JsonNode root=json.readTree(value); Map<Dimension,Integer> scores=new EnumMap<>(Dimension.class);
      for(var dimension:Dimension.values()) { JsonNode score=root.get(dimension.name()); if(score==null||!score.isIntegralNumber()||score.intValue()<0||score.intValue()>100) throw new IllegalArgumentException("Puntaje de proveedor inválido"); scores.put(dimension,score.intValue()); }
      if(root.size()!=Dimension.values().length) throw new IllegalArgumentException("Respuesta estructurada inválida"); return Map.copyOf(scores);
    } catch (Exception error) { throw new IllegalArgumentException("La respuesta del proveedor no tiene el formato de evaluación requerido",error); }
  }
  private Diagnostic diagnostic(Exception failure) {
    String message = failure.getMessage() == null ? "" : failure.getMessage();
    if (message.contains("formato de evaluación")) return new Diagnostic("MALFORMED_MODEL_RESPONSE", "El modelo respondió, pero no devolvió los cinco puntajes enteros requeridos.");
    if (message.contains("HTTP 429")) return new Diagnostic("PROVIDER_RATE_LIMIT", "El proveedor limitó las solicitudes. Esperá un momento y reintentá.");
    if (message.contains("HTTP 5")) return new Diagnostic("PROVIDER_UNAVAILABLE", "El proveedor tuvo un error temporal. Reintentá más tarde.");
    if (message.contains("No se pudo conectar")) return new Diagnostic("PROVIDER_CONNECTION_FAILED", "No se pudo conectar con el proveedor. Verificá la credencial, el modelo y la conectividad.");
    return new Diagnostic("CALIBRATION_EXECUTION_FAILED", "La calibración no pudo completarse. Revisá la configuración del modelo e intentá nuevamente.");
  }
  private record Diagnostic(String code, String detail) {}
}
