package ar.edu.utn.frc.tup.piv.llm.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;

class KafkaEventProducerTest {

  @Test
  void enqueueWritesTheEnvelopeWithTheMdcCorrelationToTheOutbox() {
    var outbox = mock(EventOutboxRepository.class);
    var environment = mock(Environment.class);
    when(environment.getProperty("spring.application.name", "llm-service")).thenReturn("llm-service");
    var producer = new KafkaEventProducer(outbox, environment);
    MDC.put("requestId", "req-1");
    MDC.put("traceId", "tp-1");
    try {
      UUID eventId = producer.enqueue(KafkaTopics.EVALUATION_EVENTS, "k1", "SCORE_CALCULATED",
          Map.of("attemptId", "a-1"));

      assertThat(eventId).isNotNull();
      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
      verify(outbox).insert(captor.capture(), eq(KafkaTopics.EVALUATION_EVENTS), eq("k1"), eq("req-1"), eq("tp-1"));
      EventEnvelope<?> envelope = captor.getValue();
      assertThat(envelope.eventId()).isEqualTo(eventId);
      assertThat(envelope.eventType()).isEqualTo("SCORE_CALCULATED");
      assertThat(envelope.producer()).isEqualTo("llm-service");
      assertThat(envelope.payload()).isEqualTo(Map.of("attemptId", "a-1"));
    } finally {
      MDC.clear();
    }
  }

  @Test
  void enqueueWritesNullCorrelationWhenTheMdcIsEmpty() {
    var outbox = mock(EventOutboxRepository.class);
    var environment = mock(Environment.class);
    when(environment.getProperty("spring.application.name", "llm-service")).thenReturn("llm-service");
    var producer = new KafkaEventProducer(outbox, environment);

    producer.enqueue(KafkaTopics.EVALUATION_EVENTS, "k1", "SCORE_CALCULATED", Map.of("attemptId", "a-1"));

    verify(outbox).insert(any(EventEnvelope.class), eq(KafkaTopics.EVALUATION_EVENTS), eq("k1"), eq(null), eq(null));
  }
}