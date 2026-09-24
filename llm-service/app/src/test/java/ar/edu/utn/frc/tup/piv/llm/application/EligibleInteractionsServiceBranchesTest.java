package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.service.EligibleInteractionsService;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EligibleInteractionsServiceBranchesTest {
  private final EligibleInteractionsService service = new EligibleInteractionsService(new ObjectMapper());
  private static final UUID INT_1 = UUID.fromString("e1111111-1111-1111-1111-111111111111");
  private static final UUID INT_2 = UUID.fromString("e2222222-2222-2222-2222-222222222222");

  @Test
  void unknownInteractionHasNoPreview() {
    assertThat(service.anonymizePreview(UUID.randomUUID(), UUID.randomUUID())).isEmpty();
  }

  @Test
  void secondInteractionPreviewIsAnonymizedWithoutEmail() {
    var preview = service.anonymizePreview(UUID.randomUUID(), INT_2);
    assertThat(preview).isPresent();
    String text = preview.get().toString();
    assertThat(text).doesNotContain("m.rodriguez@facultad.edu.ar");
    assertThat(text).contains("ch-tree-02");
  }

  @Test
  void firstInteractionPreviewHidesEmailAndPhone() {
    String text = service.anonymizePreview(UUID.randomUUID(), INT_1).orElseThrow().toString();
    assertThat(text).doesNotContain("l.gomez@alumno.frc.utn.edu.ar").doesNotContain("3514829102");
  }

  @Test
  void listReturnsBothSamplesWithStableIds() {
    var list = service.listEligible(UUID.randomUUID());
    assertThat(list).extracting(EligibleInteractionsService.EligibleInteractionSummary::id).containsExactly(INT_1, INT_2);
  }
}
