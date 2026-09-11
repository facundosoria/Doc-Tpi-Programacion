package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.RubricDraftService.RubricVersion;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.RubricVersionRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RubricVersioningServiceTest {
  @Test void createsASeparateDraftFromAPublishedVersion() {
    var repository = mock(RubricVersionRepository.class); var service = new RubricDraftService(repository);
    UUID course = UUID.randomUUID(), published = UUID.randomUUID(), draft = UUID.randomUUID(), family = UUID.randomUUID(), user = UUID.randomUUID();
    RubricVersion newDraft = new RubricVersion(draft, family, 2, "Rúbrica", "DRAFT", 1, null, List.of());
    when(repository.createNextDraft(course, published, user)).thenReturn(Optional.of(newDraft));

    assertThat(service.createNextVersion(course, published, new CallerIdentity("gateway", user, null, null))).isEqualTo(newDraft);
    verify(repository).createNextDraft(course, published, user);
  }

  @Test void refusesToCreateANewVersionFromAnythingOtherThanAPublishedVersion() {
    var repository = mock(RubricVersionRepository.class); UUID course = UUID.randomUUID(), source = UUID.randomUUID(), user = UUID.randomUUID();
    when(repository.createNextDraft(course, source, user)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> new RubricDraftService(repository).createNextVersion(course, source,
        new CallerIdentity("gateway", user, null, null))).isInstanceOf(IllegalStateException.class);
  }
}
