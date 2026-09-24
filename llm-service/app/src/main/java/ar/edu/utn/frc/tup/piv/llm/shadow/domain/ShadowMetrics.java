package ar.edu.utn.frc.tup.piv.llm.shadow.domain;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Compara la evaluación de una rúbrica candidata contra la de la baseline (la activa del curso)
 * sobre las mismas transcripciones. Función pura: no toca base ni proveedor.
 *
 * <p>El puntaje final de cada versión usa los pesos de SU rúbrica, porque cambiar pesos es
 * justamente uno de los cambios que el shadow quiere detectar. La divergencia de un caso es
 * {@code candidata - baseline} (con signo); el sesgo promedio dice si la candidata puntúa más
 * alto (+) o más bajo (-). */
public final class ShadowMetrics {
  private static final int TOP_DIVERGENT = 5;

  private ShadowMetrics() {}

  /** Un caso ya evaluado con ambas versiones, o fallido ({@code errorCode != null}). {@code human}
   * es nulo cuando la fuente no tiene nota humana (conversaciones del tutor). */
  public record CaseOutcome(String sourceRef, Map<Dimension, Integer> baseline, Map<Dimension, Integer> candidate,
      Map<Dimension, Integer> human, String errorCode) {
    public boolean failed() {
      return errorCode != null;
    }
  }

  public record CaseDelta(String sourceRef, double baselineFinal, double candidateFinal, double delta) {}

  /** Umbrales de la recomendación. Son un consejo para el docente, no una decisión automática. */
  public record Criteria(double divergenceThreshold, double maxMae, double maxAbsBias, double maxFailedShare) {}

  public enum Recommendation { PROCEED_TO_CALIBRATION, REVIEW, INSUFFICIENT_DATA }

  public record Summary(int totalCases, int comparedCases, int failedCases, double mae, double bias,
      double maxDivergence, double shareOverThreshold, Double baselineMaeVsHuman, Double candidateMaeVsHuman,
      List<CaseDelta> mostDivergent, Recommendation recommendation) {}

  public static Summary summarize(List<CaseOutcome> outcomes, Map<Dimension, Integer> baselineWeights,
      Map<Dimension, Integer> candidateWeights, Criteria criteria) {
    List<CaseOutcome> compared = outcomes.stream().filter(o -> !o.failed()).toList();
    int failed = outcomes.size() - compared.size();
    if (compared.isEmpty()) {
      return new Summary(outcomes.size(), 0, failed, 0, 0, 0, 0, null, null, List.of(), Recommendation.INSUFFICIENT_DATA);
    }

    List<CaseDelta> deltas = compared.stream().map(o -> {
      double b = finalScore(o.baseline(), baselineWeights);
      double c = finalScore(o.candidate(), candidateWeights);
      return new CaseDelta(o.sourceRef(), round(b), round(c), round(c - b));
    }).toList();

    double mae = deltas.stream().mapToDouble(d -> Math.abs(d.delta())).average().orElse(0);
    double bias = deltas.stream().mapToDouble(CaseDelta::delta).average().orElse(0);
    double max = deltas.stream().mapToDouble(d -> Math.abs(d.delta())).max().orElse(0);
    double over = deltas.stream().filter(d -> Math.abs(d.delta()) > criteria.divergenceThreshold()).count()
        / (double) deltas.size();
    List<CaseDelta> top = deltas.stream()
        .sorted(Comparator.comparingDouble((CaseDelta d) -> Math.abs(d.delta())).reversed())
        .limit(TOP_DIVERGENT).toList();

    List<CaseOutcome> withHuman = compared.stream().filter(o -> o.human() != null).toList();
    Double baselineVsHuman = withHuman.isEmpty() ? null
        : round(errorVsHuman(withHuman, true, baselineWeights));
    Double candidateVsHuman = withHuman.isEmpty() ? null
        : round(errorVsHuman(withHuman, false, candidateWeights));

    double failedShare = failed / (double) outcomes.size();
    boolean ok = mae <= criteria.maxMae() && Math.abs(bias) <= criteria.maxAbsBias() && failedShare <= criteria.maxFailedShare();
    return new Summary(outcomes.size(), compared.size(), failed, round(mae), round(bias), round(max), round(over),
        baselineVsHuman, candidateVsHuman, top, ok ? Recommendation.PROCEED_TO_CALIBRATION : Recommendation.REVIEW);
  }

  private static double errorVsHuman(List<CaseOutcome> cases, boolean useBaseline, Map<Dimension, Integer> weights) {
    return cases.stream().mapToDouble(o -> Math.abs(
        finalScore(useBaseline ? o.baseline() : o.candidate(), weights) - finalScore(o.human(), weights))).average().orElse(0);
  }

  static double finalScore(Map<Dimension, Integer> scores, Map<Dimension, Integer> weights) {
    double total = 0;
    for (Dimension dimension : Dimension.values()) {
      total += scores.get(dimension) * weights.get(dimension) / 100.0;
    }
    return total;
  }

  private static double round(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
