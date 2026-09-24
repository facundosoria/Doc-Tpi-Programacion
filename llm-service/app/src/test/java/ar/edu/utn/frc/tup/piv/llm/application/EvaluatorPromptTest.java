package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.service.EvaluatorPrompt;
import ar.edu.utn.frc.tup.piv.llm.application.service.RubricDraftService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvaluatorPromptTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void readsScoresWithLowerCaseKeysFromTheModelAndUpperCaseKeysFromTheGoldenSet() throws Exception {
    var fromModel = EvaluatorPrompt.scoresFrom(mapper.readTree(
        "{\"autonomy\":1,\"clarity\":2,\"progression\":3,\"compliance\":4,\"efficiency\":5}"));
    var fromGoldenSet = EvaluatorPrompt.scoresFrom(mapper.readTree(
        "{\"AUTONOMY\":1,\"CLARITY\":2,\"PROGRESSION\":3,\"COMPLIANCE\":4,\"EFFICIENCY\":5}"));

    assertThat(fromGoldenSet).isEqualTo(fromModel);
    assertThat(fromModel.get(Dimension.EFFICIENCY)).isEqualTo(5);
  }

  @Test
  void rejectsMissingOrNonIntegerDimensions() throws Exception {
    assertThatThrownBy(() -> EvaluatorPrompt.scoresFrom(mapper.readTree("{\"autonomy\":1}")))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("CLARITY");
    assertThatThrownBy(() -> EvaluatorPrompt.scoresFrom(mapper.readTree(
        "{\"autonomy\":1.5,\"clarity\":2,\"progression\":3,\"compliance\":4,\"efficiency\":5}")))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("AUTONOMY");
  }

  @Test
  void rendersTheRubricIntoThePromptAndTheIntegerWeights() {
    var rendered = EvaluatorPrompt.render(List.of(new RubricDraftService.DimensionInput(Dimension.CLARITY, "Claridad",
        "explica con claridad", null, BigDecimal.valueOf(25))));

    assertThat(rendered.weights()).containsEntry(Dimension.CLARITY, 25);
    assertThat(rendered.systemPrompt()).contains("- clarity (peso 25): explica con claridad");
  }
}
