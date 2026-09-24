package ar.edu.utn.frc.tup.piv.llm.messaging.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.application.service.AttemptEvaluationService;
import ar.edu.utn.frc.tup.piv.llm.application.service.AttemptEvaluationService.ClosedAttempt;
import ar.edu.utn.frc.tup.piv.llm.messaging.kafka.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PracticeAttemptClosedListenerTest {
  private final UUID eventId = UUID.randomUUID();
  private final UUID attemptId = UUID.randomUUID();
  private final UUID cohortId = UUID.randomUUID();
  private final UUID learnerId = UUID.randomUUID();

  private final KafkaConsumedEventsRepository consumed = mock(KafkaConsumedEventsRepository.class);
  private final DeadLetterPublisher deadLetter = mock(DeadLetterPublisher.class);
  private final AttemptEvaluationService evaluation = mock(AttemptEvaluationService.class);
  private final PracticeAttemptClosedListener listener =
      new PracticeAttemptClosedListener(consumed, deadLetter, evaluation, new ObjectMapper());

  @Test
  void aClosedAttemptIsEvaluatedOnceWithTheFieldsOfTheContract() {
    when(consumed.tryReserve(eq(eventId), eq(KafkaTopics.PRACTICE_EVENTS), eq("ATTEMPT_CLOSED"), anyString())).thenReturn(true);

    listener.onMessage(attemptClosed("ATTEMPT_CLOSED", validPayload()), "key");

    var attempt = ArgumentCaptor.forClass(ClosedAttempt.class);
    verify(evaluation).evaluate(attempt.capture());
    assertThat(attempt.getValue().attemptId()).isEqualTo(attemptId);
    assertThat(attempt.getValue().courseCohortId()).isEqualTo(cohortId);
    assertThat(attempt.getValue().learnerId()).isEqualTo(learnerId);
    assertThat(attempt.getValue().transcript()).hasSize(1);
    verify(deadLetter, never()).send(anyString(), any(), anyString(), anyString());
  }

  @Test
  void aDuplicatedEventIsNotEvaluatedAgain() {
    when(consumed.tryReserve(eq(eventId), anyString(), anyString(), anyString())).thenReturn(false);

    listener.onMessage(attemptClosed("ATTEMPT_CLOSED", validPayload()), "key");

    verify(evaluation, never()).evaluate(any());
  }

  @Test
  void otherEventTypesOnTheSameTopicAreReservedButNotEvaluated() {
    when(consumed.tryReserve(eq(eventId), anyString(), eq("ATTEMPT_STARTED"), anyString())).thenReturn(true);

    listener.onMessage(attemptClosed("ATTEMPT_STARTED", validPayload()), "key");

    verify(evaluation, never()).evaluate(any());
    verify(deadLetter, never()).send(anyString(), any(), anyString(), anyString());
  }

  @Test
  void anAttemptClosedWithoutTheContractFieldsGoesToDeadLetterInsteadOfBeingEvaluated() {
    when(consumed.tryReserve(eq(eventId), anyString(), anyString(), anyString())).thenReturn(true);

    listener.onMessage(attemptClosed("ATTEMPT_CLOSED", "{}"), "key");

    verify(evaluation, never()).evaluate(any());
    verify(deadLetter).send(eq(KafkaTopics.PRACTICE_EVENTS), eq("key"), anyString(), anyString());
  }

  @Test
  void aPayloadWithANonUuidIdOrANonArrayTranscriptGoesToDeadLetter() {
    when(consumed.tryReserve(eq(eventId), anyString(), anyString(), anyString())).thenReturn(true);

    listener.onMessage(attemptClosed("ATTEMPT_CLOSED", validPayload().replace(attemptId.toString(), "no-es-uuid")), "key");
    listener.onMessage(attemptClosed("ATTEMPT_CLOSED", validPayload().replace("[{\"role\":\"student\"}]", "\"texto\"")), "key");

    verify(evaluation, never()).evaluate(any());
    verify(deadLetter, org.mockito.Mockito.times(2)).send(eq(KafkaTopics.PRACTICE_EVENTS), eq("key"), anyString(), anyString());
  }

  @Test
  void malformedEventsStillGoToDeadLetterBeforeReservingAnything() {
    listener.onMessage("no es json {", "key");
    listener.onMessage("{\"noEventId\":true}", "key");

    verify(deadLetter, org.mockito.Mockito.times(2)).send(eq(KafkaTopics.PRACTICE_EVENTS), eq("key"), anyString(), anyString());
    verify(consumed, never()).tryReserve(any(), anyString(), anyString(), anyString());
    verify(evaluation, never()).evaluate(any());
  }

  private String attemptClosed(String eventType, String payload) {
    return "{\"eventId\":\"" + eventId + "\",\"eventType\":\"" + eventType + "\","
        + "\"producer\":\"practice-service\",\"payload\":" + payload + "}";
  }

  private String validPayload() {
    return "{\"attemptId\":\"" + attemptId + "\",\"courseCohortId\":\"" + cohortId + "\",\"learnerId\":\"" + learnerId
        + "\",\"transcript\":[{\"role\":\"student\"}]}";
  }
}
