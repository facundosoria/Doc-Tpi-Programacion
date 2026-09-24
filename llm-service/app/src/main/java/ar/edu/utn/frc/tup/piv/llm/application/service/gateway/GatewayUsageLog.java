package ar.edu.utn.frc.tup.piv.llm.application.service.gateway;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Registro de costo/latencia/errores de cada llamada al gateway (EP-02, KPI de observabilidad).
 * MOCK: buffer en memoria de las últimas 500 llamadas, tokens estimados (chars/4) y precios por
 * 1k tokens hardcodeados. Nunca guarda el contenido de prompts ni respuestas. Publica métricas
 * Micrometer `llm.gateway.calls` / `llm.gateway.latency` / `llm.gateway.cost.usd` por función. */
@Component
public class GatewayUsageLog {
  static final int CAPACITY = 500;

  public enum Outcome { OK, TIMEOUT, INVALID_RESPONSE, PROVIDER_ERROR, BREAKER_OPEN, BUDGET_EXCEEDED }

  public record CallRecord(Instant at, ModelFunction function, String provider, String model, long latencyMs,
      Outcome outcome, int inputTokens, int outputTokens, double costUsd, int attempts) {}

  public record Summary(ModelFunction function, long calls, long errors, int inputTokens, int outputTokens,
      double costUsd, double avgLatencyMs) {}

  private final Deque<CallRecord> records = new ArrayDeque<>();
  private final MeterRegistry meters;
  private final Map<String, Double> usdPer1kTokens;

  public GatewayUsageLog() { this(null, new GatewayProperties()); }

  @Autowired
  public GatewayUsageLog(@Autowired(required = false) MeterRegistry meters, GatewayProperties properties) {
    this.meters = meters;
    this.usdPer1kTokens = properties.getPricing().getUsdPer1kTokens();
  }

  public static int estimateTokens(String text) { return text == null ? 0 : (text.length() + 3) / 4; }

  public double costOf(String provider, int inputTokens, int outputTokens) {
    double price = usdPer1kTokens.getOrDefault(provider == null ? "" : provider.toLowerCase(), 0.0);
    return (inputTokens + outputTokens) / 1000.0 * price;
  }

  public void record(ModelFunction function, String provider, String model, long latencyMs, Outcome outcome,
      int inputTokens, int outputTokens, int attempts) {
    double cost = outcome == Outcome.OK ? costOf(provider, inputTokens, outputTokens) : 0.0;
    var rec = new CallRecord(Instant.now(), function, provider, model, latencyMs, outcome, inputTokens,
        outputTokens, cost, attempts);
    synchronized (records) {
      if (records.size() == CAPACITY) records.removeFirst();
      records.addLast(rec);
    }
    if (meters != null) {
      String fn = function.name().toLowerCase();
      meters.counter("llm.gateway.calls", "function", fn, "outcome", outcome.name()).increment();
      meters.timer("llm.gateway.latency", "function", fn).record(Duration.ofMillis(latencyMs));
      if (cost > 0) meters.counter("llm.gateway.cost.usd", "function", fn).increment(cost);
    }
  }

  public List<CallRecord> recent(int limit) {
    synchronized (records) {
      var all = new ArrayList<>(records);
      return all.subList(Math.max(0, all.size() - limit), all.size());
    }
  }

  public List<Summary> summary() {
    Map<ModelFunction, List<CallRecord>> by = new EnumMap<>(ModelFunction.class);
    synchronized (records) { records.forEach(r -> by.computeIfAbsent(r.function(), k -> new ArrayList<>()).add(r)); }
    return by.entrySet().stream().map(e -> {
      var rs = e.getValue();
      return new Summary(e.getKey(), rs.size(), rs.stream().filter(r -> r.outcome() != Outcome.OK).count(),
          rs.stream().mapToInt(CallRecord::inputTokens).sum(), rs.stream().mapToInt(CallRecord::outputTokens).sum(),
          rs.stream().mapToDouble(CallRecord::costUsd).sum(),
          rs.stream().mapToLong(CallRecord::latencyMs).average().orElse(0));
    }).toList();
  }
}
