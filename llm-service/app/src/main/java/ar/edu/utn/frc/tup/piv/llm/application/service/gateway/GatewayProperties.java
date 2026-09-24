package ar.edu.utn.frc.tup.piv.llm.application.service.gateway;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuración del AI Gateway (`llm.gateway.resilience.*` y `llm.gateway.pricing.*`). Los defaults
 * son los de {@link GatewayPolicy#defaults()}, así que sin configurar nada el comportamiento no cambia. */
@Component
@ConfigurationProperties(prefix = "llm.gateway")
public class GatewayProperties {
  private final Resilience resilience = new Resilience();
  private final Pricing pricing = new Pricing();

  public Resilience getResilience() { return resilience; }

  public Pricing getPricing() { return pricing; }

  public GatewayPolicy policy() {
    return new GatewayPolicy(resilience.maxAttempts, Duration.ofMillis(resilience.backoffMs),
        resilience.breakerMinCalls, resilience.breakerFailureRate, Duration.ofSeconds(resilience.breakerOpenWaitS));
  }

  public static class Resilience {
    private int maxAttempts = 3;
    private long backoffMs = 200;
    private int breakerMinCalls = 5;
    private float breakerFailureRate = 50f;
    private long breakerOpenWaitS = 30;

    public void setMaxAttempts(int v) { this.maxAttempts = Math.max(1, v); }
    public void setBackoffMs(long v) { this.backoffMs = Math.max(0, v); }
    public void setBreakerMinCalls(int v) { this.breakerMinCalls = Math.max(1, v); }
    public void setBreakerFailureRate(float v) { this.breakerFailureRate = v; }
    public void setBreakerOpenWaitS(long v) { this.breakerOpenWaitS = Math.max(1, v); }
  }

  public static class Pricing {
    private Map<String, Double> usdPer1kTokens = new HashMap<>(Map.of("groq", 0.0002, "fake", 0.0));

    public Map<String, Double> getUsdPer1kTokens() { return usdPer1kTokens; }
    public void setUsdPer1kTokens(Map<String, Double> v) { this.usdPer1kTokens = v; }
  }
}
