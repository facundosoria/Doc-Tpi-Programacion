package ar.edu.utn.frc.tup.piv.llm.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RealCaseAnonymizerTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void removesIdentifiersAndSanitizesPiiIrreversibly() {
    ObjectNode root = mapper.createObjectNode();
    root.put("studentId", "12345");
    root.put("student_name", "Juan Perez");
    root.put("userId", "usr-8899");
    root.put("attemptId", "att-7766");
    root.put("email", "juan.perez@alumnos.frc.utn.edu.ar");

    ArrayNode transcript = root.putArray("transcript");
    ObjectNode m1 = transcript.addObject();
    m1.put("role", "STUDENT");
    m1.put("content", "Hola tutor, soy Juan Perez (email: juan.perez@alumnos.frc.utn.edu.ar). Mi id es 550e8400-e29b-41d4-a716-446655440000 y tengo una duda en Java.");

    ObjectNode challenge = root.putObject("challengeContext");
    challenge.put("statement", "Implementar cola");

    var anonymized = RealCaseAnonymizer.anonymize(root);

    // Forbidden keys removed
    assertThat(anonymized.has("studentId")).isFalse();
    assertThat(anonymized.has("student_name")).isFalse();
    assertThat(anonymized.has("userId")).isFalse();
    assertThat(anonymized.has("attemptId")).isFalse();
    assertThat(anonymized.has("email")).isFalse();

    // Transcript text sanitized
    String transcriptText = anonymized.path("transcript").get(0).path("content").asText();
    assertThat(transcriptText).doesNotContain("juan.perez@alumnos.frc.utn.edu.ar");
    assertThat(transcriptText).doesNotContain("550e8400-e29b-41d4-a716-446655440000");
    assertThat(transcriptText).contains("[REDACTED_EMAIL]");
    assertThat(transcriptText).contains("[REDACTED_ID]");

    // Challenge context preserved
    assertThat(anonymized.path("challengeContext").path("statement").asText()).isEqualTo("Implementar cola");
  }
}
