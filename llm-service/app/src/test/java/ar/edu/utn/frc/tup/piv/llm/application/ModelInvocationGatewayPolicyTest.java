package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ModelDeploymentRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelDeploymentSummary;
import java.util.UUID;

import ar.edu.utn.frc.tup.piv.llm.application.service.EmbeddingInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.ModelInvocationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayExecutor;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayProperties;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingPort;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayBudget;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayPolicy;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayUsageLog;
import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayUsageLog.Outcome;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.BudgetExceededException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.InvalidModelResponseException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.application.port.out.ModelInvocationPort;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationRequest;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ProviderUnavailableException;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.FunctionModelConfigRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** EP-02·H03: reintentos, circuit breaker, presupuesto y registro de uso del gateway. */
class ModelInvocationGatewayPolicyTest {
  private static final GatewayPolicy FAST = new GatewayPolicy(3, Duration.ZERO, 2, 50f, Duration.ofMinutes(1));
  private final GatewayUsageLog usage = new GatewayUsageLog();
  private final GatewayBudget budget = new GatewayBudget();

  @Test
  void retriesTransientFailuresAndRecordsUsage() {
    var calls = new AtomicInteger();
    var service = service(FAST, req -> {
      if (calls.incrementAndGet() < 3) throw new IllegalStateException("503 del proveedor");
      return new ModelInvocationResult("una pista", "fake", "m");
    });

    var result = service.invoke(ModelFunction.TUTOR, "sys", "pregunta", Duration.ofSeconds(1));

    assertThat(result.text()).isEqualTo("una pista");
    assertThat(calls).hasValue(3);
    var rec = usage.recent(1).get(0);
    assertThat(rec.outcome()).isEqualTo(Outcome.OK);
    assertThat(rec.attempts()).isEqualTo(3);
    assertThat(budget.snapshot(ModelFunction.TUTOR).calls()).isEqualTo(1);
  }

  @Test
  void doesNotRetryAnInvalidResponse() {
    var calls = new AtomicInteger();
    var service = service(FAST, req -> {
      calls.incrementAndGet();
      return new ModelInvocationResult("", "fake", "m");
    });

    assertThatThrownBy(() -> service.invoke(ModelFunction.TUTOR, "s", "p", Duration.ofSeconds(1)))
        .isInstanceOf(InvalidModelResponseException.class);
    assertThat(calls).hasValue(1);
    assertThat(usage.recent(1).get(0).outcome()).isEqualTo(Outcome.INVALID_RESPONSE);
  }

  @Test
  void opensTheBreakerAfterRepeatedFailuresAndFailsFast() {
    var calls = new AtomicInteger();
    var service = service(new GatewayPolicy(1, Duration.ZERO, 2, 50f, Duration.ofMinutes(1)), req -> {
      calls.incrementAndGet();
      throw new IllegalStateException("caído");
    });

    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(() -> service.invoke(ModelFunction.TUTOR, "s", "p", Duration.ofSeconds(1)))
          .isInstanceOf(IllegalStateException.class);
    }
    assertThatThrownBy(() -> service.invoke(ModelFunction.TUTOR, "s", "p", Duration.ofSeconds(1)))
        .isInstanceOf(ProviderUnavailableException.class);

    assertThat(calls).hasValue(2);
    assertThat(usage.recent(1).get(0).outcome()).isEqualTo(Outcome.BREAKER_OPEN);
  }

  @Test
  void refusesTheCallWhenTheFunctionBudgetIsExhausted() {
    budget.setLimit(ModelFunction.TUTOR, new GatewayBudget.Limit(1, 100.0));
    var service = service(FAST, req -> new ModelInvocationResult("ok", "fake", "m"));

    service.invoke(ModelFunction.TUTOR, "s", "p", Duration.ofSeconds(1));

    assertThatThrownBy(() -> service.invoke(ModelFunction.TUTOR, "s", "p", Duration.ofSeconds(1)))
        .isInstanceOf(BudgetExceededException.class);
    assertThat(usage.recent(1).get(0).outcome()).isEqualTo(Outcome.BUDGET_EXCEEDED);
  }

  @Test
  void summaryAggregatesByFunctionWithoutStoringContent() {
    var service = service(FAST, req -> new ModelInvocationResult("respuesta", "fake", "m"));
    service.invoke(ModelFunction.TUTOR, "s", "p", Duration.ofSeconds(1));

    var summary = usage.summary();
    assertThat(summary).hasSize(1);
    assertThat(summary.get(0).calls()).isEqualTo(1);
    assertThat(summary.get(0).errors()).isZero();
    assertThat(usage.recent(1).get(0).toString()).doesNotContain("respuesta");
  }

  private ModelInvocationService service(GatewayPolicy policy, Function<ModelInvocationRequest, ModelInvocationResult> fn) {
    var configs = mock(FunctionModelConfigRepository.class);
    var deployments = mock(ModelDeploymentRepository.class);
    var cfg1 = asignado(deployments, "fake", "m", "1", true);
    when(configs.find(ModelFunction.TUTOR)).thenReturn(Optional.of(cfg1));
    ModelInvocationPort port = new ModelInvocationPort() {
      public ModelInvocationResult invoke(ModelInvocationRequest r) { return fn.apply(r); }
      public String provider() { return "fake"; }
      public String model() { return "m"; }
    };
    return new ModelInvocationService(configs, deployments, List.of(port), policy, budget, usage);
  }

  @Test
  void usesTheTokensReportedByTheProviderInsteadOfEstimating() {
    var service = service(FAST, req -> new ModelInvocationResult("ok", "fake", "m", 1000, 500));

    service.invoke(ModelFunction.TUTOR, "s", "p", Duration.ofSeconds(1));

    var rec = usage.recent(1).get(0);
    assertThat(rec.inputTokens()).isEqualTo(1000);
    assertThat(rec.outputTokens()).isEqualTo(500);
  }

  @Test
  void embeddingsGoThroughTheSamePolicy() {
    var calls = new AtomicInteger();
    var configs = mock(FunctionModelConfigRepository.class);
    var deployments = mock(ModelDeploymentRepository.class);
    var cfg2 = asignado(deployments, "fake", "emb", "1", true);
    when(configs.find(ModelFunction.EMBEDDING)).thenReturn(Optional.of(cfg2));
    ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingPort port = new ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingPort() {
      public ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult embed(String text) {
        if (calls.incrementAndGet() < 2) throw new IllegalStateException("503");
        return new ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult(new float[768], "fake", "emb");
      }
      public List<ar.edu.utn.frc.tup.piv.llm.domain.ai.EmbeddingResult> embedBatch(List<String> texts) { return List.of(); }
      public String provider() { return "fake"; }
      public String model() { return "emb"; }
    };
    var service = new EmbeddingInvocationService(configs, deployments, port,
        new ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayExecutor(FAST, budget, usage));

    service.embed("hola", Duration.ofSeconds(1));

    assertThat(calls).hasValue(2);
    var rec = usage.recent(1).get(0);
    assertThat(rec.function()).isEqualTo(ModelFunction.EMBEDDING);
    assertThat(rec.outcome()).isEqualTo(Outcome.OK);
    assertThat(rec.attempts()).isEqualTo(2);
  }

  @Test
  void pricingComesFromConfiguration() {
    var props = new ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayProperties();
    props.getPricing().setUsdPer1kTokens(java.util.Map.of("acme", 1.0));
    var log = new GatewayUsageLog(null, props);

    assertThat(log.costOf("acme", 500, 500)).isEqualTo(1.0);
    assertThat(log.costOf("desconocido", 500, 500)).isZero();
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
