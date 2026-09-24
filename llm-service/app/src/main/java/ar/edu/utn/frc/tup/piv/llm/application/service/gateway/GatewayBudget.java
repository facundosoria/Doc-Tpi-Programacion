package ar.edu.utn.frc.tup.piv.llm.application.service.gateway;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.BudgetExceededException;
import ar.edu.utn.frc.tup.piv.llm.domain.ai.ModelFunction;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Presupuesto diario por función de IA (EP-02, restricción "presupuesto y cuotas por función y por
 * período"). MOCK: límites hardcodeados y contadores en memoria (se pierden al reiniciar);
 * el paso real es persistirlos y editarlos por admin. Alerta por log al 80 % y al agotarse. */
@Component
public class GatewayBudget {
  private static final Logger log = LoggerFactory.getLogger(GatewayBudget.class);
  private static final double ALERT_RATIO = 0.8;

  public record Limit(int maxCalls, double maxCostUsd) {}

  public record Snapshot(ModelFunction function, LocalDate period, int calls, double costUsd, Limit limit) {}

  private final Clock clock;
  private final Map<ModelFunction, Limit> limits = new EnumMap<>(ModelFunction.class);
  private final Map<ModelFunction, int[]> calls = new EnumMap<>(ModelFunction.class);
  private final Map<ModelFunction, double[]> cost = new EnumMap<>(ModelFunction.class);
  private LocalDate period;

  public GatewayBudget() { this(Clock.systemUTC()); }

  GatewayBudget(Clock clock) {
    this.clock = clock;
    this.period = LocalDate.now(clock.withZone(ZoneOffset.UTC));
    limits.put(ModelFunction.TUTOR, new Limit(5000, 10.0));
    limits.put(ModelFunction.EVALUATOR, new Limit(2000, 20.0));
    limits.put(ModelFunction.MODERATOR, new Limit(20000, 5.0));
    limits.put(ModelFunction.GENERATOR, new Limit(500, 5.0));
    limits.put(ModelFunction.EMBEDDING, new Limit(10000, 2.0));
    for (ModelFunction f : ModelFunction.values()) {
      calls.put(f, new int[1]);
      cost.put(f, new double[1]);
    }
  }

  public synchronized void setLimit(ModelFunction function, Limit limit) { limits.put(function, limit); }

  /** Falla si la función ya agotó su presupuesto del período. */
  public synchronized void check(ModelFunction function) {
    rollover();
    Limit limit = limits.get(function);
    if (calls.get(function)[0] >= limit.maxCalls() || cost.get(function)[0] >= limit.maxCostUsd()) {
      log.error("ALERTA presupuesto agotado function={} period={}", function, period);
      throw new BudgetExceededException(function,
          "La función " + function.name().toLowerCase() + " agotó su presupuesto diario");
    }
  }

  public synchronized void record(ModelFunction function, double costUsd) {
    rollover();
    calls.get(function)[0]++;
    cost.get(function)[0] += costUsd;
    Limit limit = limits.get(function);
    if (calls.get(function)[0] == (int) Math.ceil(limit.maxCalls() * ALERT_RATIO)
        || cost.get(function)[0] >= limit.maxCostUsd() * ALERT_RATIO) {
      log.warn("ALERTA presupuesto al 80% function={} calls={} costUsd={}", function,
          calls.get(function)[0], cost.get(function)[0]);
    }
  }

  public synchronized Snapshot snapshot(ModelFunction function) {
    rollover();
    return new Snapshot(function, period, calls.get(function)[0], cost.get(function)[0], limits.get(function));
  }

  private void rollover() {
    LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
    if (!today.equals(period)) {
      period = today;
      calls.values().forEach(a -> a[0] = 0);
      cost.values().forEach(a -> a[0] = 0);
    }
  }
}
