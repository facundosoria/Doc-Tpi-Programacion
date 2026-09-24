package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent;

import java.util.List;

/**
 * Respuesta a la mención del agente con citas y estado (HU LLM-S18-H01 · T1).
 */
public record AgentMentionResponse(
    String replyText,
    List<MentionCitationDto> citations,
    String status
) {
  public AgentMentionResponse {
    if (citations == null) {
      citations = List.of();
    }
  }
}
