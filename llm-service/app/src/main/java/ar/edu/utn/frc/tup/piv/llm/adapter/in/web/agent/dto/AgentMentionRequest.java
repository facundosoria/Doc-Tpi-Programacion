package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

/**
 * Payload de solicitud para menciones al agente en el chat (LLM-S18-H01 / LLM-S18-H02).
 * Contrato acordado con chat-service.
 */
public record AgentMentionRequest(
        @JsonProperty("message_id")
        String messageId,

        @JsonProperty("cohort_id")
        String cohortId,

        @JsonProperty("thread_id")
        String threadId,

        @JsonProperty("sender_role")
        String senderRole,

        @JsonProperty("student_id")
        String studentId,

        @Size(max = 4096, message = "text no puede superar los 4096 caracteres")
        @JsonProperty("text")
        String text
) {
}
