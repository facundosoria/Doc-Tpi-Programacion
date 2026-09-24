package ar.edu.utn.frc.tup.piv.llm.moderation.application.dto;

import java.util.UUID;

/**
 * Comando de caso de uso para solicitar la apelación de un bloqueo de moderación (LLM-S12-H01).
 */
public record CreateModerationAppealCommand(
        UUID incidentId,
        String userId,
        String appealReason
) {
}
