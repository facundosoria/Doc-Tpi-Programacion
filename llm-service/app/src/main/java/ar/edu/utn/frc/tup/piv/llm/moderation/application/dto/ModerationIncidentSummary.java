package ar.edu.utn.frc.tup.piv.llm.moderation.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resumen de un incidente para la bandeja docente (LLM-S12-H02 / CA1 / CA2).
 * Aplica minimización estricta de datos según GDPR: el messagePreview nunca supera los 200 caracteres.
 */
public record ModerationIncidentSummary(
        UUID incidentId,
        String messagePreview,
        String reasonCode,
        String decision,
        OffsetDateTime createdAt,
        boolean hasAppeal,
        String appealReason,
        String courseId,
        String status
) {
    public static ModerationIncidentSummary of(
            UUID incidentId,
            String rawMessagePreview,
            String reasonCode,
            String status,
            OffsetDateTime createdAt,
            boolean hasAppeal,
            String appealReason,
            String courseId
    ) {
        return of(incidentId, rawMessagePreview, reasonCode, status, createdAt, hasAppeal, appealReason, courseId, false);
    }

    public static ModerationIncidentSummary of(
            UUID incidentId,
            String rawMessagePreview,
            String reasonCode,
            String status,
            OffsetDateTime createdAt,
            boolean hasAppeal,
            String appealReason,
            String courseId,
            boolean isPurged
    ) {
        String safePreview;
        if (isPurged) {
            safePreview = "PURGED";
        } else if (rawMessagePreview != null) {
            String trimmed = rawMessagePreview.trim();
            safePreview = trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
        } else {
            safePreview = null;
        }
        return new ModerationIncidentSummary(
                incidentId,
                safePreview,
                reasonCode,
                status != null ? status : "BLOCK",
                createdAt,
                hasAppeal,
                appealReason,
                courseId,
                status
        );
    }
}
