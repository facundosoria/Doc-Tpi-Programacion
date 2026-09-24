package ar.edu.utn.frc.tup.piv.llm.moderation.domain;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de dominio emitido cuando un incidente de moderación es revertido (falso positivo),
 * notificando a chat-service y notifications-service el desbloqueo del mensaje (LLM-S12-H02 / T4).
 * Sigue el envelope común de la plataforma (contratos-kafka).
 */
public final class ModerationResolutionDomainEvent {

    private final UUID eventId;
    private final String version;
    private final OffsetDateTime occurredAt;
    private final String producer;
    private final String eventType;
    private final String messageId;
    private final UUID incidentId;
    private final String courseId;
    private final String userId;
    private final String resolvedBy;
    private final String resolution;
    private final String resolutionReason;

    public ModerationResolutionDomainEvent(UUID eventId,
                                           String version,
                                           OffsetDateTime occurredAt,
                                           String producer,
                                           String eventType,
                                           String messageId,
                                           UUID incidentId,
                                           String courseId,
                                           String userId,
                                           String resolvedBy,
                                           String resolution,
                                           String resolutionReason) {
        this.eventId = Objects.requireNonNull(eventId, "eventId no puede ser nulo.");
        this.version = Objects.requireNonNull(version, "version no puede ser nula.");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt no puede ser nulo.");
        this.producer = Objects.requireNonNull(producer, "producer no puede ser nulo.");
        this.eventType = Objects.requireNonNull(eventType, "eventType no puede ser nulo.");
        this.messageId = messageId;
        this.incidentId = Objects.requireNonNull(incidentId, "incidentId no puede ser nulo.");
        this.courseId = courseId;
        this.userId = userId;
        this.resolvedBy = Objects.requireNonNull(resolvedBy, "resolvedBy no puede ser nulo.");
        this.resolution = Objects.requireNonNull(resolution, "resolution no puede ser nula.");
        this.resolutionReason = resolutionReason;
    }

    public static ModerationResolutionDomainEvent ofReversed(String messageId,
                                                             UUID incidentId,
                                                             String courseId,
                                                             String userId,
                                                             String resolvedBy,
                                                             String resolutionReason) {
        return new ModerationResolutionDomainEvent(
                UUID.randomUUID(),
                "1.0",
                OffsetDateTime.now(),
                "llm-service",
                "MESSAGE_UNBLOCKED",
                messageId,
                incidentId,
                courseId,
                userId,
                resolvedBy,
                "REVERSED",
                resolutionReason
        );
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getVersion() {
        return version;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getProducer() {
        return producer;
    }

    public String getEventType() {
        return eventType;
    }

    public String getMessageId() {
        return messageId;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public String getCourseId() {
        return courseId;
    }

    public String getUserId() {
        return userId;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public String getResolution() {
        return resolution;
    }

    public String getResolutionReason() {
        return resolutionReason;
    }

    @Override
    public String toString() {
        return "ModerationResolutionDomainEvent{" +
                "eventId=" + eventId +
                ", version='" + version + '\'' +
                ", occurredAt=" + occurredAt +
                ", producer='" + producer + '\'' +
                ", eventType='" + eventType + '\'' +
                ", messageId='" + messageId + '\'' +
                ", incidentId=" + incidentId +
                ", courseId='" + courseId + '\'' +
                ", userId='" + userId + '\'' +
                ", resolvedBy='" + resolvedBy + '\'' +
                ", resolution='" + resolution + '\'' +
                '}';
    }
}
