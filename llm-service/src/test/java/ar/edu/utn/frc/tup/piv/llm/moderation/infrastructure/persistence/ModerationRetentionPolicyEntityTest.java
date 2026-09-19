package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationRetentionPolicyEntityTest {

    @Test
    void constructorDefaultsNullSeverityActorAndTimestamp() {
        ModerationRetentionPolicyEntity e = new ModerationRetentionPolicyEntity(UUID.randomUUID(), "T", null, 7, null, null, 3L);

        assertThat(e.getSeverity()).isEqualTo("DEFAULT");
        assertThat(e.getUpdatedBy()).isEqualTo("SYSTEM");
        assertThat(e.getUpdatedAt()).isNotNull();
        assertThat(e.getRetentionDays()).isEqualTo(7);
        assertThat(e.getVersion()).isEqualTo(3L);
        assertThat(e.getIncidentType()).isEqualTo("T");
    }

    @Test
    void constructorKeepsExplicitValuesAndSettersWork() {
        OffsetDateTime at = OffsetDateTime.now().minusHours(1);
        UUID id = UUID.randomUUID();
        ModerationRetentionPolicyEntity e = new ModerationRetentionPolicyEntity(id, "T", "HIGH", 7, at, "bob", 1L);
        assertThat(e.getSeverity()).isEqualTo("HIGH");
        assertThat(e.getUpdatedBy()).isEqualTo("bob");
        assertThat(e.getUpdatedAt()).isEqualTo(at);
        assertThat(e.getId()).isEqualTo(id);

        ModerationRetentionPolicyEntity d = new ModerationRetentionPolicyEntity();
        UUID id2 = UUID.randomUUID();
        d.setId(id2);
        d.setIncidentType("Z");
        d.setSeverity("LOW");
        d.setRetentionDays(11);
        d.setUpdatedAt(at);
        d.setUpdatedBy("amy");
        d.setVersion(9L);
        assertThat(d.getId()).isEqualTo(id2);
        assertThat(d.getIncidentType()).isEqualTo("Z");
        assertThat(d.getSeverity()).isEqualTo("LOW");
        assertThat(d.getRetentionDays()).isEqualTo(11);
        assertThat(d.getUpdatedAt()).isEqualTo(at);
        assertThat(d.getUpdatedBy()).isEqualTo("amy");
        assertThat(d.getVersion()).isEqualTo(9L);
    }
}
