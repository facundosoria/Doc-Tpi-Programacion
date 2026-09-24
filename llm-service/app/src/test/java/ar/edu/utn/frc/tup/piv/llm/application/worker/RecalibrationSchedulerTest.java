package ar.edu.utn.frc.tup.piv.llm.application.worker;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RecalibrationSchedulerTest {
  @Test void runsTheMonthlyRecalibrationTrigger() {
    var trigger = mock(RecalibrationScheduler.RecalibrationTrigger.class);
    new RecalibrationScheduler(trigger).monthly();
    verify(trigger).enqueueMonthlyDue();
  }

  @Test void propagatesAModelDeploymentChangeToTheTrigger() {
    var trigger = mock(RecalibrationScheduler.RecalibrationTrigger.class);
    UUID deploymentId = UUID.randomUUID();
    new RecalibrationScheduler(trigger).modelDeploymentChanged(deploymentId);
    verify(trigger).enqueueForModelChange(eq(deploymentId));
  }
}