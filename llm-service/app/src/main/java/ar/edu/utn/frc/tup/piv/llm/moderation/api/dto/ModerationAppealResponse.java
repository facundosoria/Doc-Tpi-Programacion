package ar.edu.utn.frc.tup.piv.llm.moderation.api.dto;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppealStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload de respuesta para el estado y detalle de una apelación de moderación (LLM-S12-H01).
 */
public record ModerationAppealResponse(
        @JsonProperty("appeal_id")
        UUID appealId,

        @JsonProperty("incident_id")
        UUID incidentId,

        @JsonProperty("user_id")
        String userId,

        @JsonProperty("appeal_reason")
        String appealReason,

        @JsonProperty("status")
        ModerationAppealStatus status,

        @JsonProperty("created_at")
        OffsetDateTime createdAt,

        @JsonProperty("updated_at")
        OffsetDateTime updatedAt
) {
}
