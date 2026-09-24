package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationIncidentTest {

    @Test
    void createAndGetters() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ModerationIncident incident = new ModerationIncident(
                id, "msg-1", "user-1", "course-1", "BLOCK", "SPAM", "preview text", now
        );

        assertThat(incident.getId()).isEqualTo(id);
        assertThat(incident.getMessageId()).isEqualTo("msg-1");
        assertThat(incident.getUserId()).isEqualTo("user-1");
        assertThat(incident.getCourseId()).isEqualTo("course-1");
        assertThat(incident.getStatus()).isEqualTo("BLOCK");
        assertThat(incident.getReasonCode()).isEqualTo("SPAM");
        assertThat(incident.getMessagePreview()).isEqualTo("preview text");
        assertThat(incident.getCreatedAt()).isEqualTo(now);
        assertThat(incident.isBlock()).isTrue();
        assertThat(incident.isOwnedBy("user-1")).isTrue();
        assertThat(incident.isOwnedBy("user-2")).isFalse();
        assertThat(incident.isOwnedBy(null)).isFalse();
    }

    @Test
    void ofBlockFactory() {
        UUID id = UUID.randomUUID();
        ModerationIncident incident = ModerationIncident.ofBlock(
                id, "msg-2", "user-2", "course-2", "OFFENSIVE", "bad words"
        );

        assertThat(incident.getId()).isEqualTo(id);
        assertThat(incident.isBlock()).isTrue();
        assertThat(incident.getStatus()).isEqualTo("BLOCK");
        assertThat(incident.getReasonCode()).isEqualTo("OFFENSIVE");
        assertThat(incident.getCreatedAt()).isNotNull();
    }

    @Test
    void validatesInvariants() {
        assertThatThrownBy(() -> new ModerationIncident(null, "m", "u", "c", "BLOCK", "r", "p", null))
                .isInstanceOf(NullPointerException.class);

        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> new ModerationIncident(id, "m", null, "c", "BLOCK", "r", "p", null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ModerationIncident(id, "m", "  ", "c", "BLOCK", "r", "p", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsAndHashCode() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        ModerationIncident inc1 = new ModerationIncident(id, "msg-1", "user-1", "course-1", "BLOCK", "SPAM", "preview", now);
        ModerationIncident inc2 = new ModerationIncident(id, "msg-1", "user-1", "course-1", "BLOCK", "SPAM", "preview", now);

        assertThat(inc1).isEqualTo(inc1);
        assertThat(inc1).isEqualTo(inc2);
        assertThat(inc1.hashCode()).isEqualTo(inc2.hashCode());
        assertThat(inc1).isNotEqualTo(null);
        assertThat(inc1).isNotEqualTo("other");
    }
}
