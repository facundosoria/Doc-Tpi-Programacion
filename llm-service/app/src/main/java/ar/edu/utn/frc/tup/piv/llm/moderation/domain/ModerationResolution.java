package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad raíz de dominio que representa la resolución inmutable de un incidente de moderación (LLM-S12-H02).
 * Desacoplada de frameworks de persistencia (ADR-019).
 */
public final class ModerationResolution {

    private final UUID id;
    private final UUID incidentId;
    private final String resolvedBy;
    private final ModerationResolutionType resolution;
    private final String resolutionReason;
    private final OffsetDateTime resolvedAt;

    public ModerationResolution(UUID id,
                                UUID incidentId,
                                String resolvedBy,
                                ModerationResolutionType resolution,
                                String resolutionReason,
                                OffsetDateTime resolvedAt) {
        this.id = Objects.requireNonNull(id, "id no puede ser nulo.");
        this.incidentId = Objects.requireNonNull(incidentId, "incident_id no puede ser nulo.");
        if (resolvedBy == null || resolvedBy.isBlank()) {
            throw new IllegalArgumentException("resolved_by no puede ser nulo ni vacío.");
        }
        this.resolvedBy = resolvedBy.trim();
        this.resolution = Objects.requireNonNull(resolution, "resolution no puede ser nulo.");
        if (resolutionReason != null && (resolutionReason.trim().length() < 20 || resolutionReason.trim().length() > 500)) {
            throw new IllegalArgumentException("resolution_reason debe tener entre 20 y 500 caracteres.");
        }
        this.resolutionReason = resolutionReason != null ? resolutionReason.trim() : null;
        this.resolvedAt = resolvedAt != null ? resolvedAt : OffsetDateTime.now();
    }

    public boolean isPurged() {
        return this.resolutionReason == null;
    }

    public ModerationResolution purge() {
        return new ModerationResolution(
                this.id,
                this.incidentId,
                this.resolvedBy,
                this.resolution,
                null,
                this.resolvedAt
        );
    }

    public static ModerationResolution create(UUID incidentId,
                                              String resolvedBy,
                                              ModerationResolutionType resolution,
                                              String resolutionReason) {
        return new ModerationResolution(
                UUID.randomUUID(),
                incidentId,
                resolvedBy,
                resolution,
                resolutionReason,
                OffsetDateTime.now()
        );
    }

    public boolean isReversed() {
        return this.resolution == ModerationResolutionType.REVERSED;
    }

    public boolean isConfirmed() {
        return this.resolution == ModerationResolutionType.CONFIRMED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public ModerationResolutionType getResolution() {
        return resolution;
    }

    public String getResolutionReason() {
        return resolutionReason;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModerationResolution that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(incidentId, that.incidentId)
                && Objects.equals(resolvedBy, that.resolvedBy)
                && resolution == that.resolution
                && Objects.equals(resolutionReason, that.resolutionReason)
                && Objects.equals(resolvedAt, that.resolvedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, incidentId, resolvedBy, resolution, resolutionReason, resolvedAt);
    }

    @Override
    public String toString() {
        return "ModerationResolution{" +
                "id=" + id +
                ", incidentId=" + incidentId +
                ", resolvedBy='" + resolvedBy + '\'' +
                ", resolution=" + resolution +
                ", resolvedAt=" + resolvedAt +
                '}';
    }
}
