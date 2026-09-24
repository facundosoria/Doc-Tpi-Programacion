package ar.edu.utn.frc.tup.piv.llm.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.FunctionModelConfigRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ModelDeploymentRepository;
import ar.edu.utn.frc.tup.piv.llm.application.port.out.ModelInvocationPort;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelDeploymentSummary;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModelInvocationServiceCoverageTest {

  @Test
  void refusesToInvokeADisabledFunction() {
    var configs = configs(false);
    Adapter adapter = request -> new ModelInvocationResult("ok", "fake", "fake-socratic-v1");
    var service = new ModelInvocationService(configs, mock(ModelDeploymentRepository.class), adapter);

    assertThatThrownBy(() -> service.invoke(ModelFunction.TUTOR, "system", "pregunta", Duration.ofSeconds(1)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("deshabilitada");
  }

  @Test
  void wrapsAdapterFailuresPreservingTheCause() {
    var service = serviceWithAdapter(request -> {
      throw new IllegalStateException("boom");
    });

    assertThatThrownBy(() -> service.invoke(ModelFunction.TUTOR, "system", "pregunta", Duration.ofSeconds(1)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Fallo al invocar el modelo de tutor");
  }

  @Test
  void interruptionIsReAssertedAndWrapped() {
    var service = serviceWithAdapter(request -> new ModelInvocationResult("ok", "fake", "fake-socratic-v1"));
    Thread.currentThread().interrupt();
    try {
      assertThatThrownBy(() -> service.invoke(ModelFunction.TUTOR, "system", "pregunta", Duration.ofSeconds(5)))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("interrumpida");
    } finally {
      Thread.interrupted();
    }
  }

  private FunctionModelConfigRepository configs(boolean enabled) {
    var configs = mock(FunctionModelConfigRepository.class);
    when(configs.find(ModelFunction.TUTOR))
        .thenReturn(Optional.of(new FunctionModelConfigRepository.Config(UUID.randomUUID(), enabled)));
    return configs;
  }

  private ModelInvocationService serviceWithAdapter(Adapter adapter) {
    var deployments = mock(ModelDeploymentRepository.class);
    when(deployments.byId(any())).thenReturn(Optional.of(
        new ModelDeploymentSummary(UUID.randomUUID(), "fake", "fake-socratic-v1", "1.0", "ENABLED")));
    return new ModelInvocationService(configs(true), deployments, adapter);
  }

  @FunctionalInterface
  private interface Adapter extends ModelInvocationPort {
    @Override
    default String provider() {
      return "fake";
    }

    @Override
    default String model() {
      return "fake-socratic-v1";
    }
  }
}