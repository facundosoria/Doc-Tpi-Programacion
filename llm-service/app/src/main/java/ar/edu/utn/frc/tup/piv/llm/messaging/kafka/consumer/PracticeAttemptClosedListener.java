package ar.edu.utn.frc.tup.piv.llm.messaging.kafka.consumer;

import ar.edu.utn.frc.tup.piv.llm.application.service.AttemptEvaluationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.AttemptEvaluationService.ClosedAttempt;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumidor idempotente (LLM-EP01-H07) de {@code practice-events} / eventType
 * {@code ATTEMPT_CLOSED}, que hoy publica practice-service (Tema 05). Valida el envelope común,
 * aplica CA2-CA4 (deduplicación + dead-letter en la tabla {@code event_dead_letter}) y por cada intento cerrado dispara la evaluación
 * ({@link AttemptEvaluationService}), que publica el score en {@code evaluation-events}.
 *
 * <p>Corre en una sola transacción: la reserva del {@code eventId} y el evento de score encolado en
 * el outbox se confirman juntos, así un fallo entre ambos no pierde el intento (el evento se
 * reintenta). Un evento mal formado no falla: queda en dead-letter (tabla {@code event_dead_letter}, no un tópico: los grupos no
 * pueden crear tópicos) y la reserva se conserva.
 *
 * <p><b>PENDIENTE tras la integración de 2026-09-21.</b> La rama {@code main} traía un consumidor
 * propio ({@code KafkaAttemptEventsListener}, tópico {@code intento_cerrado.v1}) que además de
 * evaluar hacía dos cosas que este consumidor no hace: bloqueaba la asignación de calibración del
 * desafío en el primer intento ({@code ChallengeCalibrationAssignmentRepository#lockOnFirstAttempt})
 * y encolaba la evaluación pendiente cuando el evaluador no estaba disponible
 * ({@code EvaluationAvailabilityService#queueWhenUnavailable}). Se descartó ese consumidor porque
 * usaba el estándar de eventos retirado, pero <b>ambos comportamientos quedaron sin disparador</b>:
 * necesitan {@code challengeId}, que el {@code ATTEMPT_CLOSED} acordado con Tema 05 todavía no
 * incluye. Hay que cerrarlo con Tema 05 antes de habilitar Kafka en la plataforma
 * ({@code llm.kafka.enabled}). Las clases {@code EvaluationAvailabilityService} y
 * {@code lockOnFirstAttempt} siguen en el código con sus pruebas.
 *
 * <p><b># fixture provisorio</b> — el contrato real de "intento cerrado" todavía converge con Tema
 * 05 (ver {@code docs/historias/ep-01/h07.md} §"Estrategia de autonomía"). Se leen solo los campos
 * del AsyncAPI ({@code attemptId, courseCohortId, learnerId, transcript}); no se inventan otros.
 */
@Component
@ConditionalOnProperty(prefix = "llm.kafka", name = "enabled", havingValue = "true")
public class PracticeAttemptClosedListener {

  private static final Logger log = LoggerFactory.getLogger(PracticeAttemptClosedListener.class);
  private static final String CONSUMER_GROUP = "llm-service";
  static final String ATTEMPT_CLOSED = "ATTEMPT_CLOSED";

  private final KafkaConsumedEventsRepository consumedEvents;
  private final DeadLetterPublisher deadLetterPublisher;
  private final AttemptEvaluationService evaluation;
  private final ObjectMapper mapper;

  public PracticeAttemptClosedListener(KafkaConsumedEventsRepository consumedEvents,
      DeadLetterPublisher deadLetterPublisher, AttemptEvaluationService evaluation, ObjectMapper mapper) {
    this.consumedEvents = consumedEvents;
    this.deadLetterPublisher = deadLetterPublisher;
    this.evaluation = evaluation;
    this.mapper = mapper;
  }

  @KafkaListener(topics = KafkaTopics.PRACTICE_EVENTS, groupId = CONSUMER_GROUP)
  @Transactional
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

    if (!ATTEMPT_CLOSED.equals(eventType)) {
      log.info("Evento ignorado [eventId={}, eventType={}]", eventId, eventType);
      return;
    }
    ClosedAttempt attempt = closedAttempt(envelope.path("payload"));
    if (attempt == null) {
      deadLetterPublisher.send(KafkaTopics.PRACTICE_EVENTS, key, rawValue,
          "ATTEMPT_CLOSED sin attemptId, courseCohortId, learnerId o transcript válidos");
      return;
    }
    evaluation.evaluate(attempt);
    log.info("Evento procesado [eventId={}, eventType={}, attemptId={}]", eventId, eventType, attempt.attemptId());
  }

  /** @return el intento cerrado, o null si el payload no trae los cuatro campos del contrato. */
  private static ClosedAttempt closedAttempt(JsonNode payload) {
    try {
      JsonNode transcript = payload.path("transcript");
      if (!transcript.isArray()) {
        return null;
      }
      return new ClosedAttempt(uuid(payload, "attemptId"), uuid(payload, "courseCohortId"), uuid(payload, "learnerId"),
          transcript);
    } catch (IllegalArgumentException notUuid) {
      return null;
    }
  }

  private static UUID uuid(JsonNode payload, String field) {
    return UUID.fromString(payload.path(field).asText(""));
  }
}
