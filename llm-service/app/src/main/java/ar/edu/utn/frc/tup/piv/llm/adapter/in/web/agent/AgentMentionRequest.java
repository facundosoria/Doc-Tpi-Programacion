package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Petición para el endpoint POST /api/llm/v1/agent/mentions (HU LLM-S18-H01 · T1).
 */
public record AgentMentionRequest(
    @NotBlank(message = "messageId no puede estar vacío")
    @JsonProperty("messageId")
    @JsonAlias("message_id")
    String messageId,

    @NotBlank(message = "cohortId no puede estar vacío")
    @JsonProperty("cohortId")
    @JsonAlias("cohort_id")
    String cohortId,

    @NotBlank(message = "threadId no puede estar vacío")
    @JsonProperty("threadId")
    @JsonAlias("thread_id")
    String threadId,

    @NotBlank(message = "senderRole no puede estar vacío")
    @JsonProperty("senderRole")
    @JsonAlias("sender_role")
    String senderRole,

    @NotBlank(message = "studentId no puede estar vacío")
    @JsonProperty("studentId")
    @JsonAlias("student_id")
    String studentId,

    @NotBlank(message = "text no puede estar vacío")
    @JsonProperty("text")
    String text
) {

  public AgentMentionRequest(String messageId, UUID cohortId, String threadId, String senderRole, String studentId, String text) {
    this(messageId, cohortId != null ? cohortId.toString() : null, threadId, senderRole, studentId, text);
  }

  public UUID cohortUuid() {
    if (cohortId == null) {
      return null;
    }
    try {
      return UUID.fromString(cohortId);
    } catch (IllegalArgumentException e) {
      return UUID.nameUUIDFromBytes(cohortId.getBytes(StandardCharsets.UTF_8));
    }
  }
}
