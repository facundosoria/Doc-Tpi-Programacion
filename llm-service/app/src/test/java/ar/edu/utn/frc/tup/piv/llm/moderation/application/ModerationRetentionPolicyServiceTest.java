package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.ModerationRetentionPolicyEntity;
import ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence.SpringDataModerationRetentionPolicyRepository;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationRetentionPolicyServiceTest {

    private SpringDataModerationRetentionPolicyRepository repository;
    private ModerationRetentionPolicyService service;

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataModerationRetentionPolicyRepository.class);
        service = new ModerationRetentionPolicyService(repository);
        when(repository.findAll()).thenReturn(List.of());
        when(repository.findByIncidentTypeAndSeverity(any(), any())).thenReturn(Optional.empty());
    }

    private static ModerationRetentionPolicyEntity entity(String type, int days) {
        return new ModerationRetentionPolicyEntity(UUID.randomUUID(), type, "DEFAULT", days,
                OffsetDateTime.now(), "SYSTEM", 1L);
    }

    @Test
    void getPoliciesMapsIncidentTypeToDaysPreservingOrder() {
        when(repository.findAll()).thenReturn(List.of(entity("B", 30), entity("A", 90)));

        Map<String, Integer> result = service.getPolicies();

        assertThat(result).containsExactly(Map.entry("B", 30), Map.entry("A", 90));
    }

    @Test
    void updateWithNullOrEmptyPayloadOnlyReturnsCurrentPolicies() {
        when(repository.findAll()).thenReturn(List.of(entity("A", 10)));

        assertThat(service.updatePolicies(null, "x")).containsEntry("A", 10);
        assertThat(service.updatePolicies(Map.of(), "x")).containsEntry("A", 10);
        verify(repository, never()).save(any());
    }

    @Test
    void updateCreatesNewEntityWhenNoneExistsAndTrimsKeyAndActor() {
        service.updatePolicies(Map.of(" HATE ", 45), "  teacher-1 ");

        ArgumentCaptor<ModerationRetentionPolicyEntity> cap = ArgumentCaptor.forClass(ModerationRetentionPolicyEntity.class);
        verify(repository).save(cap.capture());
        ModerationRetentionPolicyEntity saved = cap.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getIncidentType()).isEqualTo("HATE");
        assertThat(saved.getSeverity()).isEqualTo("DEFAULT");
        assertThat(saved.getRetentionDays()).isEqualTo(45);
        assertThat(saved.getUpdatedBy()).isEqualTo("teacher-1");
        assertThat(saved.getVersion()).isEqualTo(1L);
    }

    @Test
    void updateModifiesExistingEntityAndDefaultsActorToAdmin() {
        ModerationRetentionPolicyEntity existing = entity("HATE", 10);
        when(repository.findByIncidentTypeAndSeverity("HATE", "DEFAULT")).thenReturn(Optional.of(existing));

        service.updatePolicies(Map.of("HATE", 60), "   ");

        verify(repository).save(existing);
        assertThat(existing.getRetentionDays()).isEqualTo(60);
        assertThat(existing.getUpdatedBy()).isEqualTo("ADMIN");
    }

    @Test
    void updateNullActorDefaultsToAdmin() {
        service.updatePolicies(Map.of("X", 5), null);

        ArgumentCaptor<ModerationRetentionPolicyEntity> cap = ArgumentCaptor.forClass(ModerationRetentionPolicyEntity.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getUpdatedBy()).isEqualTo("ADMIN");
    }

    @Test
    void extractDaysSupportsNestedMapKeysAndStringValues() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("A", Map.of("retentionDays", 11));
        payload.put("B", Map.of("retention_days", "22"));
        payload.put("C", Map.of("days", 33));
        payload.put("D", "44");
        payload.put("E", 55L);

        service.updatePolicies(payload, "u");

        ArgumentCaptor<ModerationRetentionPolicyEntity> cap = ArgumentCaptor.forClass(ModerationRetentionPolicyEntity.class);
        verify(repository, times(5)).save(cap.capture());
        Map<String, Integer> saved = new HashMap<>();
        cap.getAllValues().forEach(e -> saved.put(e.getIncidentType(), e.getRetentionDays()));
        assertThat(saved).containsOnly(
                Map.entry("A", 11), Map.entry("B", 22), Map.entry("C", 33),
                Map.entry("D", 44), Map.entry("E", 55));
    }

    @Test
    void invalidOrNonPositiveValuesAreSkipped() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ZERO", 0);
        payload.put("NEG", -5);
        payload.put("TEXT", "abc");
        payload.put("NULL", null);
        payload.put("MAPBAD", Map.of("days", "nope"));
        payload.put("MAPEMPTY", Map.of("other", 3));
        payload.put("BOOL", true);

        service.updatePolicies(payload, "u");

        verify(repository, never()).save(any());
    }
}
