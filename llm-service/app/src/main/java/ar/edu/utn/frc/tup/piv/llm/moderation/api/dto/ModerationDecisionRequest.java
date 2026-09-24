package ar.edu.utn.frc.tup.piv.llm.moderation.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Payload de solicitud para POST /moderation/v1/decisions según contrato con chat-service.
 */
public record ModerationDecisionRequest(
        @NotBlank(message = "message_id es obligatorio")
        @JsonProperty("message_id")
        String messageId,

        @NotBlank(message = "course_id es obligatorio")
        @JsonProperty("course_id")
        String courseId,

        @NotBlank(message = "sender_id es obligatorio")
        @Size(max = 255, message = "sender_id no puede superar los 255 caracteres")
        @JsonProperty("sender_id")
        String senderId,

        @NotBlank(message = "sender_role es obligatorio")
        @JsonProperty("sender_role")
        String senderRole,

        @NotBlank(message = "text es obligatorio")
        @Size(max = 4096, message = "text no puede superar los 4096 caracteres")
        @JsonProperty("text")
        String text,

        @JsonProperty("context_flags")
        Map<String, Object> contextFlags
) {
}
