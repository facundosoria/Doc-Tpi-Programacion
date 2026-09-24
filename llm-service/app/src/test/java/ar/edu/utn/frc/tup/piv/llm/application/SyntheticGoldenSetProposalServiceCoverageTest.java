package ar.edu.utn.frc.tup.piv.llm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SyntheticGoldenSetProposalServiceCoverageTest {
  private final SyntheticGoldenSetProposalService service = new SyntheticGoldenSetProposalService(new ObjectMapper());

  @Test void clampsCountToAtLeastOne() {
    assertThat(service.propose(UUID.randomUUID(), 0).items()).hasSize(1);
  }

  @Test void clampsCountToAtMostTen() {
    assertThat(service.propose(UUID.randomUUID(), 25).items()).hasSize(10);
    assertThat(service.propose(UUID.randomUUID(), 10).items()).hasSize(10);
  }

  @Test void generatesAllThreeSampleChallengeFamilies() {
    var items = service.propose(UUID.randomUUID(), 6).items();
    Set<String> ids = new HashSet<>();
    for (var item : items) {
      ids.add(item.challengeContext().path("externalChallengeId").asText());
      assertThat(item.transcript()).hasSize(3);
      assertThat(item.transcript().get(0).path("position").asInt()).isZero();
    }
    assertThat(ids).containsExactlyInAnyOrder("ch-rec-01", "ch-sort-02", "ch-hash-03");
  }
}