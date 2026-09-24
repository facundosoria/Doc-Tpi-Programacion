package ar.edu.utn.frc.tup.piv.llm.moderation.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representación detallada de un incidente de moderación (LLM-S13-H02 / CA_negativo_2).
 * Expone {@code content: "PURGED"} y la fecha {@code purged_at} cuando la evidencia ha sido destruida.
 */
public record ModerationIncidentDetailResponse(
        @JsonProperty("incident_id")
        UUID incidentId,

        @JsonProperty("reason_code")
        String reasonCode,

        @JsonProperty("resolution")
        String resolution,

        @JsonProperty("created_at")
        OffsetDateTime createdAt,

        @JsonProperty("content")
        String content,

        @JsonProperty("purged_at")
        OffsetDateTime purgedAt,

        @JsonProperty("course_id")
        String courseId,

        @JsonProperty("status")
        String status,

        @JsonProperty("has_appeal")
        boolean hasAppeal,

        @JsonProperty("appeal_reason")
        String appealReason
) {
}
