package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OutputAntiLeakGuardTest {
  private final OutputAntiLeakGuard guard = new OutputAntiLeakGuard();

  @Test
  void flagsALongCodeBlockAsALeak() {
    String longBlock = "```java\n" + "line;\n".repeat(9) + "```";
    assertThat(guard.containsLeak(longBlock, null)).isTrue();
  }

  @Test
  void doesNotFlagAShortCodeSnippet() {
    String shortBlock = "```java\nSystem.out.println(1);\n```";
    assertThat(guard.containsLeak(shortBlock, null)).isFalse();
  }

  @Test
  void flagsAResponseThatMatchesTheExpectedSolution() {
    assertThat(guard.containsLeak("la respuesta es usar un HashMap<String,Integer>", "usar un HashMap<String,Integer>")).isTrue();
  }

  @Test
  void doesNotFlagAPlainSocraticQuestion() {
    assertThat(guard.containsLeak("¿Qué pasaría si la lista está vacía?", null)).isFalse();
  }
}
