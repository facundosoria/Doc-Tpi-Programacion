package ar.edu.utn.frc.tup.piv.llm.messaging.kafka.consumer;

import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Ejemplo concreto del esqueleto de consumidor idempotente (LLM-EP01-H07), sobre el topic
 * {@code practice-events} / eventType {@code ATTEMPT-CLOSED} que hoy consume desde practice-service
 * (Tema 05).
 *
 * <p><b># fixture provisorio</b> — el contrato real de "intento cerrado" todavía converge con Tema
 * 05 (ver {@code docs/historias/ep-01/h07.md} §"Estrategia de autonomía"); este listener valida el
 * envelope común y aplica CA2-CA4 (deduplicación + dead-letter), pero el payload se trata como
 * opaco. Reemplazar la validación del payload cuando el schema real esté congelado — no inventar
 * campos mientras tanto.
 */
@Component
@ConditionalOnProperty(prefix = "llm.kafka", name = "enabled", havingValue = "true")
public class PracticeAttemptClosedListener {

  private static final Logger log = LoggerFactory.getLogger(PracticeAttemptClosedListener.class);
  private static final String CONSUMER_GROUP = "llm-service";

  private final KafkaConsumedEventsRepository consumedEvents;
  private final DeadLetterPublisher deadLetterPublisher;
  private final ObjectMapper mapper;

  public PracticeAttemptClosedListener(KafkaConsumedEventsRepository consumedEvents,
      DeadLetterPublisher deadLetterPublisher, ObjectMapper mapper) {
    this.consumedEvents = consumedEvents;
    this.deadLetterPublisher = deadLetterPublisher;
    this.mapper = mapper;
  }

  @KafkaListener(topics = KafkaTopics.PRACTICE_EVENTS, groupId = CONSUMER_GROUP)
  public void onMessage(@Payload String rawValue, @Header(value = "kafka_receivedMessageKey", required = false) String key) {
    JsonNode envelope;
    try {
      envelope = mapper.readTree(rawValue);
    } catch (Exception malformed) {
      deadLetterPublisher.send(KafkaTopics.PRACTICE_EVENTS, key, rawValue, "JSON malformado: " + malformed.getMessage());
      return;
    }

    JsonNode eventIdNode = envelope.get("eventId");
    if (eventIdNode == null || eventIdNode.isNull() || eventIdNode.asText().isBlank()) {
      deadLetterPublisher.send(KafkaTopics.PRACTICE_EVENTS, key, rawValue, "Evento sin eventId");
      return;
    }

    UUID eventId;
    try {
      eventId = UUID.fromString(eventIdNode.asText());
    } catch (IllegalArgumentException notUuid) {
      deadLetterPublisher.send(KafkaTopics.PRACTICE_EVENTS, key, rawValue, "eventId no es un UUID válido");
      return;
    }

    String eventType = envelope.path("eventType").asText(null);
    boolean isNew = consumedEvents.tryReserve(eventId, KafkaTopics.PRACTICE_EVENTS, eventType, CONSUMER_GROUP);
    if (!isNew) {
      log.info("Evento duplicado ignorado [eventId={}, eventType={}]", eventId, eventType);
      return;
    }

    // El efecto de negocio real (registrar el cierre de intento) se agrega cuando el contrato de
    // Tema 05 esté congelado; por ahora el esqueleto solo garantiza dedup + no bloqueo de partición.
    log.info("Evento procesado [eventId={}, eventType={}]", eventId, eventType);
  }
}
