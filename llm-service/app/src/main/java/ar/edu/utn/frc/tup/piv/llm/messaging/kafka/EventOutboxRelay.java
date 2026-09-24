package ar.edu.utn.frc.tup.piv.llm.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relay asíncrono del outbox (CA1 de LLM-EP01-H07): hace polling de eventos pendientes y los
 * publica a Kafka con {@code traceparent}/{@code X-Request-Id} como headers del mensaje (nunca en
 * el body, {@code .skill-hub/request-correlation-across-http-and-kafka.md}).
 *
 * <p>Deshabilitado por defecto ({@code llm.kafka.enabled=false}) porque todavía no hay un broker
 * operado por la cátedra para S1 — activar con {@code LLM_KAFKA_ENABLED=true} cuando el servicio
 * `kafka` de compose esté levantado.
 */
@Component
@ConditionalOnProperty(prefix = "llm.kafka", name = "enabled", havingValue = "true")
public class EventOutboxRelay {

  private static final Logger log = LoggerFactory.getLogger(EventOutboxRelay.class);
  private static final int BATCH_SIZE = 50;

  private final EventOutboxRepository outboxRepository;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper mapper;

  public EventOutboxRelay(EventOutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
      ObjectMapper mapper) {
    this.outboxRepository = outboxRepository;
    this.kafkaTemplate = kafkaTemplate;
    this.mapper = mapper;
  }

  @Scheduled(fixedDelayString = "${llm.kafka.outbox-poll-ms:2000}")
  public void publishPending() {
    List<EventOutboxRepository.OutboxRow> pending = outboxRepository.findUnpublished(BATCH_SIZE);
    for (EventOutboxRepository.OutboxRow row : pending) {
      publishOne(row);
    }
  }

  private void publishOne(EventOutboxRepository.OutboxRow row) {
    Message<String> message = MessageBuilder
        .withPayload(envelopeJson(row, mapper))
        .setHeader(KafkaHeaders.TOPIC, row.topic())
        .setHeader(KafkaHeaders.KEY, row.messageKey())
        .setHeader("eventId", row.eventId().toString())
        .setHeader("eventType", row.eventType())
        .setHeader("traceparent", row.traceparent() == null ? "" : row.traceparent())
        .setHeader("X-Request-Id", row.requestId() == null ? "" : row.requestId())
        .build();

    CompletableFuture<?> future = kafkaTemplate.send(message);
    future.whenComplete((result, exception) -> {
      if (exception == null) {
        outboxRepository.markPublished(row.eventId());
        log.info("Evento publicado [eventId={}, topic={}, eventType={}]", row.eventId(), row.topic(), row.eventType());
      } else {
        outboxRepository.recordAttemptFailure(row.eventId());
        log.warn("No se pudo publicar el evento [eventId={}, topic={}], reintentará en el próximo poll",
            row.eventId(), row.topic(), exception);
      }
    });
  }

  /** El cuerpo del mensaje es el envelope completo del estándar (KAFKA_EVENT_STANDARD §2):
   * {@code eventId, eventType, timestamp, producer, payload}, sin {@code eventVersion}. Los headers repiten
   * {@code eventId}/{@code eventType} para filtrar sin deserializar. */
  static String envelopeJson(EventOutboxRepository.OutboxRow row, ObjectMapper mapper) {
    try {
      ObjectNode envelope = mapper.createObjectNode();
      envelope.put("eventId", row.eventId().toString());
      envelope.put("eventType", row.eventType());
      envelope.put("timestamp", row.occurredAt().toInstant().toString());
      envelope.put("producer", row.producer());
      envelope.set("payload", mapper.readTree(row.payloadJson()));
      return mapper.writeValueAsString(envelope);
    } catch (JsonProcessingException invalid) {
      throw new IllegalStateException("El payload del evento " + row.eventId() + " no es JSON válido", invalid);
    }
  }
}
