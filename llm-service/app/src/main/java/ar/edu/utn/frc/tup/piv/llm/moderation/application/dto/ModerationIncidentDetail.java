package ar.edu.utn.frc.tup.piv.llm.moderation.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Detalle completo de un incidente de moderación (LLM-S13-H02 / CA_negativo_2).
 * Si el incidente fue purgado, {@code content} contendrá explícitamente "PURGED".
 */
public record ModerationIncidentDetail(
        UUID incidentId,
        String messageId,
        String userId,
        String courseId,
        String status,
        String reasonCode,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime purgedAt,
        String resolution,
        boolean hasAppeal,
        String appealReason
) {
}
