package ar.edu.utn.frc.tup.piv.llm.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.InvalidModelResponseException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationPort;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelTimeoutException;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.FunctionModelConfigRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModelInvocationServiceTest {

  @Test
  void invokesTheAdapterWhenTheFunctionIsEnabled() {
    Adapter adapter = request -> new ModelInvocationResult("una pista socrática", "fake", "fake-socratic-v1");
    var service = serviceWithAdapter(adapter);

    var result = service.invoke(ModelFunction.TUTOR, "system", "¿cómo ordeno una lista?", Duration.ofSeconds(1));

    assertThat(result.text()).isEqualTo("una pista socrática");
  }

  @Test
  void refusesToInvokeAFunctionWithoutModelAssigned() {
    var configs = mock(FunctionModelConfigRepository.class);
    when(configs.find(ModelFunction.TUTOR)).thenReturn(Optional.empty());
    Adapter adapter = request -> {
      throw new AssertionError("no debería invocarse el adaptador sin configuración");
    };
    var service = new ModelInvocationService(configs, adapter);

    assertThatThrownBy(() -> service.invoke(ModelFunction.TUTOR, "system", "pregunta", Duration.ofSeconds(1)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsAnOutOfSchemaResponseInsteadOfPropagatingIt() {
    Adapter adapter = request -> new ModelInvocationResult("", "fake", "fake-socratic-v1");
    var service = serviceWithAdapter(adapter);

    assertThatThrownBy(() -> service.invoke(ModelFunction.TUTOR, "system", "pregunta", Duration.ofSeconds(1)))
        .isInstanceOf(InvalidModelResponseException.class);
  }

  @Test
  void cutsTheCallWhenTheAdapterExceedsTheConfiguredTimeout() {
    Adapter adapter = request -> {
      try {
        Thread.sleep(300);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
      }
      return new ModelInvocationResult("tarde", "fake", "fake-socratic-v1");
    };
    var service = serviceWithAdapter(adapter);

    long start = System.currentTimeMillis();
    assertThatThrownBy(() -> service.invoke(ModelFunction.TUTOR, "system", "pregunta", Duration.ofMillis(50)))
        .isInstanceOf(ModelTimeoutException.class);
    long elapsed = System.currentTimeMillis() - start;

    assertThat(elapsed).isLessThan(300);
  }

  private ModelInvocationService serviceWithAdapter(Adapter adapter) {
    var configs = mock(FunctionModelConfigRepository.class);
    when(configs.find(ModelFunction.TUTOR))
        .thenReturn(Optional.of(new FunctionModelConfigRepository.Config("fake", "fake-socratic-v1", "1", true)));
    return new ModelInvocationService(configs, adapter);
  }

  /** {@link ModelInvocationPort} declara `provider()`/`model()` además de `invoke()`; esta
   * subinterfaz con defaults lo vuelve funcional para poder pasar una lambda como test double sin
   * escribir una clase entera por escenario. */
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
