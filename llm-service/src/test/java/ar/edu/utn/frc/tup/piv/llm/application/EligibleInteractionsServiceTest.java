package ar.edu.utn.frc.tup.piv.llm.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EligibleInteractionsServiceTest {
  private final EligibleInteractionsService service = new EligibleInteractionsService(new ObjectMapper());

  @Test void listsEligibleInteractionsForCourse() {
    var items = service.listEligible(UUID.randomUUID());
    assertThat(items).hasSize(2);
    assertThat(items.getFirst().externalChallengeId()).isEqualTo("ch-pila-01");
  }

  @Test void anonymizesPreviewRedactingEmailsAndPhone() {
    UUID int1 = UUID.fromString("e1111111-1111-1111-1111-111111111111");
    var preview = service.anonymizePreview(UUID.randomUUID(), int1);

    assertThat(preview).isPresent();
    var node = preview.get();
    assertThat(node.path("author").asText()).isEqualTo("Interacción real anonimizada");
    String text = node.toString();
    assertThat(text)
        .doesNotContain("l.gomez@alumno.frc.utn.edu.ar")
        .doesNotContain("3514829102")
        .doesNotContain("88321") // student_id removed
        .contains("[REDACTED_EMAIL]")
        .contains("[REDACTED_PHONE]");
  }
}
