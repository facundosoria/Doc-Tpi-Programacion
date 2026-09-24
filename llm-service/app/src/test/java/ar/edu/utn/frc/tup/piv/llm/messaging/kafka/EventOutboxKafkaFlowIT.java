package ar.edu.utn.frc.tup.piv.llm.messaging.kafka;

import ar.edu.utn.frc.tup.piv.llm.it.AbstractIntegrationIT;
import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.consumer.KafkaConsumedEventsRepository;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cubre los criterios de aceptación de LLM-EP01-H07 sobre un broker Kafka embebido:
 * <ul>
 *   <li>CA1: {@link KafkaEventProducer} + {@link EventOutboxRelay} publican con eventId,
 *       eventType y correlación (traceparent/X-Request-Id) como headers, y sin eventVersion.</li>
 *   <li>CA2/CA3: {@link KafkaConsumedEventsRepository} reserva el eventId antes de procesar y
 *       reconoce duplicados sin repetir el efecto.</li>
 *   <li>CA4: un mensaje sin eventId (o malformado) queda en la tabla {@code event_dead_letter} (no hay
 *       tópico {@code .dlt}: los grupos no pueden crear tópicos) y no bloquea el consumo de los eventos
 *       siguientes.</li>
 *   <li>Un {@code ATTEMPT_CLOSED} válido se evalúa contra el evaluador {@code fake} sembrado y su
 *       {@code SCORE_CALCULATED} sale por {@code evaluation-events}, con la cohorte como key.</li>
 * </ul>
 */
@SpringBootTest(properties = {
    "llm.credentials.master-key=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
    "app.jwks-refresh-ms=3600000",
    "llm.kafka.enabled=true",
    "llm.kafka.outbox-poll-ms=200",
    "spring.task.scheduling.enabled=true",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = {"moderation-events", "practice-events", "evaluation-events"})
class EventOutboxKafkaFlowIT extends AbstractIntegrationIT {

  @Autowired
  private KafkaEventProducer kafkaEventProducer;
  @Autowired
  private KafkaConsumedEventsRepository consumedEventsRepository;
  @Autowired
  private JdbcTemplate jdbc;
  @Autowired
  private EmbeddedKafkaBroker embeddedKafkaBroker;

  private KafkaConsumer<String, String> testConsumer;
  private KafkaProducer<String, String> testProducer;

  @BeforeEach
  void setUpClients() {
    Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group-" + UUID.randomUUID(), "true", embeddedKafkaBroker);
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
    testConsumer = new KafkaConsumer<>(consumerProps);

    Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
    producerProps.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class);
    testProducer = new KafkaProducer<>(producerProps);
  }

  @AfterEach
  void tearDownClients() {
    testConsumer.close();
    testProducer.close();
    // event_outbox y kafka_consumed_events son append-only (triggers de V1/V35/V36); cada test usa
    // UUID.randomUUID() para no colisionar entre corridas, así que no hace falta (ni se puede)
    // limpiarlas acá.
  }

  @Test
  void producesEnvelopeWithCorrelationHeadersOnModerationEventsTopic() {
    MDC.put("requestId", "req-h07-1");
    MDC.put("traceId", "trace-h07-1");
    UUID eventId;
    try {
      eventId = kafkaEventProducer.enqueue(
          KafkaTopics.MODERATION_EVENTS, "curso-h07", "MESSAGE_UNBLOCKED",
          Map.of("messageId", "msg-h07"));
    } finally {
      MDC.clear();
    }

    testConsumer.subscribe(java.util.List.of(KafkaTopics.MODERATION_EVENTS));
    ConsumerRecord<String, String> record = pollUntilFound(testConsumer, eventId.toString());

    assertThat(record.key()).isEqualTo("curso-h07");
    assertThat(record.value()).contains("messageId").contains("msg-h07");
    // El cuerpo es el envelope completo del estándar; el payload va anidado, no solo.
    assertThat(record.value()).contains("\"eventId\":\"" + eventId + "\"").contains("\"eventType\":\"MESSAGE_UNBLOCKED\"")
        .contains("\"timestamp\"").contains("\"producer\"").contains("\"payload\":{");
    assertThat(headerValue(record, "eventId")).isEqualTo(eventId.toString());
    assertThat(headerValue(record, "eventType")).isEqualTo("MESSAGE_UNBLOCKED");
    assertThat(record.value()).doesNotContain("eventVersion");
    assertThat(record.headers().lastHeader("eventVersion")).as("no hay header eventVersion").isNull();
    assertThat(headerValue(record, "traceparent")).isEqualTo("trace-h07-1");
    assertThat(headerValue(record, "X-Request-Id")).isEqualTo("req-h07-1");

    waitUntil(() -> {
      Integer publishedCount = jdbc.queryForObject(
          "select count(*) from llm.event_outbox where event_id = ? and published_at is not null",
          Integer.class, eventId);
      return publishedCount != null && publishedCount == 1;
    });
  }

  @Test
  void consumerDeduplicatesReprocessedEventId() {
    UUID eventId = UUID.randomUUID();

    boolean firstReservation = consumedEventsRepository.tryReserve(eventId, "practice-events", "ATTEMPT_CLOSED", "llm-service");
    boolean secondReservation = consumedEventsRepository.tryReserve(eventId, "practice-events", "ATTEMPT_CLOSED", "llm-service");

    assertThat(firstReservation).isTrue();
    assertThat(secondReservation).isFalse();

    Integer rows = jdbc.queryForObject(
        "select count(*) from llm.kafka_consumed_events where event_id = ?", Integer.class, eventId);
    assertThat(rows).isEqualTo(1);
  }

  @Test
  void malformedEventWithoutEventIdIsRoutedToDeadLetterAndDoesNotBlockNextEvents() {
    String marker = "noEventId-" + UUID.randomUUID();
    testProducer.send(new ProducerRecord<>(KafkaTopics.PRACTICE_EVENTS, "curso-x", "{\"" + marker + "\":true}"));
    testProducer.flush();

    waitUntil(() -> {
      Integer rows = jdbc.queryForObject(
          "select count(*) from llm.event_dead_letter where source_topic = ? and raw_value like ? and reason = ?",
          Integer.class, KafkaTopics.PRACTICE_EVENTS, "%" + marker + "%", "Evento sin eventId");
      return rows != null && rows == 1;
    });

    // El próximo evento bien formado en el mismo topic se sigue consumiendo sin bloqueo (CA4).
    UUID nextEventId = UUID.randomUUID();
    String wellFormed = "{\"eventId\":\"" + nextEventId + "\",\"eventType\":\"ATTEMPT_CLOSED\","
        + "\"timestamp\":\"2026-09-20T15:00:00Z\",\"producer\":\"practice-service\",\"payload\":{}}";
    testProducer.send(new ProducerRecord<>(KafkaTopics.PRACTICE_EVENTS, "curso-x", wellFormed));
    testProducer.flush();

    waitUntil(() -> {
      Integer rows = jdbc.queryForObject(
          "select count(*) from llm.kafka_consumed_events where event_id = ?", Integer.class, nextEventId);
      return rows != null && rows == 1;
    });
  }

  @Test
  void aClosedAttemptIsEvaluatedByTheFakeAndItsScoreIsPublishedInEvaluationEvents() {
    UUID attemptId = UUID.randomUUID();
    UUID cohortId = UUID.randomUUID();
    String attemptClosed = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"ATTEMPT_CLOSED\","
        + "\"timestamp\":\"2026-09-20T15:00:00Z\",\"producer\":\"practice-service\",\"payload\":{\"attemptId\":\"" + attemptId + "\",\"courseCohortId\":\"" + cohortId
        + "\",\"learnerId\":\"" + UUID.randomUUID() + "\",\"transcript\":[{\"role\":\"student\",\"content\":\"no entiendo mi recursión\"}]}}";
    testProducer.send(new ProducerRecord<>(KafkaTopics.PRACTICE_EVENTS, cohortId.toString(), attemptClosed));
    testProducer.flush();

    testConsumer.subscribe(java.util.List.of(KafkaTopics.EVALUATION_EVENTS));
    ConsumerRecord<String, String> score = pollUntilAny(testConsumer);

    assertThat(score.key()).isEqualTo(cohortId.toString());
    assertThat(headerValue(score, "eventType")).isEqualTo("SCORE_CALCULATED");
    assertThat(score.value()).contains(attemptId.toString()).contains("\"score\"").contains("fake-evaluator-v1");
    assertThat(score.value()).contains("\"eventType\":\"SCORE_CALCULATED\"").contains("\"payload\":{");
  }

  private void waitUntil(java.util.function.BooleanSupplier condition) {
    long deadline = System.currentTimeMillis() + 10_000;
    while (System.currentTimeMillis() < deadline) {
      if (condition.getAsBoolean()) {
        return;
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AssertionError(e);
      }
    }
    throw new AssertionError("La condición esperada no se cumplió a tiempo");
  }

  private ConsumerRecord<String, String> pollUntilFound(KafkaConsumer<String, String> consumer, String eventId) {
    long deadline = System.currentTimeMillis() + 10_000;
    while (System.currentTimeMillis() < deadline) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
      for (ConsumerRecord<String, String> record : records) {
        if (headerValue(record, "eventId") != null && headerValue(record, "eventId").equals(eventId)) {
          return record;
        }
      }
    }
    throw new AssertionError("No se encontró el evento " + eventId + " en el topic");
  }

  private ConsumerRecord<String, String> pollUntilAny(KafkaConsumer<String, String> consumer) {
    long deadline = System.currentTimeMillis() + 10_000;
    while (System.currentTimeMillis() < deadline) {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
      if (!records.isEmpty()) {
        return records.iterator().next();
      }
    }
    throw new AssertionError("No llegó ningún mensaje al dead-letter topic");
  }

  private String headerValue(ConsumerRecord<String, String> record, String header) {
    org.apache.kafka.common.header.Header h = record.headers().lastHeader(header);
    return h == null ? null : new String(h.value(), java.nio.charset.StandardCharsets.UTF_8);
  }
}
