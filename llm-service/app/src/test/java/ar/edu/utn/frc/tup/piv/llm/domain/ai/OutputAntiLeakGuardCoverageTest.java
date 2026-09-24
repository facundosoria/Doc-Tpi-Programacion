package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OutputAntiLeakGuardCoverageTest {
  private final OutputAntiLeakGuard guard = new OutputAntiLeakGuard();

  @Test
  void nullOrBlankResponsesAreNeverLeaks() {
    assertThat(guard.containsLeak(null, null)).isFalse();
    assertThat(guard.containsLeak("   ", "clave")).isFalse();
  }

  @Test
  void flagsInlineCodeSnippets() {
    assertThat(guard.containsLeak("seguí la intención de `hashCode()`", null)).isTrue();
  }

  @Test
  void allowsAConceptualBlockThatFitsTheLimit() {
    String conceptual = "```\nExplicación conceptual.\n```";
    assertThat(guard.containsLeak(conceptual, null)).isFalse();
  }

  @Test
  void matchesTheExpectedSolutionNormalizingCaseAndWhitespace() {
    assertThat(guard.containsLeak("la clave es\n  USAR UN HASHMAP", "usar un hashmap")).isTrue();
  }
}