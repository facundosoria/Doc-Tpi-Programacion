package ar.edu.utn.frc.tup.piv.llm.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class EventOutboxRepositoryTest {

  @Test
  void insertWritesTheEnvelopeAndItsCorrelationToTheOutbox() {
    var jdbc = mock(JdbcTemplate.class);
    var repository = new EventOutboxRepository(jdbc, new ObjectMapper());
    UUID eventId = UUID.randomUUID();
    OffsetDateTime occurredAt = OffsetDateTime.parse("2026-09-19T15:00:00Z");
    var envelope = new EventEnvelope<>(eventId, "SCORE_CALCULATED", occurredAt, "llm-service",
        Map.of("attemptId", "a-1"));

    repository.insert(envelope, KafkaTopics.EVALUATION_EVENTS, "k1", "req-1", "tp-1");

    verify(jdbc).update(contains("insert into llm.event_outbox"),
        eq(eventId), eq(KafkaTopics.EVALUATION_EVENTS), eq("k1"), eq("SCORE_CALCULATED"),
        eq("llm-service"), any(Timestamp.class), contains("\"attemptId\":\"a-1\""), eq("req-1"), eq("tp-1"));
  }

  @Test
  void insertFailsWhenThePayloadCannotBeSerialized() throws Exception {
    var jdbc = mock(JdbcTemplate.class);
    var failing = mock(ObjectMapper.class);
    doThrow(new JsonProcessingException("boom") {}).when(failing).writeValueAsString(any());
    var repository = new EventOutboxRepository(jdbc, failing);
    var envelope = new EventEnvelope<>(UUID.randomUUID(), "SCORE_CALCULATED",
        OffsetDateTime.now(), "llm-service", Map.of("attemptId", "a-1"));

    assertThatThrownBy(() -> repository.insert(envelope, KafkaTopics.EVALUATION_EVENTS, "k1", null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No se pudo serializar el payload del evento");
  }
}