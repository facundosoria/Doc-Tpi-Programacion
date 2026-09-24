package ar.edu.utn.frc.tup.piv.llm.moderation.application.dto;

import java.util.Map;

/**
 * Comando desacoplado para solicitar una decisión de moderación.
 */
public record ModerationDecisionCommand(
        String messageId,
        String courseId,
        String senderRole,
        String text,
        Map<String, Object> contextFlags,
        String senderId
) {
    /** Compatibilidad: emisor sin identificar (mensajes generados por el sistema o agente). */
    public ModerationDecisionCommand(String messageId, String courseId, String senderRole,
                                     String text, Map<String, Object> contextFlags) {
        this(messageId, courseId, senderRole, text, contextFlags, null);
    }
}
