package ar.edu.utn.frc.tup.piv.llm.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SyntheticGoldenSetProposalServiceTest {
  private final SyntheticGoldenSetProposalService service = new SyntheticGoldenSetProposalService(new ObjectMapper());

  @Test
  void proposesSyntheticCasesMarkedDraftWithoutFinalHumanScores() {
    UUID courseId = UUID.randomUUID();
    var result = service.propose(courseId, 3);

    assertThat(result.state()).isEqualTo("COMPLETED");
    assertThat(result.items()).hasSize(3);

    for (var item : result.items()) {
      assertThat(item.author()).isEqualTo("Generador Asistido / LLM");
      assertThat(item.reviewState()).isEqualTo("DRAFT");
      assertThat(item.transcript().isArray()).isTrue();
      assertThat(item.transcript().size()).isGreaterThanOrEqualTo(2);
      assertThat(item.challengeContext().path("statement").asText()).isNotBlank();
    }
  }
}
