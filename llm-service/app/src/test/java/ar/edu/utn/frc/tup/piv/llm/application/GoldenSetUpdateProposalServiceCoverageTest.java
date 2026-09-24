package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.GoldenSetUpdateProposalRepository;
import ar.edu.utn.frc.tup.piv.llm.domain.goldenset.GoldenSetUpdateProposal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoldenSetUpdateProposalServiceCoverageTest {
  @Test void proposesForPublishedBaseAndExpiresThatFamilyRuns() {
    var proposals = mock(GoldenSetUpdateProposalRepository.class);
    var jdbc = mock(JdbcTemplate.class);
    var expirations = mock(CalibrationExpirationService.class);
    var service = new GoldenSetUpdateProposalService(proposals, jdbc, expirations);
    UUID base = UUID.randomUUID(), family = UUID.randomUUID();
    when(jdbc.queryForObject(anyString(), eq(UUID.class), eq(base))).thenReturn(family);
    when(proposals.createForPublishedBase(base)).thenReturn(2);
    assertThat(service.proposeForPublishedBase(base)).isEqualTo(2);
    verify(expirations).expireByGoldenSet(family);
  }

  @Test void listsPendingProposalsForACourse() {
    var proposals = mock(GoldenSetUpdateProposalRepository.class);
    var service = new GoldenSetUpdateProposalService(proposals, mock(JdbcTemplate.class), mock(CalibrationExpirationService.class));
    UUID course = UUID.randomUUID();
    var proposal = new GoldenSetUpdateProposal(UUID.randomUUID(), course, UUID.randomUUID(), UUID.randomUUID(), 3, 5, OffsetDateTime.now());
    when(proposals.pendingForCourse(course)).thenReturn(List.of(proposal));
    assertThat(service.pendingForCourse(course)).containsExactly(proposal);
  }
}