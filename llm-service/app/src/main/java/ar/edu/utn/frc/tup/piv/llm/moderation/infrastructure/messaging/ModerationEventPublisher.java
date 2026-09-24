package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.messaging;

import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.KafkaEventProducer;
import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.KafkaTopics;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionDomainEvent;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationEventPublisherPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura para publicar eventos de dominio de moderación (LLM-S12-H02 / T4,
 * LLM-EP01-H07). Publica eventos internos vía {@link ApplicationEventPublisher} de Spring (para
 * listeners locales, p. ej. auditoría/métricas), notifica al alumno de forma real a través de
 * {@link ModerationNotificationClient} (CA5 / LLM-S12-H02), y encola el evento de plataforma en el
 * outbox transaccional del topic {@code moderation-events} para publicación asíncrona a Kafka
 * ({@link KafkaEventProducer} / {@code EventOutboxRelay}).
 *
 * <p>Message Key Kafka: {@code courseId} — preserva el orden relativo de los eventos de moderación
 * dentro de un mismo curso (KAFKA_EVENT_STANDARD.md §6).
 */
@Component
public class ModerationEventPublisher implements ModerationEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(ModerationEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ModerationNotificationClient notificationClient;
    private final KafkaEventProducer kafkaEventProducer;

    public ModerationEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            ModerationNotificationClient notificationClient,
            KafkaEventProducer kafkaEventProducer) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.notificationClient = notificationClient;
        this.kafkaEventProducer = kafkaEventProducer;
    }

    @Override
    public void publishMessageUnblocked(ModerationResolutionDomainEvent event) {
        log.info("Publicando evento de dominio '{}' [eventId={}, messageId={}, incidentId={}, courseId={}, resolvedBy={}, resolution={}]",
                event.getEventType(),
                event.getEventId(),
                event.getMessageId(),
                event.getIncidentId(),
                event.getCourseId(),
                event.getResolvedBy(),
                event.getResolution());

        applicationEventPublisher.publishEvent(event);
        notificationClient.notifyMessageUnblocked(event);
        enqueueKafkaEvent(event);
    }

    private void enqueueKafkaEvent(ModerationResolutionDomainEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", event.getMessageId());
        payload.put("incidentId", event.getIncidentId());
        payload.put("courseId", event.getCourseId());
        payload.put("userId", event.getUserId());
        payload.put("resolvedBy", event.getResolvedBy());
        payload.put("resolution", event.getResolution());
        payload.put("resolutionReason", event.getResolutionReason());

        kafkaEventProducer.enqueue(
                KafkaTopics.MODERATION_EVENTS,
                event.getCourseId(),
                event.getEventType(),
                payload);
    }
}
