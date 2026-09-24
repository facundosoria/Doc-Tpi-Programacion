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

  @Test void validateForPublication_shouldRejectNullOrWrongNumberOfDimensions() {
    assertThatThrownBy(() -> RubricValidator.validateForPublication(null))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("A rubric must contain exactly five dimensions");
    assertThatThrownBy(() -> RubricValidator.validateForPublication(List.of(dimension(AUTONOMY, 50), dimension(CLARITY, 50))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("A rubric must contain exactly five dimensions");
  }

  @Test void validateForPublication_shouldRejectANullDimension() {
    var dimensions = java.util.Arrays.<RubricValidator.DimensionDefinition>asList(
        dimension(AUTONOMY, 30), null, dimension(PROGRESSION, 20), dimension(COMPLIANCE, 15), dimension(EFFICIENCY, 10));
    assertThatThrownBy(() -> RubricValidator.validateForPublication(dimensions))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("A rubric must contain each dimension exactly once");
  }

  @Test void validateForPublication_shouldRejectWeightsOutOfRange() {
    var over = List.of(dimension(AUTONOMY, 30), dimension(CLARITY, 25), dimension(PROGRESSION, 20), dimension(COMPLIANCE, 15), dimension(EFFICIENCY, 110));
    assertThatThrownBy(() -> RubricValidator.validateForPublication(over))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Each dimension weight must be between 0 and 100");
    var zero = List.of(dimension(AUTONOMY, 30), dimension(CLARITY, 25), dimension(PROGRESSION, 20), dimension(COMPLIANCE, 0), dimension(EFFICIENCY, 25));
    assertThatThrownBy(() -> RubricValidator.validateForPublication(zero))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Each dimension weight must be between 0 and 100");
    var negative = List.of(dimension(AUTONOMY, -5), dimension(CLARITY, 25), dimension(PROGRESSION, 20), dimension(COMPLIANCE, 15), dimension(EFFICIENCY, 45));
    assertThatThrownBy(() -> RubricValidator.validateForPublication(negative))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Each dimension weight must be between 0 and 100");
    var nullWeight = List.of(dimension(AUTONOMY, 30), dimension(CLARITY, 25), dimension(PROGRESSION, 20), dimension(COMPLIANCE, 15), new RubricValidator.DimensionDefinition(EFFICIENCY, null));
    assertThatThrownBy(() -> RubricValidator.validateForPublication(nullWeight))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Each dimension weight must be between 0 and 100");
  }

  @Test void validateModularRubric_shouldAcceptOneDimensionTotallingOneHundred() {
    assertThatCode(() -> RubricValidator.validateModularRubric(List.of(custom("algoritmos", 100))))
        .doesNotThrowAnyException();
  }

  @Test void validateModularRubric_shouldAcceptNArbitraryDimensionsTotallingOneHundred() {
    assertThatCode(() -> RubricValidator.validateModularRubric(List.of(
        custom("algoritmos", 35), custom("modularidad", 25), custom("pruebas", 20), custom("autonomia", 20))))
        .doesNotThrowAnyException();
  }

  @Test void validateModularRubric_shouldRejectSumsThatAreNotExactlyOneHundred() {
    assertThatThrownBy(() -> RubricValidator.validateModularRubric(List.of(custom("algoritmos", 99.99))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Rubric weights must total 100");
    assertThatThrownBy(() -> RubricValidator.validateModularRubric(List.of(custom("a", 60), custom("b", 40.01))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Rubric weights must total 100");
    assertThatThrownBy(() -> RubricValidator.validateModularRubric(List.of(custom("a", 95), custom("b", 5.01))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Rubric weights must total 100");
  }

  @Test void validateModularRubric_shouldRejectEmptyOrNullCollections() {
    assertThatThrownBy(() -> RubricValidator.validateModularRubric(null))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("A modular rubric must contain at least one dimension");
    assertThatThrownBy(() -> RubricValidator.validateModularRubric(List.of()))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("A modular rubric must contain at least one dimension");
  }

  @Test void validateModularRubric_shouldRejectDuplicateOrBlankKeys() {
    assertThatThrownBy(() -> RubricValidator.validateModularRubric(List.of(custom("algoritmos", 50), custom("algoritmos", 50))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Each modular dimension must have a unique key");
    assertThatThrownBy(() -> RubricValidator.validateModularRubric(List.of(custom(" ", 100))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Each modular dimension must have a unique key");
    assertThatThrownBy(() -> RubricValidator.validateModularRubric(List.of(new RubricValidator.DimensionCustomDefinition(null, BigDecimal.valueOf(100)))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Each modular dimension must have a unique key");
  }

  @Test void validateModularRubric_shouldRejectWeightsOutOfRange() {
    assertThatThrownBy(() -> RubricValidator.validateModularRubric(List.of(custom("a", 0))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Each dimension weight must be between 0 and 100");
    assertThatThrownBy(() -> RubricValidator.validateModularRubric(List.of(custom("a", -5))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Each dimension weight must be between 0 and 100");
    assertThatThrownBy(() -> RubricValidator.validateModularRubric(List.of(new RubricValidator.DimensionCustomDefinition("a", null))))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Each dimension weight must be between 0 and 100");
  }

  private RubricValidator.DimensionCustomDefinition custom(String key, double weight) {
    return new RubricValidator.DimensionCustomDefinition(key, BigDecimal.valueOf(weight));
  }

  private List<RubricValidator.DimensionDefinition> validDimensions() {
    return List.of(dimension(AUTONOMY, 30), dimension(CLARITY, 25), dimension(PROGRESSION, 20), dimension(COMPLIANCE, 15), dimension(EFFICIENCY, 10));
  }

  private RubricValidator.DimensionDefinition dimension(CalibrationMetrics.Dimension key, int weight) {
    return new RubricValidator.DimensionDefinition(key, BigDecimal.valueOf(weight));
  }
}