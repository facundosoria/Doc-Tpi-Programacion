package ar.edu.utn.frc.tup.piv.llm.moderation.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Respuesta de resolución de incidente de moderación (LLM-S12-H02 / CA3 / CA4).
 */
public record ModerationResolutionResponse(
        @JsonProperty("incident_id")
        UUID incidentId,

        @JsonProperty("resolution")
        String resolution,

        @JsonProperty("resolved_by")
        String resolvedBy,

        @JsonProperty("resolution_reason")
        String resolutionReason,

        @JsonProperty("resolved_at")
        OffsetDateTime resolvedAt,

        @JsonProperty("status")
        String status
) {
}
