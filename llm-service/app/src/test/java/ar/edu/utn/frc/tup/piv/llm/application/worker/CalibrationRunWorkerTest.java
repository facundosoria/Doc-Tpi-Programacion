package ar.edu.utn.frc.tup.piv.llm.application.worker;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.application.service.RealCalibrationExecutor;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalibrationRunWorkerTest {
  @Test void dispatchesAClaimedQueuedRunToTheExecutor() {
    var runs = mock(CalibrationRunRepository.class);
    var executor = mock(RealCalibrationExecutor.class);
    UUID runId = UUID.randomUUID();
    when(runs.claimNextQueued()).thenReturn(Optional.of(new CalibrationRunRepository.Run(runId, "QUEUED", 0)));

    new CalibrationRunWorker(runs, executor).dispatch();

    verify(executor).execute(runId);
  }

  @Test void skipsWhenNoRunIsClaimable() {
    var runs = mock(CalibrationRunRepository.class);
    var executor = mock(RealCalibrationExecutor.class);
    when(runs.claimNextQueued()).thenReturn(Optional.empty());

    new CalibrationRunWorker(runs, executor).dispatch();

    verify(executor, never()).execute(any());
  }
}