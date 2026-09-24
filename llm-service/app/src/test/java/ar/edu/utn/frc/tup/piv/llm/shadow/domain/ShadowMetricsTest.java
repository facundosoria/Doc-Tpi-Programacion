package ar.edu.utn.frc.tup.piv.llm.shadow.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowMetrics.CaseOutcome;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowMetrics.Criteria;
import ar.edu.utn.frc.tup.piv.llm.shadow.domain.ShadowMetrics.Recommendation;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShadowMetricsTest {
  private static final Map<Dimension, Integer> WEIGHTS = weights(30, 25, 20, 15, 10);
  private static final Criteria CRITERIA = new Criteria(10, 5, 3, 0.2);

  @Test
  void identicalRubricsDoNotDiverge() {
    var summary = ShadowMetrics.summarize(List.of(ok("a", 70, 70), ok("b", 40, 40)), WEIGHTS, WEIGHTS, CRITERIA);

    assertThat(summary.comparedCases()).isEqualTo(2);
    assertThat(summary.mae()).isZero();
    assertThat(summary.bias()).isZero();
    assertThat(summary.shareOverThreshold()).isZero();
    assertThat(summary.recommendation()).isEqualTo(Recommendation.PROCEED_TO_CALIBRATION);
  }

  @Test
  void biasCarriesTheSignOfCandidateMinusBaseline() {
    var higher = ShadowMetrics.summarize(List.of(ok("a", 60, 68)), WEIGHTS, WEIGHTS, CRITERIA);
    var lower = ShadowMetrics.summarize(List.of(ok("a", 60, 52)), WEIGHTS, WEIGHTS, CRITERIA);

    assertThat(higher.bias()).isEqualTo(8.0);
    assertThat(lower.bias()).isEqualTo(-8.0);
    assertThat(higher.mae()).isEqualTo(lower.mae());
  }

  @Test
  void aLargeButSymmetricDriftStillFailsTheMaeGate() {
    // +8 y -8: el sesgo se cancela pero la candidata puntúa distinto en cada caso.
    var summary = ShadowMetrics.summarize(List.of(ok("a", 60, 68), ok("b", 60, 52)), WEIGHTS, WEIGHTS, CRITERIA);

    assertThat(summary.bias()).isZero();
    assertThat(summary.mae()).isEqualTo(8.0);
    assertThat(summary.recommendation()).isEqualTo(Recommendation.REVIEW);
  }

  @Test
  void eachVersionIsWeightedWithItsOwnWeights() {
    Map<Dimension, Integer> onlyAutonomy = weights(100, 0, 0, 0, 0);
    Map<Dimension, Integer> onlyClarity = weights(0, 100, 0, 0, 0);
    var scores = scores(100, 0, 0, 0, 0);

    var summary = ShadowMetrics.summarize(List.of(new CaseOutcome("a", scores, scores, null, null)),
        onlyAutonomy, onlyClarity, CRITERIA);

    assertThat(summary.mostDivergent().get(0).baselineFinal()).isEqualTo(100.0);
    assertThat(summary.mostDivergent().get(0).candidateFinal()).isZero();
    assertThat(summary.bias()).isEqualTo(-100.0);
  }

  @Test
  void failedCasesAreCountedButExcludedFromTheStatistics() {
    var summary = ShadowMetrics.summarize(List.of(ok("a", 70, 70), failed("b"), ok("c", 70, 70), ok("d", 70, 70),
        ok("e", 70, 70), ok("f", 70, 70)), WEIGHTS, WEIGHTS, CRITERIA);

    assertThat(summary.totalCases()).isEqualTo(6);
    assertThat(summary.comparedCases()).isEqualTo(5);
    assertThat(summary.failedCases()).isEqualTo(1);
    assertThat(summary.mae()).isZero();
    assertThat(summary.recommendation()).as("1/6 fallidos ≈ 17% ≤ 20%").isEqualTo(Recommendation.PROCEED_TO_CALIBRATION);
  }

  @Test
  void tooManyFailuresBlockTheRecommendationEvenIfTheRestAgrees() {
    var summary = ShadowMetrics.summarize(List.of(ok("a", 70, 70), failed("b")), WEIGHTS, WEIGHTS, CRITERIA);

    assertThat(summary.recommendation()).isEqualTo(Recommendation.REVIEW);
  }

  @Test
  void withoutAnyComparableCaseThereIsNothingToRecommend() {
    var summary = ShadowMetrics.summarize(List.of(failed("a"), failed("b")), WEIGHTS, WEIGHTS, CRITERIA);

    assertThat(summary.comparedCases()).isZero();
    assertThat(summary.recommendation()).isEqualTo(Recommendation.INSUFFICIENT_DATA);
    assertThat(summary.baselineMaeVsHuman()).isNull();
  }

  @Test
  void measuresEachVersionAgainstTheHumanScoreWhenThereIsOne() {
    var withHuman = new CaseOutcome("a", scores(70, 70, 70, 70, 70), scores(90, 90, 90, 90, 90), scores(72, 72, 72, 72, 72), null);
    var withoutHuman = ok("b", 50, 50);

    var summary = ShadowMetrics.summarize(List.of(withHuman, withoutHuman), WEIGHTS, WEIGHTS, CRITERIA);

    assertThat(summary.baselineMaeVsHuman()).isEqualTo(2.0);
    assertThat(summary.candidateMaeVsHuman()).isEqualTo(18.0);
  }

  @Test
  void sourcesWithoutHumanScoresLeaveTheHumanMetricsEmpty() {
    var summary = ShadowMetrics.summarize(List.of(ok("a", 70, 70)), WEIGHTS, WEIGHTS, CRITERIA);

    assertThat(summary.baselineMaeVsHuman()).isNull();
    assertThat(summary.candidateMaeVsHuman()).isNull();
  }

  @Test
  void shareOverThresholdAndTopDivergentAreOrderedByAbsoluteDivergence() {
    var outcomes = List.of(ok("c1", 50, 51), ok("c2", 50, 65), ok("c3", 50, 30), ok("c4", 50, 50),
        ok("c5", 50, 56), ok("c6", 50, 71), ok("c7", 50, 50));

    var summary = ShadowMetrics.summarize(outcomes, WEIGHTS, WEIGHTS, CRITERIA);

    assertThat(summary.mostDivergent()).hasSize(5);
    assertThat(summary.mostDivergent()).extracting(ShadowMetrics.CaseDelta::sourceRef)
        .containsExactly("c6", "c3", "c2", "c5", "c1");
    assertThat(summary.shareOverThreshold()).as("c2 (15), c3 (20) y c6 (21) superan 10 de 7 casos").isEqualTo(0.43);
    assertThat(summary.maxDivergence()).isEqualTo(21.0);
  }

  private static CaseOutcome ok(String ref, int baseline, int candidate) {
    return new CaseOutcome(ref, scores(baseline, baseline, baseline, baseline, baseline),
        scores(candidate, candidate, candidate, candidate, candidate), null, null);
  }

  private static CaseOutcome failed(String ref) {
    return new CaseOutcome(ref, null, null, null, "EVALUATION_FAILED");
  }

  private static Map<Dimension, Integer> scores(int a, int c, int p, int co, int e) {
    return weights(a, c, p, co, e);
  }

  private static Map<Dimension, Integer> weights(int a, int c, int p, int co, int e) {
    Map<Dimension, Integer> map = new EnumMap<>(Dimension.class);
    map.put(Dimension.AUTONOMY, a);
    map.put(Dimension.CLARITY, c);
    map.put(Dimension.PROGRESSION, p);
    map.put(Dimension.COMPLIANCE, co);
    map.put(Dimension.EFFICIENCY, e);
    return map;
  }
}
