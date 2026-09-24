package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Respuesta a una mención del agente en el chat.
 */
public record AgentMentionResponse(
        @JsonProperty("message_id")
        String messageId,

        @JsonProperty("response_text")
        String responseText,

        @JsonProperty("suppress_reply_events")
        boolean suppressReplyEvents,

        @JsonProperty("metadata")
        Map<String, Object> metadata
) {
}
