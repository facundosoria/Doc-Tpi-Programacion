package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationResolutionTest {

    private static final String REASON = "Motivo suficientemente largo para validar.";

    @Test
    void createTrimsFieldsAndDefaultsTimestamp() {
        UUID incident = UUID.randomUUID();
        ModerationResolution r = ModerationResolution.create(incident, "  doc-1 ", ModerationResolutionType.CONFIRMED, "  " + REASON + " ");

        assertThat(r.getId()).isNotNull();
        assertThat(r.getIncidentId()).isEqualTo(incident);
        assertThat(r.getResolvedBy()).isEqualTo("doc-1");
        assertThat(r.getResolutionReason()).isEqualTo(REASON);
        assertThat(r.getResolvedAt()).isNotNull();
        assertThat(r.isConfirmed()).isTrue();
        assertThat(r.isReversed()).isFalse();
        assertThat(r.isPurged()).isFalse();
    }

    @Test
    void reversedFlags() {
        ModerationResolution r = ModerationResolution.create(UUID.randomUUID(), "d", ModerationResolutionType.REVERSED, REASON);
        assertThat(r.isReversed()).isTrue();
        assertThat(r.isConfirmed()).isFalse();
    }

    @Test
    void purgeRemovesReasonKeepingRest() {
        ModerationResolution r = ModerationResolution.create(UUID.randomUUID(), "d", ModerationResolutionType.REVERSED, REASON);

        ModerationResolution purged = r.purge();

        assertThat(purged.isPurged()).isTrue();
        assertThat(purged.getResolutionReason()).isNull();
        assertThat(purged.getId()).isEqualTo(r.getId());
        assertThat(purged.getResolvedAt()).isEqualTo(r.getResolvedAt());
        assertThat(purged.getResolution()).isEqualTo(ModerationResolutionType.REVERSED);
    }

    @Test
    void validatesMandatoryFieldsAndReasonLength() {
        UUID i = UUID.randomUUID();
        assertThatThrownBy(() -> new ModerationResolution(null, i, "d", ModerationResolutionType.CONFIRMED, REASON, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ModerationResolution.create(null, "d", ModerationResolutionType.CONFIRMED, REASON))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ModerationResolution.create(i, "  ", ModerationResolutionType.CONFIRMED, REASON))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModerationResolution.create(i, null, ModerationResolutionType.CONFIRMED, REASON))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModerationResolution.create(i, "d", null, REASON))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ModerationResolution.create(i, "d", ModerationResolutionType.CONFIRMED, "corto"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("entre 20 y 500");
        assertThatThrownBy(() -> ModerationResolution.create(i, "d", ModerationResolutionType.CONFIRMED, "x".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void boundaryReasonLengthsAreAccepted() {
        UUID i = UUID.randomUUID();
        assertThat(ModerationResolution.create(i, "d", ModerationResolutionType.CONFIRMED, "x".repeat(20)).getResolutionReason()).hasSize(20);
        assertThat(ModerationResolution.create(i, "d", ModerationResolutionType.CONFIRMED, "x".repeat(500)).getResolutionReason()).hasSize(500);
    }

    @Test
    void equalsHashCodeToString() {
        UUID id = UUID.randomUUID();
        UUID inc = UUID.randomUUID();
        OffsetDateTime at = OffsetDateTime.now();
        ModerationResolution a = new ModerationResolution(id, inc, "d", ModerationResolutionType.CONFIRMED, REASON, at);
        ModerationResolution b = new ModerationResolution(id, inc, "d", ModerationResolutionType.CONFIRMED, REASON, at);

        assertThat(a).isEqualTo(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(null).isNotEqualTo("x");
        assertThat(a).isNotEqualTo(new ModerationResolution(UUID.randomUUID(), inc, "d", ModerationResolutionType.CONFIRMED, REASON, at));
        assertThat(a).isNotEqualTo(new ModerationResolution(id, UUID.randomUUID(), "d", ModerationResolutionType.CONFIRMED, REASON, at));
        assertThat(a).isNotEqualTo(new ModerationResolution(id, inc, "e", ModerationResolutionType.CONFIRMED, REASON, at));
        assertThat(a).isNotEqualTo(new ModerationResolution(id, inc, "d", ModerationResolutionType.REVERSED, REASON, at));
        assertThat(a).isNotEqualTo(a.purge());
        assertThat(a).isNotEqualTo(new ModerationResolution(id, inc, "d", ModerationResolutionType.CONFIRMED, REASON, at.plusSeconds(1)));
        assertThat(a.toString()).contains("resolvedBy='d'").contains("CONFIRMED").doesNotContain(REASON);
    }
}
