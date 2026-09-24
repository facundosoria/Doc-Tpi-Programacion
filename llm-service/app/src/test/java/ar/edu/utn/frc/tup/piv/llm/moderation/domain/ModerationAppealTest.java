package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModerationAppealTest {

    @Test
    void createInitializesWithPendingReviewStatusAndValidFields() {
        UUID incidentId = UUID.randomUUID();
        String userId = "user-55";
        String reason = "El mensaje explicaba cómo funciona el encoding Base64 en el contexto de la clase.";

        ModerationAppeal appeal = ModerationAppeal.create(incidentId, userId, reason);

        assertThat(appeal.getId()).isNotNull();
        assertThat(appeal.getIncidentId()).isEqualTo(incidentId);
        assertThat(appeal.getUserId()).isEqualTo(userId);
        assertThat(appeal.getAppealReason()).isEqualTo(reason);
        assertThat(appeal.getStatus()).isEqualTo(ModerationAppealStatus.PENDING_REVIEW);
        assertThat(appeal.getCreatedAt()).isNotNull();
        assertThat(appeal.getUpdatedAt()).isNotNull();
        assertThat(appeal.isOwnedBy("user-55")).isTrue();
        assertThat(appeal.isOwnedBy("user-99")).isFalse();
    }

    @Test
    void rejectReasonLessThan20Characters() {
        UUID incidentId = UUID.randomUUID();
        assertThatThrownBy(() -> ModerationAppeal.create(incidentId, "user-55", "Demasiado corto"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 20 y 1000 caracteres");
    }

    @Test
    void rejectReasonGreaterThan1000Characters() {
        UUID incidentId = UUID.randomUUID();
        String longReason = "a".repeat(1001);
        assertThatThrownBy(() -> ModerationAppeal.create(incidentId, "user-55", longReason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 20 y 1000 caracteres");
    }

    @Test
    void rejectNullOrBlankUserId() {
        UUID incidentId = UUID.randomUUID();
        String validReason = "Motivo de longitud válida para el reclamo formal.";
        assertThatThrownBy(() -> ModerationAppeal.create(incidentId, null, validReason))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModerationAppeal.create(incidentId, "   ", validReason))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectNullIncidentId() {
        String validReason = "Motivo de longitud válida para el reclamo formal.";
        assertThatThrownBy(() -> ModerationAppeal.create(null, "user-55", validReason))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void transitionsToConfirmedAndReversed() {
        UUID incidentId = UUID.randomUUID();
        String reason = "Explicación detallada de por qué el contenido es legítimo.";
        ModerationAppeal appeal = ModerationAppeal.create(incidentId, "user-55", reason);

        ModerationAppeal confirmed = appeal.confirm();
        assertThat(confirmed.getStatus()).isEqualTo(ModerationAppealStatus.CONFIRMED);
        assertThat(confirmed.getId()).isEqualTo(appeal.getId());

        ModerationAppeal reversed = appeal.reverse();
        assertThat(reversed.getStatus()).isEqualTo(ModerationAppealStatus.REVERSED);
        assertThat(reversed.getId()).isEqualTo(appeal.getId());
    }

    @Test
    void equalsHashCodeAndToString() {
        UUID incidentId = UUID.randomUUID();
        String reason = "Explicación detallada de por qué el contenido es legítimo.";
        ModerationAppeal appeal1 = ModerationAppeal.create(incidentId, "user-55", reason);

        assertThat(appeal1).isEqualTo(appeal1);
        assertThat(appeal1).isNotEqualTo(null);
        assertThat(appeal1).isNotEqualTo("other type");
        assertThat(appeal1.hashCode()).isEqualTo(appeal1.hashCode());
        assertThat(appeal1.toString()).contains("user-55", "PENDING_REVIEW");
    }
}
