package ar.edu.utn.frc.tup.piv.llm.infrastructure.ai;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationPort;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationRequest;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** El fake que pide `LLM-S01-H10` (T3): no llama a ningún proveedor real. Adaptado de
 * `codigo-ejemplo/ms-evaluacion-llm/.../adapter/GroqAdapter.java` — acá no hay rama "real" que
 * mockear (ese proyecto usaba `mock-key` como atajo): este adaptador es *siempre* simulado, a
 * propósito, porque H10 solo pide el puerto + fake en esta etapa. El adaptador real a un
 * proveedor (langchain4j, ADR-016) es trabajo futuro fuera de esta historia.
 *
 * <p>El constructor con parámetros es solo para tests: permite simular una demora (H10·CA5,
 * timeout) o una respuesta inválida (H10·CA4, schema) sin depender de un mock de red. */
@Component
public class FakeModelAdapter implements ModelInvocationPort {
  private static final String PROVIDER = "fake";
  private static final String MODEL = "fake-socratic-v1";

  private final Duration artificialDelay;
  private final boolean forceInvalidResponse;

  public FakeModelAdapter() {
    this(Duration.ZERO, false);
  }

  FakeModelAdapter(Duration artificialDelay, boolean forceInvalidResponse) {
    this.artificialDelay = artificialDelay;
    this.forceInvalidResponse = forceInvalidResponse;
  }

  @Override
  public ModelInvocationResult invoke(ModelInvocationRequest request) {
    if (!artificialDelay.isZero()) {
      try {
        Thread.sleep(artificialDelay.toMillis());
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Adaptador fake interrumpido", exception);
      }
    }
    if (forceInvalidResponse) {
      return new ModelInvocationResult("", PROVIDER, MODEL);
    }
    return new ModelInvocationResult(respond(request), PROVIDER, MODEL);
  }

  private String respond(ModelInvocationRequest request) {
    if (request.function() != ModelFunction.TUTOR) {
      throw new UnsupportedOperationException("El fake todavía no simula la función " + request.function());
    }
    String excerpt = firstWords(request.userPrompt(), 12);
    return "¿Qué estructura o patrón te ayudaría a resolver \"" + excerpt
        + "\" sin escribir todavía el código completo? Contame qué probaste hasta ahora.";
  }

  private String firstWords(String text, int count) {
    if (text == null || text.isBlank()) return "tu consulta";
    String[] words = text.trim().split("\\s+");
    return String.join(" ", java.util.Arrays.copyOf(words, Math.min(count, words.length)));
  }

  @Override
  public String provider() {
    return PROVIDER;
  }

  @Override
  public String model() {
    return MODEL;
  }
}
