package ar.edu.utn.frc.tup.piv.llm.moderation.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Payload de solicitud para POST /moderation/v1/appeals (LLM-S12-H01).
 */
public record CreateModerationAppealRequest(
        @NotNull(message = "incident_id es obligatorio")
        @JsonProperty("incident_id")
        UUID incidentId,

        @NotBlank(message = "appeal_reason es obligatorio")
        @Size(min = 20, max = 1000, message = "appeal_reason debe tener entre 20 y 1000 caracteres")
        @JsonProperty("appeal_reason")
        String appealReason
) {
}
