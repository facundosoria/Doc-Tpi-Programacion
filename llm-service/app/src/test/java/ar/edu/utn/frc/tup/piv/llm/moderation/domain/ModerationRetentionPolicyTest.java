package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationRetentionPolicyTest {

    @Test
    void ofBuildsDefaultPolicy() {
        ModerationRetentionPolicy p = ModerationRetentionPolicy.of("HATE", 30);

        assertThat(p.id()).isNotNull();
        assertThat(p.incidentType()).isEqualTo("HATE");
        assertThat(p.severity()).isEqualTo("DEFAULT");
        assertThat(p.retentionDays()).isEqualTo(30);
        assertThat(p.updatedBy()).isEqualTo("SYSTEM");
        assertThat(p.updatedAt()).isNotNull();
        assertThat(p.version()).isEqualTo(1L);
    }

    @Test
    void blankAndNullFieldsFallBackToDefaults() {
        ModerationRetentionPolicy p = new ModerationRetentionPolicy(UUID.randomUUID(), "X", " ", 1, null, "", 2L);

        assertThat(p.severity()).isEqualTo("DEFAULT");
        assertThat(p.updatedBy()).isEqualTo("SYSTEM");
        assertThat(p.updatedAt()).isNotNull();
        assertThat(new ModerationRetentionPolicy(UUID.randomUUID(), "X", null, 1, OffsetDateTime.now(), null, 1L)
                .severity()).isEqualTo("DEFAULT");
    }

    @Test
    void keepsExplicitValues() {
        OffsetDateTime at = OffsetDateTime.now().minusDays(1);
        ModerationRetentionPolicy p = new ModerationRetentionPolicy(UUID.randomUUID(), "X", "HIGH", 9, at, "bob", 3L);

        assertThat(p.severity()).isEqualTo("HIGH");
        assertThat(p.updatedBy()).isEqualTo("bob");
        assertThat(p.updatedAt()).isEqualTo(at);
    }

    @Test
    void rejectsNullIncidentTypeAndInvalidDays() {
        assertThatThrownBy(() -> ModerationRetentionPolicy.of(null, 5)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ModerationRetentionPolicy.of("X", 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("al menos 1");
    }
}
