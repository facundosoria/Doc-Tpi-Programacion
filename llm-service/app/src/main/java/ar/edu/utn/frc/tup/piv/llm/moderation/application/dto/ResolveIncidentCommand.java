package ar.edu.utn.frc.tup.piv.llm.moderation.application.dto;

import java.util.UUID;

/**
 * Comando para resolver un incidente de moderación (LLM-S12-H02 / T2).
 */
public record ResolveIncidentCommand(
        UUID incidentId,
        String resolution,
        String resolutionReason,
        String resolvedBy
) {
}
