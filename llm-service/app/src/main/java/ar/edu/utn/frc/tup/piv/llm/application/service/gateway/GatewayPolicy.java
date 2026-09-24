package ar.edu.utn.frc.tup.piv.llm.application.service.gateway;

import java.time.Duration;

/** Política de resiliencia del AI Gateway (EP-02·H03): reintentos y circuit breaker por proveedor.
 * Valores hardcodeados por ahora; migrar a `application.yml` cuando se confirmen. */
public record GatewayPolicy(int maxAttempts, Duration backoff, int breakerMinCalls, float breakerFailureRate,
    Duration breakerOpenWait) {

  public static GatewayPolicy defaults() {
    return new GatewayPolicy(3, Duration.ofMillis(200), 5, 50f, Duration.ofSeconds(30));
  }

  /** Sin reintentos ni breaker: comportamiento previo a H03, usado por los constructores legacy. */
  public static GatewayPolicy disabled() {
    return new GatewayPolicy(1, Duration.ZERO, Integer.MAX_VALUE, 100f, Duration.ofSeconds(1));
  }

  public boolean breakerEnabled() { return breakerMinCalls != Integer.MAX_VALUE; }
}
