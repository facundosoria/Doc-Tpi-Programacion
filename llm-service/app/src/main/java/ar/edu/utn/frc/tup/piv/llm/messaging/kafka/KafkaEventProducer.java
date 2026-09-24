package ar.edu.utn.frc.tup.piv.llm.messaging.kafka;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Productor Kafka reutilizable (CA1 de LLM-EP01-H07): cualquier módulo que necesite publicar un
 * evento llama a {@link #enqueue} dentro de su propia transacción de negocio; el evento queda en
 * el outbox con su correlación (requestId/traceparent tomados del MDC, ver las claves
 * {@code requestId}/{@code traceId} que puebla el filtro de correlación de {@code api})
 * y {@link EventOutboxRelay} lo publica de forma asíncrona.
 *
 * <p>Por regla del contrato ({@code .skill-hub/kafka-event-contract-rules.md}), nadie debe llamar
 * a {@code KafkaTemplate#send} directamente desde lógica de negocio — siempre a través de este
 * componente, para no perder atomicidad entre el cambio de estado y la publicación del evento.
 */
@Component
public class KafkaEventProducer {

  private final EventOutboxRepository outboxRepository;
  private final String producerId;

  public KafkaEventProducer(EventOutboxRepository outboxRepository,
      org.springframework.core.env.Environment environment) {
    this.outboxRepository = outboxRepository;
    this.producerId = environment.getProperty("spring.application.name", "llm-service");
  }

  /**
   * Encola un evento para publicación asíncrona.
   *
   * @param topic       topic por dominio (ver {@link KafkaTopics})
   * @param messageKey  Kafka Message Key del dominio (KAFKA_EVENT_STANDARD.md §6)
   * @param eventType   hecho del dominio en MAYÚSCULAS_CON_GUION_BAJO (§3)
   * @param payload     datos específicos del evento (serializados como JSON)
   */
  public <T> UUID enqueue(String topic, String messageKey, String eventType, T payload) {
    UUID eventId = UUID.randomUUID();
    EventEnvelope<T> envelope = new EventEnvelope<>(
        eventId, eventType, OffsetDateTime.now(), producerId, payload);
    String requestId = MDC.get("requestId");
    String traceId = MDC.get("traceId");
    outboxRepository.insert(envelope, topic, messageKey, requestId, traceId);
    return eventId;
  }
}
