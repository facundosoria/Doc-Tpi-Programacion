package ar.edu.utn.frc.tup.piv.llm.application.service.gateway;

import ar.edu.utn.frc.tup.piv.llm.application.service.gateway.GatewayUsageLog.Outcome;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ProviderUnavailableException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Decorador único de toda llamada a un proveedor (EP-02·H03): presupuesto, circuit breaker por
 * proveedor, timeout, reintentos y registro de uso. {@code ModelInvocationService},
 * {@code EmbeddingInvocationService} y el chat de prueba del admin pasan por acá, así que la
 * política es una sola y no hay caminos que la esquiven. */
@Component
public class GatewayExecutor {
  private final GatewayPolicy policy;
  private final GatewayBudget budget;
  private final GatewayUsageLog usageLog;
  private final ProviderBreakers breakers;

  @Autowired
  public GatewayExecutor(GatewayProperties properties, GatewayBudget budget, GatewayUsageLog usageLog) {
    this(properties.policy(), budget, usageLog);
  }

  public GatewayExecutor(GatewayPolicy policy, GatewayBudget budget, GatewayUsageLog usageLog) {
    this.policy = policy;
    this.budget = budget;
    this.usageLog = usageLog;
    this.breakers = new ProviderBreakers(policy);
  }

  /** Sin reintentos, breaker ni presupuesto efectivo: comportamiento previo a H03. */
  public static GatewayExecutor disabled() {
    return new GatewayExecutor(GatewayPolicy.disabled(), new GatewayBudget(), new GatewayUsageLog());
  }

  /** @param usage {in, out} tokens reales del resultado, o {@code null} para estimarlos (out=0 en embeddings).
   * @param validator lanza si el resultado no es válido; esa falla no se reintenta ni cuenta contra el breaker.
   * @param retry {@code false} para llamadas con efectos parciales (streaming). */
  public record Spec<T>(ModelFunction function, String provider, String model, int inputTokens, Duration timeout,
      Supplier<T> call, Function<T, int[]> usage, Consumer<T> validator, String timeoutMessage,
      String failureMessage, Function<String, RuntimeException> timeoutFactory, boolean retry) {}

  public <T> T run(Spec<T> spec) {
    var function = spec.function();
    String provider = spec.provider() == null ? "" : spec.provider().toLowerCase(java.util.Locale.ROOT);
    try {
      budget.check(function);
    } catch (RuntimeException exception) {
      usageLog.record(function, provider, spec.model(), 0, Outcome.BUDGET_EXCEEDED, spec.inputTokens(), 0, 0);
      throw exception;
    }
    var breaker = policy.breakerEnabled() ? breakers.of(provider) : null;
    if (breaker != null && !breaker.tryAcquirePermission()) {
      usageLog.record(function, provider, spec.model(), 0, Outcome.BREAKER_OPEN, spec.inputTokens(), 0, 0);
      throw new ProviderUnavailableException("El proveedor '" + provider + "' no está disponible (circuit breaker abierto)");
    }

    int maxAttempts = spec.retry() ? policy.maxAttempts() : 1;
    long started = System.nanoTime();
    RuntimeException last = null;
    boolean timedOut = false;
    int attempt = 0;
    while (attempt < maxAttempts) {
      attempt++;
      try {
        T result = callOnce(spec);
        int[] tokens = spec.usage() != null ? spec.usage().apply(result) : null;
        int in = tokens != null ? tokens[0] : spec.inputTokens();
        int out = tokens != null ? tokens[1] : 0;
        try {
          spec.validator().accept(result);
        } catch (RuntimeException invalid) {
          // El proveedor respondió: no es una falla de disponibilidad, no se reintenta.
          if (breaker != null) breaker.onSuccess(elapsed(started), TimeUnit.MILLISECONDS);
          usageLog.record(function, provider, spec.model(), elapsed(started), Outcome.INVALID_RESPONSE, in, out, attempt);
          throw invalid;
        }
        if (breaker != null) breaker.onSuccess(elapsed(started), TimeUnit.MILLISECONDS);
        usageLog.record(function, provider, spec.model(), elapsed(started), Outcome.OK, in, out, attempt);
        budget.record(function, usageLog.costOf(provider, in, out));
        return result;
      } catch (Timeout timeout) {
        timedOut = true;
        last = timeout;
      } catch (Transient failure) {
        timedOut = false;
        last = failure;
      }
      if (attempt < maxAttempts) pause();
    }
    if (breaker != null) breaker.onError(elapsed(started), TimeUnit.MILLISECONDS, last);
    usageLog.record(function, provider, spec.model(), elapsed(started),
        timedOut ? Outcome.TIMEOUT : Outcome.PROVIDER_ERROR, spec.inputTokens(), 0, attempt);
    if (timedOut) throw spec.timeoutFactory().apply(spec.timeoutMessage());
    throw new IllegalStateException(spec.failureMessage(), last.getCause());
  }

  private <T> T callOnce(Spec<T> spec) {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    try {
      return CompletableFuture.supplyAsync(spec.call(), executor).get(spec.timeout().toMillis(), TimeUnit.MILLISECONDS);
    } catch (java.util.concurrent.TimeoutException exception) {
      throw new Timeout();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(spec.failureMessage() + " (interrumpida)", exception);
    } catch (ExecutionException exception) {
      throw new Transient(exception.getCause());
    } finally {
      executor.shutdownNow();
    }
  }

  private long elapsed(long startedNanos) { return (System.nanoTime() - startedNanos) / 1_000_000; }

  private void pause() {
    if (policy.backoff().isZero()) return;
    try {
      Thread.sleep(policy.backoff().toMillis());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  private static final class Timeout extends RuntimeException {
    Timeout() { super(null, null, false, false); }
  }

  private static final class Transient extends RuntimeException {
    Transient(Throwable cause) { super(cause); }
  }
}
