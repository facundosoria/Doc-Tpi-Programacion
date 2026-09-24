package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionType;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationResolutionEntityTest {

    @Test
    void constructorAndAccessorsRoundTrip() {
        UUID id = UUID.randomUUID();
        UUID inc = UUID.randomUUID();
        OffsetDateTime at = OffsetDateTime.now();
        ModerationResolutionEntity e = new ModerationResolutionEntity(id, inc, "d", ModerationResolutionType.CONFIRMED, "r", at);

        assertThat(e.getId()).isEqualTo(id);
        assertThat(e.getIncidentId()).isEqualTo(inc);
        assertThat(e.getResolvedBy()).isEqualTo("d");
        assertThat(e.getResolution()).isEqualTo(ModerationResolutionType.CONFIRMED);
        assertThat(e.getResolutionReason()).isEqualTo("r");
        assertThat(e.getResolvedAt()).isEqualTo(at);
    }

    @Test
    void settersOverrideValuesOnDefaultConstructedEntity() {
        ModerationResolutionEntity e = new ModerationResolutionEntity();
        assertThat(e.getId()).isNull();
        UUID id = UUID.randomUUID();
        UUID inc = UUID.randomUUID();
        OffsetDateTime at = OffsetDateTime.now();

        e.setId(id);
        e.setIncidentId(inc);
        e.setResolvedBy("x");
        e.setResolution(ModerationResolutionType.REVERSED);
        e.setResolutionReason("reason");
        e.setResolvedAt(at);

        assertThat(e.getId()).isEqualTo(id);
        assertThat(e.getIncidentId()).isEqualTo(inc);
        assertThat(e.getResolvedBy()).isEqualTo("x");
        assertThat(e.getResolution()).isEqualTo(ModerationResolutionType.REVERSED);
        assertThat(e.getResolutionReason()).isEqualTo("reason");
        assertThat(e.getResolvedAt()).isEqualTo(at);
    }
}
