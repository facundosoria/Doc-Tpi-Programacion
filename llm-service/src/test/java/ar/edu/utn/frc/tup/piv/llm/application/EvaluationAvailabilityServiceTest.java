package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.application.CalibrationWorkflowService.CalibrationWorkflowStore;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EvaluationAvailabilityServiceTest {

  @Test
  void queuesEvaluationWhenNoValidCalibrationExists() {
    var store = mock(CalibrationWorkflowStore.class);
    var service = new EvaluationAvailabilityService(store);
    UUID attemptId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();

    when(store.hasValidCalibration(challengeId)).thenReturn(false);

    boolean queued = service.queueWhenUnavailable(attemptId, challengeId, idempotencyKey);

    assertThat(queued).isTrue();
    verify(store).enqueue(attemptId, challengeId, idempotencyKey);
  }

  @Test
  void doesNotQueueEvaluationWhenValidCalibrationExists() {
    var store = mock(CalibrationWorkflowStore.class);
    var service = new EvaluationAvailabilityService(store);
    UUID attemptId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();

    when(store.hasValidCalibration(challengeId)).thenReturn(true);

    boolean queued = service.queueWhenUnavailable(attemptId, challengeId, idempotencyKey);

    assertThat(queued).isFalse();
    verify(store, never()).enqueue(attemptId, challengeId, idempotencyKey);
  }

  @Test
  void pendingEvaluationWorkerResumesAllEligibleQueuedEvaluations() {
    var workflow = mock(CalibrationWorkflowService.class);
    var worker = new PendingEvaluationWorker(workflow);

    when(workflow.resumeNext())
        .thenReturn(true)
        .thenReturn(true)
        .thenReturn(false);

    worker.resume();

    verify(workflow, org.mockito.Mockito.times(3)).resumeNext();
  }
}
