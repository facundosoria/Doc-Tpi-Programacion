package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
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

class ModerationIncidentRepositoryTest {

    private SpringDataModerationIncidentRepository springDataRepository;
    private ModerationIncidentRepository repository;

    @BeforeEach
    void setUp() {
        springDataRepository = mock(SpringDataModerationIncidentRepository.class);
        repository = new ModerationIncidentRepository(springDataRepository);
    }

    @Test
    void saveMapsDomainToEntityAndReturnsSavedDomain() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ModerationIncident domain = new ModerationIncident(
                id, "msg-1", "user-1", "course-1", "BLOCK", "SPAM", "preview", now
        );

        when(springDataRepository.save(any(ModerationIncidentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ModerationIncident saved = repository.save(domain);

        ArgumentCaptor<ModerationIncidentEntity> captor = ArgumentCaptor.forClass(ModerationIncidentEntity.class);
        verify(springDataRepository).save(captor.capture());

        ModerationIncidentEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getMessageId()).isEqualTo("msg-1");
        assertThat(entity.getUserId()).isEqualTo("user-1");
        assertThat(entity.getCourseId()).isEqualTo("course-1");
        assertThat(entity.getStatus()).isEqualTo("BLOCK");
        assertThat(entity.getReasonCode()).isEqualTo("SPAM");
        assertThat(entity.getMessagePreview()).isEqualTo("preview");
        assertThat(entity.getCreatedAt()).isEqualTo(now);

        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getMessageId()).isEqualTo("msg-1");
    }

    @Test
    void findByIdMapsEntityToDomain() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ModerationIncidentEntity entity = new ModerationIncidentEntity(
                id, "msg-1", "user-1", "course-1", "BLOCK", "SPAM", "preview", now
        );

        when(springDataRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<ModerationIncident> result = repository.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getUserId()).isEqualTo("user-1");
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(springDataRepository.findById(id)).thenReturn(Optional.empty());

        Optional<ModerationIncident> result = repository.findById(id);

        assertThat(result).isEmpty();
    }

    @Test
    void entityGettersAndSetters() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ModerationIncidentEntity entity = new ModerationIncidentEntity();
        entity.setId(id);
        entity.setMessageId("m1");
        entity.setUserId("u1");
        entity.setCourseId("c1");
        entity.setStatus("PENDING");
        entity.setReasonCode("CODE_OBFUSCATION");
        entity.setMessagePreview("code");
        entity.setCreatedAt(now);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getMessageId()).isEqualTo("m1");
        assertThat(entity.getUserId()).isEqualTo("u1");
        assertThat(entity.getCourseId()).isEqualTo("c1");
        assertThat(entity.getStatus()).isEqualTo("PENDING");
        assertThat(entity.getReasonCode()).isEqualTo("CODE_OBFUSCATION");
        assertThat(entity.getMessagePreview()).isEqualTo("code");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void findByCourseIdAndStatusReturnsMappedIncidents() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ModerationIncidentEntity entity = new ModerationIncidentEntity(
                id, "m1", "u1", "course-1", "BLOCK", "SPAM", "preview", now
        );
        org.springframework.data.domain.Page<ModerationIncidentEntity> page =
                new org.springframework.data.domain.PageImpl<>(java.util.List.of(entity));

        when(springDataRepository.findByCourseIdAndStatusFilter(
                org.mockito.ArgumentMatchers.eq("course-1"),
                org.mockito.ArgumentMatchers.eq("BLOCK"),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(page);

        java.util.List<ModerationIncident> result = repository.findByCourseIdAndStatus("course-1", "BLOCK", 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(id);
        assertThat(result.get(0).getCourseId()).isEqualTo("course-1");
    }

    @Test
    void countByCourseIdAndStatusDelegates() {
        when(springDataRepository.countByCourseIdAndStatusFilter("course-1", "BLOCK")).thenReturn(5L);

        long count = repository.countByCourseIdAndStatus("course-1", "BLOCK");

        assertThat(count).isEqualTo(5L);
        verify(springDataRepository).countByCourseIdAndStatusFilter("course-1", "BLOCK");
    }
}
