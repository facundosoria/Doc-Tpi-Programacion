package ar.edu.utn.frc.tup.piv.llm.application.service.agent;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Registro y control de cuotas diarias por alumno y función (HU LLM-S18-H01 · T5).
 */
@Component
public class QuotaRegistry {

  public static final String FUNCTION_AGENT = "agent";
  public static final String FUNCTION_TUTOR = "tutor";

  private final Map<String, Integer> defaultLimits = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Integer>> customLimits = new ConcurrentHashMap<>();
  private final Map<String, Integer> usageCounters = new ConcurrentHashMap<>();
  private volatile LocalDate lastResetDate = LocalDate.now(ZoneOffset.UTC);

  public QuotaRegistry() {
    registerFunction(FUNCTION_AGENT, 20);
    registerFunction(FUNCTION_TUTOR, 50);
  }

  public void registerFunction(String function, int defaultDailyLimit) {
    defaultLimits.put(function.toLowerCase(), defaultDailyLimit);
  }

  public synchronized void consume(String function, String studentId) {
    checkDayRollover();
    String fn = function.toLowerCase();
    int limit = resolveLimit(fn, studentId);
    String key = buildKey(fn, studentId);
    int current = usageCounters.getOrDefault(key, 0);

    if (current >= limit) {
      long retryAfter = calculateSecondsUntilMidnightUtc();
      throw new QuotaExceededException(
          fn,
          retryAfter,
          "Alcanzaste tu límite diario de consultas para la función '" + fn + "'."
      );
    }

    usageCounters.put(key, current + 1);
  }

  public int getRemaining(String function, String studentId) {
    checkDayRollover();
    String fn = function.toLowerCase();
    int limit = resolveLimit(fn, studentId);
    String key = buildKey(fn, studentId);
    int current = usageCounters.getOrDefault(key, 0);
    return Math.max(0, limit - current);
  }

  public void setLimit(String function, String studentId, int limit) {
    customLimits.computeIfAbsent(function.toLowerCase(), k -> new ConcurrentHashMap<>())
        .put(studentId, limit);
  }

  public void exhaustQuota(String function, String studentId) {
    String fn = function.toLowerCase();
    int limit = resolveLimit(fn, studentId);
    usageCounters.put(buildKey(fn, studentId), limit);
  }

  public void resetAll() {
    usageCounters.clear();
    customLimits.clear();
  }

  private int resolveLimit(String function, String studentId) {
    Map<String, Integer> overrides = customLimits.get(function);
    if (overrides != null && overrides.containsKey(studentId)) {
      return overrides.get(studentId);
    }
    return defaultLimits.getOrDefault(function, 20);
  }

  private String buildKey(String function, String studentId) {
    return function + ":" + studentId + ":" + lastResetDate;
  }

  private void checkDayRollover() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    if (!today.equals(lastResetDate)) {
      usageCounters.clear();
      lastResetDate = today;
    }
  }

  private long calculateSecondsUntilMidnightUtc() {
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    ZonedDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(ZoneOffset.UTC);
    return Math.max(1, Duration.between(now, nextMidnight).toSeconds());
  }
}
