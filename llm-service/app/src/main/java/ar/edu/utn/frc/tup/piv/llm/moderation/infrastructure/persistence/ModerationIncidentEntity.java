package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA mapeada a la tabla 'moderation_incidents' en el esquema 'llm'.
 */
@Entity
@Table(name = "moderation_incidents", schema = "llm")
public class ModerationIncidentEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "course_id")
    private String courseId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "reason_code", length = 100)
    private String reasonCode;

    @Column(name = "message_preview", columnDefinition = "TEXT")
    private String messagePreview;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "purged_at")
    private OffsetDateTime purgedAt;

    public ModerationIncidentEntity() {
    }

    public ModerationIncidentEntity(UUID id, String messageId, String userId, String courseId,
                                  String status, String reasonCode, String messagePreview, OffsetDateTime createdAt) {
        this(id, messageId, userId, courseId, status, reasonCode, messagePreview, createdAt, null);
    }

    public ModerationIncidentEntity(UUID id, String messageId, String userId, String courseId,
                                  String status, String reasonCode, String messagePreview,
                                  OffsetDateTime createdAt, OffsetDateTime purgedAt) {
        this.id = id;
        this.messageId = messageId;
        this.userId = userId;
        this.courseId = courseId;
        this.status = status;
        this.reasonCode = reasonCode;
        this.messagePreview = messagePreview;
        this.createdAt = createdAt;
        this.purgedAt = purgedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getMessagePreview() {
        return messagePreview;
    }

    public void setMessagePreview(String messagePreview) {
        this.messagePreview = messagePreview;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getPurgedAt() {
        return purgedAt;
    }

    public void setPurgedAt(OffsetDateTime purgedAt) {
        this.purgedAt = purgedAt;
    }
}
