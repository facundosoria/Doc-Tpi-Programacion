package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ModelResponseSchemaCoverageTest {
  private final ModelResponseSchema schema = new ModelResponseSchema();

  @Test
  void acceptsAShortSocraticTutorAnswer() {
    assertThatCode(() -> schema.validate(ModelFunction.TUTOR, "pensá en el caso de la lista vacía"))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsFunctionsWithoutASchemaYet() {
    assertThatThrownBy(() -> schema.validate(ModelFunction.MODERATOR, "ok"))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("MODERATOR");
  }

  @Test
  void rejectsBlankOrNullText() {
    assertThatThrownBy(() -> schema.validate(ModelFunction.TUTOR, " "))
        .isInstanceOf(InvalidModelResponseException.class)
        .hasMessageContaining("vacía");
    assertThatThrownBy(() -> schema.validate(ModelFunction.TUTOR, null))
        .isInstanceOf(InvalidModelResponseException.class);
  }

  @Test
  void rejectsAnOversizedAnswer() {
    assertThatThrownBy(() -> schema.validate(ModelFunction.TUTOR, "x".repeat(4001)))
        .isInstanceOf(InvalidModelResponseException.class)
        .hasMessageContaining("límite");
  }
}