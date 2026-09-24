package ar.edu.utn.frc.tup.piv.llm.moderation.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * Payload para resolver un incidente de moderación (LLM-S12-H02 / CA3).
 * Soporta tanto 'resolution_reason' como 'reason' para máxima interoperabilidad.
 */
public record ResolveModerationIncidentRequest(
        @NotNull(message = "resolution es obligatorio")
        @JsonProperty("resolution")
        String resolution,

        @JsonProperty("resolution_reason")
        String resolutionReason,

        @JsonProperty("reason")
        String reason
) {
    public String effectiveReason() {
        if (resolutionReason != null && !resolutionReason.isBlank()) {
            return resolutionReason;
        }
        return reason;
    }
}
