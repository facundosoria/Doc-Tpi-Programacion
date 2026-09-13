package ar.edu.utn.frc.tup.piv.llm.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension.AUTONOMY;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension.CLARITY;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension.COMPLIANCE;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension.EFFICIENCY;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension.PROGRESSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalibrationMetricsTest {
  private static final Map<CalibrationMetrics.Dimension, Integer> WEIGHTS = Map.of(AUTONOMY, 30, CLARITY, 25, PROGRESSION, 20, COMPLIANCE, 15, EFFICIENCY, 10);

  @Test void assess_shouldPass_whenMaeAndEveryIndividualErrorMeetPar14() {
    var result = CalibrationMetrics.assess(List.of(caseScores(80, 76)), WEIGHTS);
    assertThat(result.maeFinal()).isEqualByComparingTo(new BigDecimal("4.0000"));
    assertThat(result.maxIndividualError()).isEqualTo(4);
    assertThat(result.passed()).isTrue();
  }

  @Test void assess_shouldFail_whenMaeExceedsFiveEvenWithoutIndividualOutlier() {
    var result = CalibrationMetrics.assess(List.of(caseScores(80, 74)), WEIGHTS);
    assertThat(result.maeFinal()).isEqualByComparingTo(new BigDecimal("6.0000"));
    assertThat(result.maxIndividualError()).isEqualTo(6);
    assertThat(result.passed()).isFalse();
  }

  @Test void assess_shouldFail_whenOneDimensionExceedsTenEvenWithAcceptableMae() {
    var result = CalibrationMetrics.assess(List.of(new CalibrationMetrics.CaseScores(
        Map.of(AUTONOMY, 80, CLARITY, 80, PROGRESSION, 80, COMPLIANCE, 80, EFFICIENCY, 80),
        Map.of(AUTONOMY, 91, CLARITY, 80, PROGRESSION, 80, COMPLIANCE, 80, EFFICIENCY, 80))), WEIGHTS);
    assertThat(result.maeFinal()).isEqualByComparingTo(new BigDecimal("3.3000"));
    assertThat(result.maxIndividualError()).isEqualTo(11);
    assertThat(result.passed()).isFalse();
  }

  @Test void assess_shouldRejectWeightsThatDoNotTotalOneHundred() {
    assertThatThrownBy(() -> CalibrationMetrics.assess(List.of(caseScores(80, 80)), Map.of(AUTONOMY, 20, CLARITY, 20, PROGRESSION, 20, COMPLIANCE, 20, EFFICIENCY, 10)))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Weights must total 100");
  }

  private CalibrationMetrics.CaseScores caseScores(int human, int model) {
    return new CalibrationMetrics.CaseScores(Map.of(AUTONOMY, human, CLARITY, human, PROGRESSION, human, COMPLIANCE, human, EFFICIENCY, human), Map.of(AUTONOMY, model, CLARITY, model, PROGRESSION, model, COMPLIANCE, model, EFFICIENCY, model));
  }
}
