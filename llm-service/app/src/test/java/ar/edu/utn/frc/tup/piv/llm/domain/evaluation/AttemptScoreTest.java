package ar.edu.utn.frc.tup.piv.llm.domain.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AttemptScoreTest {
  private final Map<Dimension, Integer> weights = Map.of(Dimension.AUTONOMY, 30, Dimension.CLARITY, 25,
      Dimension.PROGRESSION, 20, Dimension.COMPLIANCE, 15, Dimension.EFFICIENCY, 10);

  @Test
  void aggregatesWithTheRubricWeightsAndRoundsHalfUp() {
    var score = AttemptScore.of(scores(80, 60, 40, 100, 20), weights);

    // 80*.30 + 60*.25 + 40*.20 + 100*.15 + 20*.10 = 24 + 15 + 8 + 15 + 2
    assertThat(score.overall()).isEqualTo(64);
    assertThat(score.dimensions()).containsEntry(Dimension.COMPLIANCE, 100);
  }

  @Test
  void equalScoresAggregateToThatScoreWhateverTheWeights() {
    assertThat(AttemptScore.of(scores(71, 71, 71, 71, 71), weights).overall()).isEqualTo(71);
  }

  @Test
  void rejectsScoresOutsideZeroToOneHundredOrMissing() {
    assertThatThrownBy(() -> AttemptScore.of(scores(101, 60, 40, 100, 20), weights))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("AUTONOMY");
    var missing = scores(80, 60, 40, 100, 20);
    missing.remove(Dimension.EFFICIENCY);
    assertThatThrownBy(() -> AttemptScore.of(missing, weights))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("EFFICIENCY");
  }

  @Test
  void rejectsWeightsThatDoNotTotalOneHundred() {
    var broken = new EnumMap<>(weights);
    broken.put(Dimension.EFFICIENCY, 20);

    assertThatThrownBy(() -> AttemptScore.of(scores(80, 60, 40, 100, 20), broken))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("100");
  }

  private static Map<Dimension, Integer> scores(int autonomy, int clarity, int progression, int compliance, int efficiency) {
    Map<Dimension, Integer> scores = new EnumMap<>(Dimension.class);
    scores.put(Dimension.AUTONOMY, autonomy);
    scores.put(Dimension.CLARITY, clarity);
    scores.put(Dimension.PROGRESSION, progression);
    scores.put(Dimension.COMPLIANCE, compliance);
    scores.put(Dimension.EFFICIENCY, efficiency);
    return scores;
  }
}
