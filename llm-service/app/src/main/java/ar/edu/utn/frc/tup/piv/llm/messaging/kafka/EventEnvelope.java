package ar.edu.utn.frc.tup.piv.llm.messaging.kafka;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Envelope común de la plataforma para eventos Kafka (KAFKA_EVENT_STANDARD.md §2, del PDF
 * {@code KAFKA.pdf}): {@code eventId, eventType, timestamp, producer, payload}. Sin
 * {@code eventVersion}. Estos campos son la API pública entre microservicios y no deben
 * renombrarse, removerse ni ampliarse.
 */
public record EventEnvelope<T>(
    UUID eventId,
    String eventType,
    OffsetDateTime timestamp,
    String producer,
    T payload) {
}
