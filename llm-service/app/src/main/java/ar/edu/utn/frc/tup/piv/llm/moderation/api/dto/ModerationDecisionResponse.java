package ar.edu.utn.frc.tup.piv.llm.moderation.api.dto;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationDecisionEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * Payload de respuesta 200 OK para POST /moderation/v1/decisions.
 */
public record ModerationDecisionResponse(
        @JsonProperty("message_id")
        String messageId,

        @JsonProperty("decision")
        ModerationDecisionEnum decision,

        @JsonProperty("reason_code")
        String reasonCode,

        @JsonProperty("classifier_used")
        String classifierUsed,

        @JsonProperty("latency_ms")
        long latencyMs,

        @JsonProperty("incident_id")
        @JsonInclude(JsonInclude.Include.ALWAYS)
        UUID incidentId,

        @JsonProperty("degradation_reason")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String degradationReason
) {
    public ModerationDecisionResponse(
            String messageId,
            ModerationDecisionEnum decision,
            String reasonCode,
            String classifierUsed,
            long latencyMs,
            UUID incidentId) {
        this(messageId, decision, reasonCode, classifierUsed, latencyMs, incidentId, null);
    }
}
