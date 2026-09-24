package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de resiliencia con Resilience4j para el clasificador contextual externo (T2 de LLM-S13-H01):
 * - Ventana deslizante de 60 segundos basada en tiempo (TIME_BASED).
 * - Umbral de apertura: > 50% de fallas o llamadas lentas (timeout 300 ms).
 * - Rate Limiter: 200 RPM para salvaguardar la cuota de la API externa.
 */
@Configuration
public class ModerationResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ModerationResilienceConfig.class);

    public static final String CONTEXTUAL_MODERATION_CB = "contextualModeration";
    public static final String CONTEXTUAL_MODERATION_RL = "contextualModeration";

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    @Bean
    public CircuitBreaker contextualCircuitBreaker(
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${resilience4j.circuitbreaker.instances.contextualModeration.sliding-window-size:60}") int slidingWindowSize,
            @Value("${resilience4j.circuitbreaker.instances.contextualModeration.failure-rate-threshold:50.0}") float failureRateThreshold,
            @Value("${resilience4j.circuitbreaker.instances.contextualModeration.slow-call-duration-threshold:300ms}") Duration slowCallDurationThreshold,
            @Value("${resilience4j.circuitbreaker.instances.contextualModeration.minimum-number-of-calls:5}") int minimumNumberOfCalls,
            @Value("${resilience4j.circuitbreaker.instances.contextualModeration.wait-duration-in-open-state:60s}") Duration waitDurationInOpenState) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(slidingWindowSize)
                .failureRateThreshold(failureRateThreshold)
                .slowCallRateThreshold(failureRateThreshold)
                .slowCallDurationThreshold(slowCallDurationThreshold)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .waitDurationInOpenState(waitDurationInOpenState)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(CONTEXTUAL_MODERATION_CB, config);

        cb.getEventPublisher()
                .onStateTransition(event -> log.info("CIRCUIT BREAKER [{}] cambio de estado: {} -> {}",
                        CONTEXTUAL_MODERATION_CB, event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
                .onError(event -> log.warn("CIRCUIT BREAKER [{}] registro error (duracion: {} ms): {}",
                        CONTEXTUAL_MODERATION_CB, event.getElapsedDuration().toMillis(), event.getThrowable().getMessage()));

        return cb;
    }

    @Bean
    public RateLimiter contextualRateLimiter(
            RateLimiterRegistry rateLimiterRegistry,
            @Value("${resilience4j.ratelimiter.instances.contextualModeration.limit-for-period:200}") int limitForPeriod,
            @Value("${resilience4j.ratelimiter.instances.contextualModeration.limit-refresh-period:60s}") Duration limitRefreshPeriod) {

        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(limitForPeriod)
                .limitRefreshPeriod(limitRefreshPeriod)
                .timeoutDuration(Duration.ZERO)
                .build();

        return rateLimiterRegistry.rateLimiter(CONTEXTUAL_MODERATION_RL, config);
    }
}
