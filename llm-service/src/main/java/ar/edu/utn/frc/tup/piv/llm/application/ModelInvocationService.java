package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationPort;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationRequest;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelInvocationResult;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelResponseSchema;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelTimeoutException;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.FunctionModelConfigRepository;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/** El caso de uso de `LLM-S01-H10`: resuelve la función contra
 * {@link FunctionModelConfigRepository}, invoca el {@link ModelInvocationPort} con timeout, y
 * valida la respuesta contra el schema. Reutilizable por cualquier función futura, no solo el
 * tutor — hoy el único llamador es {@link TutorInteractionService}. */
@Service
public class ModelInvocationService {
  private final FunctionModelConfigRepository configs;
  private final ModelInvocationPort adapter;
  private final ModelResponseSchema schema = new ModelResponseSchema();

  public ModelInvocationService(FunctionModelConfigRepository configs, ModelInvocationPort adapter) {
    this.configs = configs;
    this.adapter = adapter;
  }

  public ModelInvocationResult invoke(ModelFunction function, String systemPrompt, String userPrompt, Duration timeout) {
    var config = configs.find(function)
        .orElseThrow(() -> new IllegalStateException("La función " + name(function) + " no tiene modelo asignado"));
    if (!config.enabled()) {
      throw new IllegalStateException("La función " + name(function) + " está deshabilitada");
    }

    var request = new ModelInvocationRequest(function, systemPrompt, userPrompt, timeout);
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    try {
      ModelInvocationResult result = CompletableFuture.supplyAsync(() -> adapter.invoke(request), executor)
          .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
      schema.validate(function, result.text());
      return result;
    } catch (java.util.concurrent.TimeoutException exception) {
      throw new ModelTimeoutException(
          "El adaptador de " + name(function) + " superó el timeout de " + timeout.toMillis() + "ms");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Invocación de " + name(function) + " interrumpida", exception);
    } catch (ExecutionException exception) {
      throw new IllegalStateException("Fallo al invocar el modelo de " + name(function), exception.getCause());
    } finally {
      executor.shutdownNow();
    }
  }

  private String name(ModelFunction function) {
    return function.name().toLowerCase(Locale.ROOT);
  }
}
