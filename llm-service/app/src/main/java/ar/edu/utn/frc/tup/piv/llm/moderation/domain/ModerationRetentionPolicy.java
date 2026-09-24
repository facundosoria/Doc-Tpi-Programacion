package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio inmutable que modela la política de retención de evidencia de moderación (LLM-S13-H02 / T1).
 * Totalmente desacoplada de Spring y frameworks de persistencia (ADR-019).
 */
public record ModerationRetentionPolicy(
        UUID id,
        String incidentType,
        String severity,
        int retentionDays,
        OffsetDateTime updatedAt,
        String updatedBy,
        long version
) {
    public ModerationRetentionPolicy {
        Objects.requireNonNull(incidentType, "incidentType no puede ser nulo.");
        if (retentionDays < 1) {
            throw new IllegalArgumentException("retentionDays debe ser al menos 1 día.");
        }
        if (severity == null || severity.isBlank()) {
            severity = "DEFAULT";
        }
        if (updatedBy == null || updatedBy.isBlank()) {
            updatedBy = "SYSTEM";
        }
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    public static ModerationRetentionPolicy of(String incidentType, int retentionDays) {
        return new ModerationRetentionPolicy(
                UUID.randomUUID(),
                incidentType,
                "DEFAULT",
                retentionDays,
                OffsetDateTime.now(),
                "SYSTEM",
                1L
        );
    }
}
