package ar.edu.utn.frc.tup.piv.llm.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CalibrationMetrics {
  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
  private static final BigDecimal MAX_MAE = BigDecimal.valueOf(5);
  private static final int MAX_INDIVIDUAL_ERROR = 10;

  private CalibrationMetrics() {}

  public static Result assess(List<CaseScores> cases, Map<Dimension, Integer> weights) {
    if (cases == null || cases.isEmpty()) throw new IllegalArgumentException("A calibration needs at least one case");
    validateWeights(weights);
    BigDecimal accumulatedFinalError = BigDecimal.ZERO;
    int maxIndividualError = 0;

    for (CaseScores scores : cases) {
      BigDecimal humanFinal = BigDecimal.ZERO;
      BigDecimal modelFinal = BigDecimal.ZERO;
      for (Dimension dimension : Dimension.values()) {
        int human = scores.humanScores().get(dimension);
        int model = scores.modelScores().get(dimension);
        int error = Math.abs(model - human);
        maxIndividualError = Math.max(maxIndividualError, error);
        BigDecimal factor = BigDecimal.valueOf(weights.get(dimension)).divide(ONE_HUNDRED);
        humanFinal = humanFinal.add(BigDecimal.valueOf(human).multiply(factor));
        modelFinal = modelFinal.add(BigDecimal.valueOf(model).multiply(factor));
      }
      accumulatedFinalError = accumulatedFinalError.add(humanFinal.subtract(modelFinal).abs());
    }

    BigDecimal maeFinal = accumulatedFinalError.divide(BigDecimal.valueOf(cases.size()), 4, RoundingMode.HALF_UP);
    return new Result(maeFinal, maxIndividualError, maeFinal.compareTo(MAX_MAE) <= 0 && maxIndividualError <= MAX_INDIVIDUAL_ERROR);
  }

  private static void validateWeights(Map<Dimension, Integer> weights) {
    if (weights == null || weights.size() != Dimension.values().length) throw new IllegalArgumentException("Weights must define the five dimensions");
    int total = 0;
    for (Dimension dimension : Dimension.values()) {
      Integer weight = weights.get(dimension);
      if (weight == null || weight < 0 || weight > 100) throw new IllegalArgumentException("Invalid weight for " + dimension);
      total += weight;
    }
    if (total != 100) throw new IllegalArgumentException("Weights must total 100");
  }

  public enum Dimension { AUTONOMY, CLARITY, PROGRESSION, COMPLIANCE, EFFICIENCY }

  public record CaseScores(Map<Dimension, Integer> humanScores, Map<Dimension, Integer> modelScores) {
    public CaseScores {
      humanScores = immutableScores(humanScores, "human");
      modelScores = immutableScores(modelScores, "model");
    }

    private static Map<Dimension, Integer> immutableScores(Map<Dimension, Integer> scores, String label) {
      if (scores == null || scores.size() != Dimension.values().length) throw new IllegalArgumentException(label + " scores must define the five dimensions");
      Map<Dimension, Integer> copy = new EnumMap<>(Dimension.class);
      for (Dimension dimension : Dimension.values()) {
        Integer score = scores.get(dimension);
        if (score == null || score < 0 || score > 100) throw new IllegalArgumentException("Invalid " + label + " score for " + dimension);
        copy.put(dimension, score);
      }
      return Map.copyOf(copy);
    }
  }

  public record Result(BigDecimal maeFinal, int maxIndividualError, boolean passed) {}
}
