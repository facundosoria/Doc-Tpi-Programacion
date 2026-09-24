package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayBudget;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayExecutor;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayPolicy;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayUsageLog;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.application.port.out.ModelInvocationPort;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationRequest;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelResponseSchema;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelTimeoutException;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.FunctionModelConfigRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ModelDeploymentRepository;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** El caso de uso de `LLM-S01-H10` y `LLM-EP02-H02`: resuelve la función contra
 * {@link FunctionModelConfigRepository}, selecciona el {@link ModelInvocationPort} según el
 * proveedor configurado en la base de datos y lo invoca a través del {@link GatewayExecutor}
 * (presupuesto, breaker, timeout, reintentos, registro de uso), validando la respuesta contra el
 * schema. Permite alternar proveedores en runtime sin redespliegue (CA2). */
@Service
public class ModelInvocationService {
  private final FunctionModelConfigRepository configs;
  private final ModelDeploymentRepository deployments;
  private final Map<String, ModelInvocationPort> adapters;
  private final ModelResponseSchema schema = new ModelResponseSchema();
  private final GatewayExecutor executor;

  @org.springframework.beans.factory.annotation.Autowired
  public ModelInvocationService(FunctionModelConfigRepository configs, ModelDeploymentRepository deployments,
      List<ModelInvocationPort> adapters, GatewayExecutor executor) {
    this.configs = configs;
    this.deployments = deployments;
    this.adapters = adapters.stream()
        .collect(Collectors.toMap(
            port -> port.provider().toLowerCase(Locale.ROOT),
            Function.identity(),
            (existing, replacement) -> existing));
    this.executor = executor;
  }

  public ModelInvocationService(FunctionModelConfigRepository configs, ModelDeploymentRepository deployments,
      List<ModelInvocationPort> adapters, GatewayPolicy policy, GatewayBudget budget, GatewayUsageLog usageLog) {
    this(configs, deployments, adapters, new GatewayExecutor(policy, budget, usageLog));
  }

  /** Sin reintentos, breaker ni presupuesto: comportamiento previo a EP-02·H03. */
  public ModelInvocationService(FunctionModelConfigRepository configs, ModelDeploymentRepository deployments,
      List<ModelInvocationPort> adapters) {
    this(configs, deployments, adapters, GatewayExecutor.disabled());
  }

  public ModelInvocationService(FunctionModelConfigRepository configs, ModelDeploymentRepository deployments,
      ModelInvocationPort adapter) {
    this(configs, deployments, List.of(adapter));
  }

  public ModelInvocationResult invoke(ModelFunction function, String systemPrompt, String userPrompt, Duration timeout) {
    var config = configs.find(function)
        .orElseThrow(() -> new IllegalStateException("La función " + name(function) + " no tiene modelo asignado"));
    if (!config.enabled()) {
      throw new IllegalStateException("La función " + name(function) + " está deshabilitada");
    }

    // Desde la V26 la asignación guarda el despliegue; proveedor y modelo salen de ahí.
    var deployment = deployments.byId(config.modelDeploymentId())
        .orElseThrow(() -> new IllegalStateException(
            "El despliegue asignado a " + name(function) + " ya no existe"));
    String providerKey = deployment.provider() != null ? deployment.provider().toLowerCase(Locale.ROOT) : "";
    ModelInvocationPort adapter = adapters.get(providerKey);
    if (adapter == null) {
      throw new IllegalStateException(
          "No hay adaptador registrado para el proveedor '" + deployment.provider() + "' asignado a " + name(function));
    }

    var request = new ModelInvocationRequest(function, systemPrompt, userPrompt, timeout, deployment.modelId());
    int estimatedIn = GatewayUsageLog.estimateTokens(systemPrompt) + GatewayUsageLog.estimateTokens(userPrompt);
    return executor.run(new GatewayExecutor.Spec<>(function, providerKey, deployment.modelId(), estimatedIn, timeout,
        () -> adapter.invoke(request),
        result -> new int[] {
            result.inputTokens() != null ? result.inputTokens() : estimatedIn,
            result.outputTokens() != null ? result.outputTokens() : GatewayUsageLog.estimateTokens(result.text())},
        result -> schema.validate(function, result.text()),
        "El adaptador de " + name(function) + " superó el timeout de " + timeout.toMillis() + "ms",
        "Fallo al invocar el modelo de " + name(function),
        ModelTimeoutException::new,
        true));
  }

  private String name(ModelFunction function) {
    return function.name().toLowerCase(Locale.ROOT);
  }
}
