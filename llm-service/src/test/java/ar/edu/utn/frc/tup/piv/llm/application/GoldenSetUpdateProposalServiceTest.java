package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.GoldenSetUpdateProposalRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoldenSetUpdateProposalServiceTest {
  @Test void proposesUpdatesAfterABaseVersionIsPublished() {
    var repository = mock(GoldenSetUpdateProposalRepository.class); var service = new GoldenSetUpdateProposalService(repository);
    UUID baseVersion = UUID.randomUUID(); when(repository.createForPublishedBase(baseVersion)).thenReturn(3);
    assertThat(service.proposeForPublishedBase(baseVersion)).isEqualTo(3);
    verify(repository).createForPublishedBase(baseVersion);
  }
}
