package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla 'moderation_resolutions' en el esquema 'llm'.
 */
@Entity
@Table(name = "moderation_resolutions", schema = "llm")
public class ModerationResolutionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "incident_id", nullable = false, unique = true)
    private UUID incidentId;

    @Column(name = "resolved_by", nullable = false)
    private String resolvedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", nullable = false, length = 50)
    private ModerationResolutionType resolution;

    @Column(name = "resolution_reason", nullable = false, length = 500)
    private String resolutionReason;

    @Column(name = "resolved_at", nullable = false, updatable = false)
    private OffsetDateTime resolvedAt;

    public ModerationResolutionEntity() {
    }

    public ModerationResolutionEntity(UUID id, UUID incidentId, String resolvedBy,
                                    ModerationResolutionType resolution, String resolutionReason,
                                    OffsetDateTime resolvedAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.resolvedBy = resolvedBy;
        this.resolution = resolution;
        this.resolutionReason = resolutionReason;
        this.resolvedAt = resolvedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(UUID incidentId) {
        this.incidentId = incidentId;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public ModerationResolutionType getResolution() {
        return resolution;
    }

    public void setResolution(ModerationResolutionType resolution) {
        this.resolution = resolution;
    }

    public String getResolutionReason() {
        return resolutionReason;
    }

    public void setResolutionReason(String resolutionReason) {
        this.resolutionReason = resolutionReason;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
