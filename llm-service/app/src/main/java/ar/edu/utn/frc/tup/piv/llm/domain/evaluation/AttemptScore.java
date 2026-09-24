package ar.edu.utn.frc.tup.piv.llm.domain.evaluation;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/** El score de un intento: el puntaje por dimensión que dio el evaluador y el agregado 0-100.
 * El agregado lo calcula el código con los pesos fijos de la rúbrica, no el modelo (RF-IA-15). */
public record AttemptScore(int overall, Map<Dimension, Integer> dimensions) {
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  public AttemptScore {
    dimensions = Map.copyOf(dimensions);
  }

  public static AttemptScore of(Map<Dimension, Integer> scores, Map<Dimension, Integer> weights) {
    Map<Dimension, Integer> checked = new EnumMap<>(Dimension.class);
    BigDecimal weighted = BigDecimal.ZERO;
    int totalWeight = 0;
    for (Dimension dimension : Dimension.values()) {
      Integer score = scores.get(dimension);
      Integer weight = weights.get(dimension);
      if (score == null || score < 0 || score > 100) {
        throw new IllegalArgumentException("Puntaje inválido para " + dimension);
      }
      if (weight == null || weight < 0 || weight > 100) {
        throw new IllegalArgumentException("Peso inválido para " + dimension);
      }
      checked.put(dimension, score);
      totalWeight += weight;
      weighted = weighted.add(BigDecimal.valueOf((long) score * weight));
    }
    if (totalWeight != 100) {
      throw new IllegalArgumentException("Los pesos deben sumar 100");
    }
    return new AttemptScore(weighted.divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP).intValueExact(), checked);
  }
}
