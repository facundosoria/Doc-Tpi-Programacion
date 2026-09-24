package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayExecutor;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayUsageLog;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingPort;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingTimeoutException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.InvalidEmbeddingException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.FunctionModelConfigRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ModelDeploymentRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelDeploymentSummary;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Service;

/** Paralelo a {@link ModelInvocationService} pero para el puerto de vectorización (EP-09): resuelve
 * la función `embedding` contra {@link FunctionModelConfigRepository}, invoca el
 * {@link EmbeddingPort} con timeout, y valida la dimensión del vector. No reutiliza
 * `ModelInvocationService` porque ese servicio está atado a texto de salida
 * ({@code ModelInvocationResult}), no a un vector — forzar el embedding ahí ensuciaría ese
 * contrato (ver plan de EP-09, §4). */
@Service
public class EmbeddingInvocationService {
  private static final int EXPECTED_DIMENSIONS = 768;

  private final FunctionModelConfigRepository configs;
  private final ModelDeploymentRepository deployments;
  private final EmbeddingPort adapter;
  private final GatewayExecutor executor;

  @org.springframework.beans.factory.annotation.Autowired
  public EmbeddingInvocationService(FunctionModelConfigRepository configs,
      ModelDeploymentRepository deployments, EmbeddingPort adapter, GatewayExecutor executor) {
    this.configs = configs;
    this.deployments = deployments;
    this.adapter = adapter;
    this.executor = executor;
  }

  /** Sin reintentos, breaker ni presupuesto: comportamiento previo a EP-02·H03. */
  public EmbeddingInvocationService(FunctionModelConfigRepository configs,
      ModelDeploymentRepository deployments, EmbeddingPort adapter) {
    this(configs, deployments, adapter, GatewayExecutor.disabled());
  }

  /** Vectoriza un único texto donde el resultado es indispensable (ej. la pregunta del alumno
   * antes de buscar contexto) — nunca devuelve un vector nulo o de otra dimensión. */
  public EmbeddingResult embed(String text, Duration timeout) {
    var config = requireEnabled();
    return run(config, GatewayUsageLog.estimateTokens(text), timeout, () -> adapter.embed(text), this::validate);
  }

  /** Vectoriza un lote (ej. los chunks de un PDF recién indexado). A diferencia de {@link #embed},
   * tolera vectores individuales nulos dentro del lote — mismo criterio que
   * `EmbeddingService.computeEmbeddings` en la demo: un fragmento sin vector no aborta la
   * indexación completa, solo queda sin buscar semánticamente ese fragmento puntual. */
  public List<EmbeddingResult> embedBatch(List<String> texts, Duration timeout) {
    if (texts == null || texts.isEmpty()) return List.of();
    var config = requireEnabled();
    int tokens = texts.stream().mapToInt(GatewayUsageLog::estimateTokens).sum();
    return run(config, tokens, timeout, () -> adapter.embedBatch(texts), batch -> { });
  }

  /** Desde la V26 la asignación guarda el despliegue; proveedor y modelo salen de ahí. */
  private ModelDeploymentSummary requireEnabled() {
    var config = configs.find(ModelFunction.EMBEDDING)
        .orElseThrow(() -> new IllegalStateException("La función embedding no tiene modelo asignado"));
    if (!config.enabled()) {
      throw new IllegalStateException("La función embedding está deshabilitada");
    }
    return deployments.byId(config.modelDeploymentId())
        .orElseThrow(() -> new IllegalStateException("El despliegue asignado a embedding ya no existe"));
  }

  private <T> T run(ModelDeploymentSummary config, int tokens, Duration timeout,
      java.util.function.Supplier<T> call, java.util.function.Consumer<T> validator) {
    return executor.run(new GatewayExecutor.Spec<>(ModelFunction.EMBEDDING, config.provider(), config.modelId(),
        tokens, timeout, call, null, validator,
        "El adaptador de embeddings superó el timeout de " + timeout.toMillis() + "ms",
        "Fallo al invocar el adaptador de embeddings", EmbeddingTimeoutException::new, true));
  }

  private void validate(EmbeddingResult result) {
    if (result == null || result.vector() == null) {
      throw new InvalidEmbeddingException("El adaptador de embeddings devolvió un vector vacío");
    }
    if (result.vector().length != EXPECTED_DIMENSIONS) {
      throw new InvalidEmbeddingException("El vector tiene " + result.vector().length + " dimensiones, se esperaban " + EXPECTED_DIMENSIONS);
    }
  }
}
