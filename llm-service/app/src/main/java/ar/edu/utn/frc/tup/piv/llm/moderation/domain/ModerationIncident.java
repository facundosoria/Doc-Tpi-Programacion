package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Representación en el dominio de un incidente de moderación (LLM-S11-H01 / LLM-S12-H01).
 */
public final class ModerationIncident {

    private final UUID id;
    private final String messageId;
    private final String userId;
    private final String courseId;
    private final String status;
    private final String reasonCode;
    private final String messagePreview;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime purgedAt;

    public ModerationIncident(UUID id,
                              String messageId,
                              String userId,
                              String courseId,
                              String status,
                              String reasonCode,
                              String messagePreview,
                              OffsetDateTime createdAt) {
        this(id, messageId, userId, courseId, status, reasonCode, messagePreview, createdAt, null);
    }

    public ModerationIncident(UUID id,
                              String messageId,
                              String userId,
                              String courseId,
                              String status,
                              String reasonCode,
                              String messagePreview,
                              OffsetDateTime createdAt,
                              OffsetDateTime purgedAt) {
        this.id = Objects.requireNonNull(id, "id no puede ser nulo.");
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("user_id no puede ser nulo ni vacío.");
        }
        this.userId = userId.trim();
        this.messageId = messageId;
        this.courseId = courseId;
        this.status = status != null ? status.trim() : "BLOCK";
        this.reasonCode = reasonCode;
        this.messagePreview = messagePreview;
        this.createdAt = createdAt != null ? createdAt : OffsetDateTime.now();
        this.purgedAt = purgedAt;
    }

    public static ModerationIncident ofBlock(UUID id, String messageId, String userId, String courseId, String reasonCode, String preview) {
        return new ModerationIncident(id, messageId, userId, courseId, "BLOCK", reasonCode, preview, OffsetDateTime.now(), null);
    }

    public boolean isBlock() {
        return "BLOCK".equalsIgnoreCase(this.status);
    }

    public boolean isResolved() {
        return "CONFIRMED".equalsIgnoreCase(this.status) || "REVERSED".equalsIgnoreCase(this.status);
    }

    public boolean isPurged() {
        return this.purgedAt != null;
    }

    public ModerationIncident purge() {
        return new ModerationIncident(
                this.id,
                this.messageId,
                this.userId,
                this.courseId,
                this.status,
                this.reasonCode,
                null,
                this.createdAt,
                OffsetDateTime.now()
        );
    }

    public ModerationIncident resolve(String resolution) {
        return new ModerationIncident(
                this.id,
                this.messageId,
                this.userId,
                this.courseId,
                resolution,
                this.reasonCode,
                this.messagePreview,
                this.createdAt,
                this.purgedAt
        );
    }

    public boolean isOwnedBy(String callerUserId) {
        return callerUserId != null && this.userId.equals(callerUserId.trim());
    }

    public UUID getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getUserId() {
        return userId;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getStatus() {
        return status;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getMessagePreview() {
        return messagePreview;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getPurgedAt() {
        return purgedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModerationIncident that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(messageId, that.messageId)
                && Objects.equals(userId, that.userId)
                && Objects.equals(courseId, that.courseId)
                && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, messageId, userId, courseId, status);
    }
}
