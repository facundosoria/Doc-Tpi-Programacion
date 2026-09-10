package ar.edu.utn.frc.tup.piv.llm.domain;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumSet;

public final class RubricValidator {
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  private RubricValidator() {}

  public static void validateForPublication(Collection<DimensionDefinition> dimensions) {
    if (dimensions == null || dimensions.size() != CalibrationMetrics.Dimension.values().length) {
      throw new IllegalArgumentException("A rubric must contain exactly five dimensions");
    }
    var keys = EnumSet.noneOf(CalibrationMetrics.Dimension.class);
    BigDecimal total = BigDecimal.ZERO;
    for (DimensionDefinition dimension : dimensions) {
      if (dimension == null || !keys.add(dimension.key())) {
        throw new IllegalArgumentException("A rubric must contain each dimension exactly once");
      }
      if (dimension.weight() == null || dimension.weight().signum() <= 0 || dimension.weight().compareTo(ONE_HUNDRED) > 0) {
        throw new IllegalArgumentException("Each dimension weight must be between 0 and 100");
      }
      total = total.add(dimension.weight());
    }
    if (!keys.equals(EnumSet.allOf(CalibrationMetrics.Dimension.class))) {
      throw new IllegalArgumentException("A rubric must contain the mandatory dimensions");
    }
    if (total.compareTo(ONE_HUNDRED) != 0) {
      throw new IllegalArgumentException("Rubric weights must total 100");
    }
  }

  public record DimensionDefinition(CalibrationMetrics.Dimension key, BigDecimal weight) {}
}
