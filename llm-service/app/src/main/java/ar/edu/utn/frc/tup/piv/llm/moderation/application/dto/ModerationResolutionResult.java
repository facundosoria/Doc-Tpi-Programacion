package ar.edu.utn.frc.tup.piv.llm.moderation.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resultado de la resolución de un incidente de moderación (LLM-S12-H02 / CA3 / CA4).
 */
public record ModerationResolutionResult(
        UUID incidentId,
        String resolution,
        String resolvedBy,
        String resolutionReason,
        OffsetDateTime resolvedAt,
        String status
) {
}
