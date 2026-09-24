package ar.edu.utn.frc.tup.piv.llm.domain;

import java.math.BigDecimal;
import java.util.HashMap;
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

@Test void assess_shouldRejectNullOrEmptyCases() {
    assertThatThrownBy(() -> CalibrationMetrics.assess(null, WEIGHTS))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("A calibration needs at least one case");
    assertThatThrownBy(() -> CalibrationMetrics.assess(List.of(), WEIGHTS))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("A calibration needs at least one case");
  }

  @Test void assessDynamic_evaluatesCustomDimensionsCorrectly() {
    var human = List.of(Map.of("code_quality", 80, "test_runner", 90));
    var model = List.of(Map.of("code_quality", 75, "test_runner", 85));
    var weights = Map.of("code_quality", 60, "test_runner", 40);

    var result = CalibrationMetrics.assessDynamic(human, model, weights);
    // humanFinal = 80*0.6 + 90*0.4 = 48 + 36 = 84
    // modelFinal = 75*0.6 + 85*0.4 = 45 + 34 = 79
    // error = |84 - 79| = 5.0000, maxIndividualError = 5
    assertThat(result.maeFinal()).isEqualByComparingTo(new BigDecimal("5.0000"));
    assertThat(result.maxIndividualError()).isEqualTo(5);
    assertThat(result.passed()).isTrue();
  }

  @Test void assess_shouldRejectNullOrIncompleteWeights() {
    assertThatThrownBy(() -> CalibrationMetrics.assess(List.of(caseScores(80, 80)), null))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Weights must define the five dimensions");
    assertThatThrownBy(() -> CalibrationMetrics.assess(List.of(caseScores(80, 80)), Map.of(AUTONOMY, 40, CLARITY, 40)))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Weights must define the five dimensions");
  }

  @Test void assess_shouldRejectInvalidWeightsPerDimension() {
    var negative = Map.of(AUTONOMY, -1, CLARITY, 25, PROGRESSION, 20, COMPLIANCE, 15, EFFICIENCY, 10);
    assertThatThrownBy(() -> CalibrationMetrics.assess(List.of(caseScores(80, 80)), negative))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid weight for AUTONOMY");
    var over = Map.of(AUTONOMY, 30, CLARITY, 25, PROGRESSION, 20, COMPLIANCE, 150, EFFICIENCY, 10);
    assertThatThrownBy(() -> CalibrationMetrics.assess(List.of(caseScores(80, 80)), over))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid weight for COMPLIANCE");
    Map<CalibrationMetrics.Dimension, Integer> missing = new HashMap<>(WEIGHTS);
    missing.put(EFFICIENCY, null);
    assertThatThrownBy(() -> CalibrationMetrics.assess(List.of(caseScores(80, 80)), missing))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid weight for EFFICIENCY");
  }

  @Test void caseScores_shouldRejectNullOrIncompleteHumanScores() {
    assertThatThrownBy(() -> new CalibrationMetrics.CaseScores(null, validModel()))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("human scores must define the five dimensions");
    assertThatThrownBy(() -> new CalibrationMetrics.CaseScores(Map.of(AUTONOMY, 80), validModel()))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("human scores must define the five dimensions");
  }

  @Test void caseScores_shouldRejectInvalidHumanScores() {
    Map<CalibrationMetrics.Dimension, Integer> over = new HashMap<>(validHuman());
    over.put(EFFICIENCY, 101);
    assertThatThrownBy(() -> new CalibrationMetrics.CaseScores(over, validModel()))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid human score for EFFICIENCY");
    Map<CalibrationMetrics.Dimension, Integer> missing = new HashMap<>(validHuman());
    missing.put(COMPLIANCE, null);
    assertThatThrownBy(() -> new CalibrationMetrics.CaseScores(missing, validModel()))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid human score for COMPLIANCE");
  }

  @Test void caseScores_shouldRejectInvalidModelScores() {
    assertThatThrownBy(() -> new CalibrationMetrics.CaseScores(validHuman(), null))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("model scores must define the five dimensions");
    assertThatThrownBy(() -> new CalibrationMetrics.CaseScores(validHuman(), Map.of(AUTONOMY, 80)))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("model scores must define the five dimensions");
    Map<CalibrationMetrics.Dimension, Integer> negative = new HashMap<>(validModel());
    negative.put(AUTONOMY, -5);
    assertThatThrownBy(() -> new CalibrationMetrics.CaseScores(validHuman(), negative))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid model score for AUTONOMY");
  }

  private CalibrationMetrics.CaseScores caseScores(int human, int model) {
    return new CalibrationMetrics.CaseScores(validHuman(human), validModel(model));
  }

  private static Map<CalibrationMetrics.Dimension, Integer> validHuman() { return validHuman(80); }
  private static Map<CalibrationMetrics.Dimension, Integer> validModel() { return validModel(80); }
  private static Map<CalibrationMetrics.Dimension, Integer> validHuman(int value) {
    return Map.of(AUTONOMY, value, CLARITY, value, PROGRESSION, value, COMPLIANCE, value, EFFICIENCY, value);
  }
  private static Map<CalibrationMetrics.Dimension, Integer> validModel(int value) {
    return Map.of(AUTONOMY, value, CLARITY, value, PROGRESSION, value, COMPLIANCE, value, EFFICIENCY, value);
  }
}