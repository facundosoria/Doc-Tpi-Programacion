package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolution;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationResolutionRepositoryTest {

    private static final String REASON = "Motivo suficientemente largo para validar.";

    private SpringDataModerationResolutionRepository spring;
    private ModerationResolutionRepository repository;

    @BeforeEach
    void setUp() {
        spring = mock(SpringDataModerationResolutionRepository.class);
        repository = new ModerationResolutionRepository(spring);
    }

    @Test
    void saveMapsDomainToEntityAndBack() {
        ModerationResolution domain = ModerationResolution.create(UUID.randomUUID(), "doc", ModerationResolutionType.REVERSED, REASON);
        when(spring.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ModerationResolution result = repository.save(domain);

        ArgumentCaptor<ModerationResolutionEntity> cap = ArgumentCaptor.forClass(ModerationResolutionEntity.class);
        verify(spring).save(cap.capture());
        assertThat(cap.getValue().getIncidentId()).isEqualTo(domain.getIncidentId());
        assertThat(cap.getValue().getResolution()).isEqualTo(ModerationResolutionType.REVERSED);
        assertThat(cap.getValue().getResolutionReason()).isEqualTo(REASON);
        assertThat(result).isEqualTo(domain);
    }

    @Test
    void findByIncidentIdMapsPresentAndEmpty() {
        UUID inc = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        OffsetDateTime at = OffsetDateTime.now();
        when(spring.findByIncidentId(inc)).thenReturn(Optional.of(
                new ModerationResolutionEntity(id, inc, "doc", ModerationResolutionType.CONFIRMED, REASON, at)));
        when(spring.findByIncidentId(other)).thenReturn(Optional.empty());

        ModerationResolution found = repository.findByIncidentId(inc).orElseThrow();
        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.isConfirmed()).isTrue();
        assertThat(found.getResolvedAt()).isEqualTo(at);
        assertThat(repository.findByIncidentId(other)).isEmpty();
    }

    @Test
    void existsByIncidentIdDelegates() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(spring.existsByIncidentId(a)).thenReturn(true);
        when(spring.existsByIncidentId(b)).thenReturn(false);

        assertThat(repository.existsByIncidentId(a)).isTrue();
        assertThat(repository.existsByIncidentId(b)).isFalse();
    }
}
