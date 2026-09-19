package ar.edu.utn.frc.tup.piv.llm.messaging.kafka;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Envelope común de la plataforma para eventos Kafka (KAFKA_EVENT_STANDARD.md §5):
 * {@code eventId, eventType, eventVersion, timestamp, producer, payload}. Estos campos son
 * la API pública entre microservicios y no deben renombrarse ni removerse.
 */
public record EventEnvelope<T>(
    UUID eventId,
    String eventType,
    int eventVersion,
    OffsetDateTime timestamp,
    String producer,
    T payload) {
}
