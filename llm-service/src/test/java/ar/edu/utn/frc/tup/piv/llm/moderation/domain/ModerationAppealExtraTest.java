package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationAppealExtraTest {

    private static final String REASON = "Motivo de apelacion suficientemente largo.";

    private ModerationAppeal appeal() {
        return ModerationAppeal.create(UUID.randomUUID(), "u1", REASON);
    }

    @Test
    void confirmAndReverseChangeStatusOnly() {
        ModerationAppeal a = appeal();

        ModerationAppeal c = a.confirm();
        ModerationAppeal r = a.reverse();

        assertThat(c.getStatus()).isEqualTo(ModerationAppealStatus.CONFIRMED);
        assertThat(r.getStatus()).isEqualTo(ModerationAppealStatus.REVERSED);
        assertThat(a.getStatus()).isEqualTo(ModerationAppealStatus.PENDING_REVIEW);
        assertThat(c.getId()).isEqualTo(a.getId());
        assertThat(c.getAppealReason()).isEqualTo(REASON);
        assertThat(c.getCreatedAt()).isEqualTo(a.getCreatedAt());
    }

    @Test
    void purgeRemovesReason() {
        ModerationAppeal a = appeal();
        assertThat(a.isPurged()).isFalse();

        ModerationAppeal p = a.purge();

        assertThat(p.isPurged()).isTrue();
        assertThat(p.getAppealReason()).isNull();
        assertThat(p.getStatus()).isEqualTo(a.getStatus());
    }

    @Test
    void isOwnedByHandlesNullAndWhitespace() {
        ModerationAppeal a = appeal();
        assertThat(a.isOwnedBy(null)).isFalse();
        assertThat(a.isOwnedBy("  u1 ")).isTrue();
    }

    @Test
    void constructorValidatesMandatoryFields() {
        UUID id = UUID.randomUUID();
        UUID inc = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ModerationAppealStatus s = ModerationAppealStatus.PENDING_REVIEW;
        assertThatThrownBy(() -> new ModerationAppeal(null, inc, "u", REASON, s, now, now)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ModerationAppeal(id, null, "u", REASON, s, now, now)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ModerationAppeal(id, inc, " ", REASON, s, now, now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModerationAppeal(id, inc, null, REASON, s, now, now)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ModerationAppeal(id, inc, "u", REASON, null, now, now)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ModerationAppeal(id, inc, "u", REASON, s, null, now)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ModerationAppeal(id, inc, "u", REASON, s, now, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void equalsHashCodeToString() {
        UUID id = UUID.randomUUID();
        UUID inc = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ModerationAppealStatus s = ModerationAppealStatus.PENDING_REVIEW;
        ModerationAppeal a = new ModerationAppeal(id, inc, "u", REASON, s, now, now);
        ModerationAppeal b = new ModerationAppeal(id, inc, "u", REASON, s, now, now);

        assertThat(a).isEqualTo(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(null).isNotEqualTo("x");
        assertThat(a).isNotEqualTo(new ModerationAppeal(UUID.randomUUID(), inc, "u", REASON, s, now, now));
        assertThat(a).isNotEqualTo(new ModerationAppeal(id, UUID.randomUUID(), "u", REASON, s, now, now));
        assertThat(a).isNotEqualTo(new ModerationAppeal(id, inc, "v", REASON, s, now, now));
        assertThat(a).isNotEqualTo(new ModerationAppeal(id, inc, "u", null, s, now, now));
        assertThat(a).isNotEqualTo(new ModerationAppeal(id, inc, "u", REASON, ModerationAppealStatus.CONFIRMED, now, now));
        assertThat(a).isNotEqualTo(new ModerationAppeal(id, inc, "u", REASON, s, now.plusSeconds(1), now));
        assertThat(a).isNotEqualTo(new ModerationAppeal(id, inc, "u", REASON, s, now, now.plusSeconds(1)));
        assertThat(a.toString()).contains("userId='u'").doesNotContain(REASON);
    }
}
