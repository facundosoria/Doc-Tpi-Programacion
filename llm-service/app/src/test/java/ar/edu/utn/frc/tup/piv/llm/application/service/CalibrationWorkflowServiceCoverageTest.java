package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationStateMachine.CalibrationState;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalibrationWorkflowServiceCoverageTest {

  @Test void startsAndFinishesRunsThroughValidTransitions() {
    var store = mock(CalibrationWorkflowService.CalibrationWorkflowStore.class);
    var service = new CalibrationWorkflowService(store);
    UUID runId = UUID.randomUUID();
    when(store.transition(runId, CalibrationState.QUEUED, CalibrationState.RUNNING)).thenReturn(true);
    when(store.transition(runId, CalibrationState.RUNNING, CalibrationState.PASSED)).thenReturn(true);
    when(store.transition(runId, CalibrationState.RUNNING, CalibrationState.FAILED)).thenReturn(true);

    service.start(runId);
    service.finish(runId, true);
    service.finish(runId, false);

    verify(store).transition(runId, CalibrationState.QUEUED, CalibrationState.RUNNING);
    verify(store, times(1)).transition(runId, CalibrationState.RUNNING, CalibrationState.PASSED);
    verify(store, times(1)).transition(runId, CalibrationState.RUNNING, CalibrationState.FAILED);
  }

  @Test void rejectsTransitionsRejectedByTheStore() {
    var store = mock(CalibrationWorkflowService.CalibrationWorkflowStore.class);
    var service = new CalibrationWorkflowService(store);
    when(store.transition(any(), any(), any())).thenReturn(false);
    assertThatThrownBy(() -> service.start(UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> service.finish(UUID.randomUUID(), true)).isInstanceOf(IllegalStateException.class);
  }

  @Test void activatesAPassedCalibrationWithinTheCourse() {
    var store = mock(CalibrationWorkflowService.CalibrationWorkflowStore.class);
    var service = new CalibrationWorkflowService(store);
    UUID course = UUID.randomUUID(), run = UUID.randomUUID();
    when(store.isPassed(course, run)).thenReturn(true);
    CallerIdentity actor = new CallerIdentity("llm-service", UUID.randomUUID(), "r", "t");
    service.activate(course, run, actor);
    verify(store).activate(course, run, actor);
  }

  @Test void rejectsActivationOfAnUnpassedCalibration() {
    var store = mock(CalibrationWorkflowService.CalibrationWorkflowStore.class);
    var service = new CalibrationWorkflowService(store);
    when(store.isPassed(any(), any())).thenReturn(false);
    assertThatThrownBy(() -> service.activate(UUID.randomUUID(), UUID.randomUUID(),
        new CallerIdentity("llm-service", UUID.randomUUID(), null, null)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test void queuesOnlyWhenNoValidCalibrationExists() {
    var store = mock(CalibrationWorkflowService.CalibrationWorkflowStore.class);
    var service = new CalibrationWorkflowService(store);
    UUID attempt = UUID.randomUUID(), challenge = UUID.randomUUID(), key = UUID.randomUUID();
    when(store.hasValidCalibration(challenge)).thenReturn(false);
    service.queue(attempt, challenge, key);
    verify(store).enqueue(attempt, challenge, key);

    when(store.hasValidCalibration(challenge)).thenReturn(true);
    service.queue(attempt, challenge, key);
    verify(store, times(1)).enqueue(attempt, challenge, key);
  }

  @Test void resumeNextReturnsFalseOnAnEmptyQueue() {
    var store = mock(CalibrationWorkflowService.CalibrationWorkflowStore.class);
    var service = new CalibrationWorkflowService(store);
    when(store.claimNextQueued()).thenReturn(Optional.empty());
    assertThat(service.resumeNext()).isFalse();
  }

  @Test void resumeNextSkipsItemsWithoutAValidCalibration() {
    var store = mock(CalibrationWorkflowService.CalibrationWorkflowStore.class);
    var service = new CalibrationWorkflowService(store);
    var item = new CalibrationWorkflowService.QueuedEvaluation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    when(store.claimNextQueued()).thenReturn(Optional.of(item));
    when(store.hasValidCalibration(item.challengeId())).thenReturn(false);
    assertThat(service.resumeNext()).isFalse();
    verify(store, never()).markRunning(item.id());
  }

  @Test void resumeNextAdvancesOnlyWhenMarkRunningSucceeds() {
    var store = mock(CalibrationWorkflowService.CalibrationWorkflowStore.class);
    var service = new CalibrationWorkflowService(store);
    var item = new CalibrationWorkflowService.QueuedEvaluation(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    when(store.claimNextQueued()).thenReturn(Optional.of(item));
    when(store.hasValidCalibration(item.challengeId())).thenReturn(true);
    when(store.markRunning(item.id())).thenReturn(true);
    assertThat(service.resumeNext()).isTrue();
    verify(store).markRunning(item.id());

    when(store.markRunning(item.id())).thenReturn(false);
    assertThat(service.resumeNext()).isFalse();
  }
}