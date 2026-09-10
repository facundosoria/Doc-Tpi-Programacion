package ar.edu.utn.frc.tup.piv.llm.domain;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension.AUTONOMY;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension.CLARITY;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension.COMPLIANCE;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension.EFFICIENCY;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension.PROGRESSION;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RubricValidatorTest {
  @Test void validateForPublication_shouldAcceptTheFiveDimensionsWithTotalWeightOneHundred() {
    assertThatCode(() -> RubricValidator.validateForPublication(validDimensions())).doesNotThrowAnyException();
  }

  @Test void validateForPublication_shouldRejectWeightsThatDoNotTotalOneHundred() {
    var dimensions = List.of(
        dimension(AUTONOMY, 30), dimension(CLARITY, 25), dimension(PROGRESSION, 20), dimension(COMPLIANCE, 15), dimension(EFFICIENCY, 9));
    assertThatThrownBy(() -> RubricValidator.validateForPublication(dimensions))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Rubric weights must total 100");
  }

  @Test void validateForPublication_shouldRejectADuplicateDimension() {
    var dimensions = List.of(
        dimension(AUTONOMY, 30), dimension(CLARITY, 25), dimension(PROGRESSION, 20), dimension(COMPLIANCE, 15), dimension(COMPLIANCE, 10));
    assertThatThrownBy(() -> RubricValidator.validateForPublication(dimensions))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("A rubric must contain each dimension exactly once");
  }

  private List<RubricValidator.DimensionDefinition> validDimensions() {
    return List.of(dimension(AUTONOMY, 30), dimension(CLARITY, 25), dimension(PROGRESSION, 20), dimension(COMPLIANCE, 15), dimension(EFFICIENCY, 10));
  }

  private RubricValidator.DimensionDefinition dimension(CalibrationMetrics.Dimension key, int weight) {
    return new RubricValidator.DimensionDefinition(key, BigDecimal.valueOf(weight));
  }
}
