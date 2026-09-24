package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad raíz de dominio que representa una apelación de moderación (LLM-S12-H01).
 * Totalmente desacoplada de Spring y frameworks de persistencia (ADR-019).
 */
public final class ModerationAppeal {

    private final UUID id;
    private final UUID incidentId;
    private final String userId;
    private final String appealReason;
    private final ModerationAppealStatus status;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;

    public ModerationAppeal(UUID id,
                            UUID incidentId,
                            String userId,
                            String appealReason,
                            ModerationAppealStatus status,
                            OffsetDateTime createdAt,
                            OffsetDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id no puede ser nulo.");
        this.incidentId = Objects.requireNonNull(incidentId, "incident_id no puede ser nulo.");
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user_id no puede ser nulo ni vacío.");
        }
        this.userId = userId.trim();

        if (appealReason != null && (appealReason.trim().length() < 20 || appealReason.trim().length() > 1000)) {
            throw new IllegalArgumentException("appeal_reason debe tener entre 20 y 1000 caracteres.");
        }
        this.appealReason = appealReason != null ? appealReason.trim() : null;
        this.status = Objects.requireNonNull(status, "status no puede ser nulo.");
        this.createdAt = Objects.requireNonNull(createdAt, "created_at no puede ser nulo.");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updated_at no puede ser nulo.");
    }

    public boolean isPurged() {
        return this.appealReason == null;
    }

    public ModerationAppeal purge() {
        return new ModerationAppeal(
                this.id,
                this.incidentId,
                this.userId,
                null,
                this.status,
                this.createdAt,
                OffsetDateTime.now()
        );
    }

    public static ModerationAppeal create(UUID incidentId, String userId, String appealReason) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ModerationAppeal(
                UUID.randomUUID(),
                incidentId,
                userId,
                appealReason,
                ModerationAppealStatus.PENDING_REVIEW,
                now,
                now
        );
    }

    public ModerationAppeal confirm() {
        return new ModerationAppeal(
                this.id,
                this.incidentId,
                this.userId,
                this.appealReason,
                ModerationAppealStatus.CONFIRMED,
                this.createdAt,
                OffsetDateTime.now()
        );
    }

    public ModerationAppeal reverse() {
        return new ModerationAppeal(
                this.id,
                this.incidentId,
                this.userId,
                this.appealReason,
                ModerationAppealStatus.REVERSED,
                this.createdAt,
                OffsetDateTime.now()
        );
    }

    public boolean isOwnedBy(String callerUserId) {
        return callerUserId != null && this.userId.equals(callerUserId.trim());
    }

    public UUID getId() {
        return id;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public String getUserId() {
        return userId;
    }

    public String getAppealReason() {
        return appealReason;
    }

    public ModerationAppealStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModerationAppeal that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(incidentId, that.incidentId)
                && Objects.equals(userId, that.userId)
                && Objects.equals(appealReason, that.appealReason)
                && status == that.status
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, incidentId, userId, appealReason, status, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "ModerationAppeal{" +
                "id=" + id +
                ", incidentId=" + incidentId +
                ", userId='" + userId + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
