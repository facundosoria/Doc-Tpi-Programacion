package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla 'moderation_retention_policies' en el esquema 'llm' (LLM-S13-H02 / T1).
 */
@Entity
@Table(name = "moderation_retention_policies", schema = "llm")
public class ModerationRetentionPolicyEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "incident_type", nullable = false, length = 50)
    private String incidentType;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", nullable = false, length = 255)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public ModerationRetentionPolicyEntity() {
    }

    public ModerationRetentionPolicyEntity(UUID id, String incidentType, String severity,
                                          int retentionDays, OffsetDateTime updatedAt,
                                          String updatedBy, long version) {
        this.id = id;
        this.incidentType = incidentType;
        this.severity = severity != null ? severity : "DEFAULT";
        this.retentionDays = retentionDays;
        this.updatedAt = updatedAt != null ? updatedAt : OffsetDateTime.now();
        this.updatedBy = updatedBy != null ? updatedBy : "SYSTEM";
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
