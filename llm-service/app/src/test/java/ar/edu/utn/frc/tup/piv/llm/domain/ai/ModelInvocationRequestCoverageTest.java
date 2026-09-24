package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ModelInvocationRequestCoverageTest {
  @Test
  void exposesItsRawFields() {
    var request = new ModelInvocationRequest(ModelFunction.TUTOR, "system", "pregunta", Duration.ofSeconds(3));

    assertThat(request.function()).isEqualTo(ModelFunction.TUTOR);
    assertThat(request.systemPrompt()).isEqualTo("system");
    assertThat(request.userPrompt()).isEqualTo("pregunta");
    assertThat(request.timeout()).isEqualTo(Duration.ofSeconds(3));
  }

  @Test
  void requiresAFunction() {
    assertThatThrownBy(() -> new ModelInvocationRequest(null, "system", "pregunta", Duration.ofSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("function");
  }

  @Test
  void requiresANonBlankUserPrompt() {
    assertThatThrownBy(() -> new ModelInvocationRequest(ModelFunction.TUTOR, "system", "  ", Duration.ofSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("userPrompt");
  }

  @Test
  void rejectsNullOrNonPositiveTimeouts() {
    assertThatThrownBy(() -> new ModelInvocationRequest(ModelFunction.TUTOR, "system", "pregunta", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ModelInvocationRequest(ModelFunction.TUTOR, "system", "pregunta", Duration.ZERO))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new ModelInvocationRequest(ModelFunction.TUTOR, "system", "pregunta", Duration.ofSeconds(-1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void recordSemanticsWork() {
    var a = new ModelInvocationRequest(ModelFunction.TUTOR, "system", "pregunta", Duration.ofSeconds(1));
    var b = new ModelInvocationRequest(ModelFunction.TUTOR, "system", "pregunta", Duration.ofSeconds(1));

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }
}