package ar.edu.utn.frc.tup.piv.llm.application.worker;

import ar.edu.utn.frc.tup.piv.llm.application.service.CalibrationWorkflowService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PendingEvaluationWorkerTest {
  @Test void resumesUntilNoPendingEvaluationRemains() {
    var workflow = mock(CalibrationWorkflowService.class);
    when(workflow.resumeNext()).thenReturn(true, false);

    new PendingEvaluationWorker(workflow).resume();

    verify(workflow, times(2)).resumeNext();
  }

  @Test void doesNothingWhenThereIsNothingToResume() {
    var workflow = mock(CalibrationWorkflowService.class);
    when(workflow.resumeNext()).thenReturn(false);

    new PendingEvaluationWorker(workflow).resume();

    verify(workflow, times(1)).resumeNext();
  }
}