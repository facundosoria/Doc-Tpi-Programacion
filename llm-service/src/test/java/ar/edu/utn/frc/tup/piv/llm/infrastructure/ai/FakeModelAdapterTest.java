package ar.edu.utn.frc.tup.piv.llm.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationRequest;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** Los 3 escenarios BDD de `LLM-S01-H10`. */
class FakeModelAdapterTest {

  @Test
  void respondsWithSimulatedDataThroughThePort() {
    var adapter = new FakeModelAdapter(Duration.ZERO, false);
    var request = new ModelInvocationRequest(ModelFunction.TUTOR, "system", "¿Cómo ordeno una lista?", Duration.ofSeconds(1));

    var result = adapter.invoke(request);

    assertThat(result.text()).isNotBlank();
    assertThat(result.provider()).isEqualTo("fake");
    assertThat(result.model()).isEqualTo("fake-socratic-v1");
  }

  @Test
  void canBeForcedToReturnAnOutOfSchemaResponse() {
    var adapter = new FakeModelAdapter(Duration.ZERO, true);
    var request = new ModelInvocationRequest(ModelFunction.TUTOR, "system", "pregunta", Duration.ofSeconds(1));

    var result = adapter.invoke(request);

    assertThat(result.text()).isBlank();
  }

  @Test
  void canSimulateADelayLongerThanTheCallersTimeout() {
    var adapter = new FakeModelAdapter(Duration.ofMillis(200), false);
    var request = new ModelInvocationRequest(ModelFunction.TUTOR, "system", "pregunta", Duration.ofMillis(50));

    long start = System.currentTimeMillis();
    adapter.invoke(request);
    long elapsed = System.currentTimeMillis() - start;

    assertThat(elapsed).isGreaterThanOrEqualTo(200);
  }
}
