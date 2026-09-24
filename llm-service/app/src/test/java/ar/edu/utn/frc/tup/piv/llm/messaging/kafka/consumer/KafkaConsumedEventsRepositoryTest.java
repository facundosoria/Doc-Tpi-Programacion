package ar.edu.utn.frc.tup.piv.llm.messaging.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.KafkaTopics;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

class KafkaConsumedEventsRepositoryTest {
  @Test
  void reservesAnEventWhenTheInsertSucceeds() {
    var jdbc = mock(JdbcTemplate.class);
    UUID eventId = UUID.randomUUID();
    var repository = new KafkaConsumedEventsRepository(jdbc);

    assertThat(repository.tryReserve(eventId, KafkaTopics.EVALUATION_EVENTS, "SCORE_CALCULATED", "llm-service"))
        .isTrue();
    verify(jdbc).update(anyString(), eq(eventId), eq(KafkaTopics.EVALUATION_EVENTS),
        eq("SCORE_CALCULATED"), eq("llm-service"));
  }

  @Test
  void rejectsADuplicateEventReservation() {
    var jdbc = mock(JdbcTemplate.class);
    UUID eventId = UUID.randomUUID();
    when(jdbc.update(anyString(), any(), any(), any(), any())).thenThrow(new DuplicateKeyException("dup"));
    var repository = new KafkaConsumedEventsRepository(jdbc);

    assertThat(repository.tryReserve(eventId, KafkaTopics.EVALUATION_EVENTS, "SCORE_CALCULATED", "llm-service"))
        .isFalse();
  }
}