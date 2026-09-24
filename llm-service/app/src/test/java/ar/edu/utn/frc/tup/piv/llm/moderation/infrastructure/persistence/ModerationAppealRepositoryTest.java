package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppealStatus;
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

class ModerationAppealRepositoryTest {

    private SpringDataModerationAppealRepository springDataRepository;
    private ModerationAppealRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataModerationAppealRepository.class);
        repository = new ModerationAppealRepository(springDataRepository);
    }

    @Test
    void saveMapsDomainToEntityAndReturnsSavedDomain() {
        UUID incidentId = UUID.randomUUID();
        ModerationAppeal domain = ModerationAppeal.create(incidentId, "user-55", "Explicación detallada del caso legítimo.");

        when(springDataRepository.save(any(ModerationAppealEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ModerationAppeal saved = repository.save(domain);

        ArgumentCaptor<ModerationAppealEntity> captor = ArgumentCaptor.forClass(ModerationAppealEntity.class);
        verify(springDataRepository).save(captor.capture());

        ModerationAppealEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo(domain.getId());
        assertThat(entity.getIncidentId()).isEqualTo(incidentId);
        assertThat(entity.getUserId()).isEqualTo("user-55");
        assertThat(entity.getAppealReason()).isEqualTo("Explicación detallada del caso legítimo.");
        assertThat(entity.getStatus()).isEqualTo(ModerationAppealStatus.PENDING_REVIEW);

        assertThat(saved.getId()).isEqualTo(domain.getId());
        assertThat(saved.getIncidentId()).isEqualTo(incidentId);
    }

    @Test
    void findByIdMapsEntityToDomain() {
        UUID id = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ModerationAppealEntity entity = new ModerationAppealEntity(
                id, incidentId, "user-55", "Motivo de prueba con longitud suficiente.",
                ModerationAppealStatus.PENDING_REVIEW, now, now
        );

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<ModerationAppeal> result = repository.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getUserId()).isEqualTo("user-55");
        assertThat(result.get().getStatus()).isEqualTo(ModerationAppealStatus.PENDING_REVIEW);
    }

    @Test
    void findByIncidentIdReturnsMappedDomain() {
        UUID incidentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ModerationAppealEntity entity = new ModerationAppealEntity(
                UUID.randomUUID(), incidentId, "user-55", "Motivo de prueba con longitud suficiente.",
                ModerationAppealStatus.PENDING_REVIEW, now, now
        );

        when(springDataRepository.findByIncidentId(incidentId)).thenReturn(Optional.of(entity));

        Optional<ModerationAppeal> result = repository.findByIncidentId(incidentId);

        assertThat(result).isPresent();
        assertThat(result.get().getIncidentId()).isEqualTo(incidentId);
    }

    @Test
    void existsByIncidentIdDelegatesToSpringData() {
        UUID incidentId = UUID.randomUUID();
        when(springDataRepository.existsByIncidentId(incidentId)).thenReturn(true);

        assertThat(repository.existsByIncidentId(incidentId)).isTrue();
        verify(springDataRepository).existsByIncidentId(incidentId);
    }

    @Test
    void entityGettersAndSetters() {
        UUID id = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ModerationAppealEntity entity = new ModerationAppealEntity();
        entity.setId(id);
        entity.setIncidentId(incidentId);
        entity.setUserId("user-1");
        entity.setAppealReason("revisión requerida");
        entity.setStatus(ModerationAppealStatus.CONFIRMED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getIncidentId()).isEqualTo(incidentId);
        assertThat(entity.getUserId()).isEqualTo("user-1");
        assertThat(entity.getAppealReason()).isEqualTo("revisión requerida");
        assertThat(entity.getStatus()).isEqualTo(ModerationAppealStatus.CONFIRMED);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }
}
