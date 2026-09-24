package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonObjectExtractorTest {
  private static final String SCORES = "{\"autonomy\":80,\"clarity\":70,\"progression\":60,\"compliance\":90,\"efficiency\":50}";

  @Test
  void aBareJsonObjectIsReturnedUnchanged() {
    assertThat(JsonObjectExtractor.extract(SCORES)).isEqualTo(SCORES);
  }

  @Test
  void stripsMarkdownCodeFencesWithAndWithoutLanguage() {
    assertThat(JsonObjectExtractor.extract("```json\n" + SCORES + "\n```")).isEqualTo(SCORES);
    assertThat(JsonObjectExtractor.extract("```\n" + SCORES + "\n```")).isEqualTo(SCORES);
  }

  @Test
  void stripsProseBeforeAndAfterTheObject() {
    assertThat(JsonObjectExtractor.extract("Claro, acá va la evaluación:\n" + SCORES + "\nEspero que sirva."))
        .isEqualTo(SCORES);
  }

  @Test
  void bracesInsideStringsDoNotBreakTheMatch() {
    String withBraces = "{\"nota\":\"usó { y } sin escapar \\\" nada\",\"autonomy\":1}";

    assertThat(JsonObjectExtractor.extract("texto " + withBraces + " fin")).isEqualTo(withBraces);
  }

  @Test
  void nestedObjectsAreKeptWhole() {
    String nested = "{\"a\":{\"b\":1},\"c\":2}";

    assertThat(JsonObjectExtractor.extract("```json\n" + nested + "\n```")).isEqualTo(nested);
  }

  @Test
  void skipsAStrayOpeningBraceAndTakesTheFirstBalancedObject() {
    assertThat(JsonObjectExtractor.extract("un { suelto y luego " + SCORES)).isEqualTo(SCORES);
  }

  @Test
  void textWithoutABalancedObjectComesBackUntouchedSoValidationStillFails() {
    assertThat(JsonObjectExtractor.extract("no hay json acá")).isEqualTo("no hay json acá");
    assertThat(JsonObjectExtractor.extract("{\"autonomy\":80")).isEqualTo("{\"autonomy\":80");
  }

  @Test
  void nullAndBlankPassThrough() {
    assertThat(JsonObjectExtractor.extract(null)).isNull();
    assertThat(JsonObjectExtractor.extract("  ")).isEqualTo("  ");
  }
}
