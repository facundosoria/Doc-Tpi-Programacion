package ar.edu.utn.frc.tup.piv.llm.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

class EventOutboxRelayPublishPendingTest {

  @Test
  void publishPendingPublishesRowsWithTheirCorrelationHeadersAndMarksThemPublished() {
    var outbox = mock(EventOutboxRepository.class);
    KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    when(kafka.send(any(Message.class)))
        .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));
    UUID idOne = UUID.randomUUID();
    UUID idTwo = UUID.randomUUID();
    when(outbox.findUnpublished(anyInt())).thenReturn(List.of(
        new EventOutboxRepository.OutboxRow(idOne, KafkaTopics.EVALUATION_EVENTS, "k1", "SCORE_CALCULATED",
            "llm-service", OffsetDateTime.now(), "{\"attemptId\":\"a-1\",\"score\":77}", "req-1", "tp-1", 0),
        new EventOutboxRepository.OutboxRow(idTwo, KafkaTopics.EVALUATION_EVENTS, "k2", "SCORE_DEFERRED",
            "llm-service", OffsetDateTime.now(), "{\"attemptId\":\"a-2\"}", null, null, 0)));
    var relay = new EventOutboxRelay(outbox, kafka, new ObjectMapper());

    relay.publishPending();

    ArgumentCaptor<Message<String>> captor = ArgumentCaptor.forClass(Message.class);
    verify(kafka, times(2)).send(captor.capture());
    Message<String> first = captor.getAllValues().get(0);
    Message<String> second = captor.getAllValues().get(1);
    assertThat(first.getHeaders().get(KafkaHeaders.TOPIC)).isEqualTo(KafkaTopics.EVALUATION_EVENTS);
    assertThat(first.getHeaders().get(KafkaHeaders.KEY)).isEqualTo("k1");
    assertThat(first.getHeaders().get("eventId")).isEqualTo(idOne.toString());
    assertThat(first.getHeaders().get("eventType")).isEqualTo("SCORE_CALCULATED");
    assertThat(first.getHeaders().get("traceparent")).isEqualTo("tp-1");
    assertThat(first.getHeaders().get("X-Request-Id")).isEqualTo("req-1");
    assertThat(second.getHeaders().get("traceparent")).isEqualTo("");
    assertThat(second.getHeaders().get("X-Request-Id")).isEqualTo("");
    verify(outbox).markPublished(idOne);
    verify(outbox).markPublished(idTwo);
  }

  @Test
  void publishPendingDoesNothingWhenTheOutboxIsEmpty() {
    var outbox = mock(EventOutboxRepository.class);
    KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    when(outbox.findUnpublished(anyInt())).thenReturn(List.of());
    var relay = new EventOutboxRelay(outbox, kafka, new ObjectMapper());

    relay.publishPending();

    verify(kafka, never()).send(any(Message.class));
    verify(outbox, never()).markPublished(any(UUID.class));
  }

  @Test
  void aFailedSendIsRecordedForTheNextPoll() {
    var outbox = mock(EventOutboxRepository.class);
    KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
    failed.completeExceptionally(new RuntimeException("broker down"));
    when(kafka.send(any(Message.class))).thenReturn(failed);
    UUID idOne = UUID.randomUUID();
    when(outbox.findUnpublished(anyInt())).thenReturn(List.of(
        new EventOutboxRepository.OutboxRow(idOne, KafkaTopics.EVALUATION_EVENTS, "k1", "SCORE_CALCULATED",
            "llm-service", OffsetDateTime.now(), "{\"attemptId\":\"a-1\"}", "req-1", "tp-1", 0)));
    var relay = new EventOutboxRelay(outbox, kafka, new ObjectMapper());

    relay.publishPending();

    verify(outbox).recordAttemptFailure(idOne);
    verify(outbox, never()).markPublished(any(UUID.class));
  }
}