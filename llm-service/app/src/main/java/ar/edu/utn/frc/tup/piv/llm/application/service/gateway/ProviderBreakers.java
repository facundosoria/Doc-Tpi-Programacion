package ar.edu.utn.frc.tup.piv.llm.application.service.gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Un circuit breaker por proveedor (EP-02·H03): un proveedor caído no arrastra a los demás. */
public class ProviderBreakers {
  private static final Logger log = LoggerFactory.getLogger(ProviderBreakers.class);
  private final GatewayPolicy policy;
  private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

  public ProviderBreakers(GatewayPolicy policy) { this.policy = policy; }

  public CircuitBreaker of(String provider) {
    return breakers.computeIfAbsent(provider, p -> {
      var cb = CircuitBreaker.of("llm-provider-" + p, CircuitBreakerConfig.custom()
          .minimumNumberOfCalls(policy.breakerMinCalls())
          .slidingWindowSize(Math.max(policy.breakerMinCalls(), 10))
          .failureRateThreshold(policy.breakerFailureRate())
          .waitDurationInOpenState(policy.breakerOpenWait())
          .build());
      cb.getEventPublisher().onStateTransition(e ->
          log.warn("ALERTA proveedor {} breaker {}", p, e.getStateTransition()));
      return cb;
    });
  }
}
