package ar.edu.utn.frc.tup.piv.llm.moderation.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO para la representación de un incidente en la bandeja docente (LLM-S12-H02 / CA2).
 * Contiene message_preview truncado a <= 200 chars y datos de apelación si existen.
 */
public record ModerationIncidentResponse(
        @JsonProperty("incident_id")
        UUID incidentId,

        @JsonProperty("message_preview")
        String messagePreview,

        @JsonProperty("reason_code")
        String reasonCode,

        @JsonProperty("decision")
        String decision,

        @JsonProperty("created_at")
        OffsetDateTime createdAt,

        @JsonProperty("has_appeal")
        boolean hasAppeal,

        @JsonProperty("appeal_reason")
        String appealReason,

        @JsonProperty("course_id")
        String courseId,

        @JsonProperty("status")
        String status
) {
}
