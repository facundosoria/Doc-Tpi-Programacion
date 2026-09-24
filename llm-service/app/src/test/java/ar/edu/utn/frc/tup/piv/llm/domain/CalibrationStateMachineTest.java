package ar.edu.utn.frc.tup.piv.llm.domain;

import org.junit.jupiter.api.Test;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationStateMachine.CalibrationState.CANCELLED;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationStateMachine.CalibrationState.FAILED;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationStateMachine.CalibrationState.PASSED;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationStateMachine.CalibrationState.QUEUED;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationStateMachine.CalibrationState.RUNNING;
import static ar.edu.utn.frc.tup.piv.llm.domain.CalibrationStateMachine.EvaluationState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalibrationStateMachineTest {
  @Test void transition_shouldAllowQueuedToRunningAndRunningToPassed() {
    assertThat(CalibrationStateMachine.transition(QUEUED, RUNNING)).isEqualTo(RUNNING);
    assertThat(CalibrationStateMachine.transition(RUNNING, PASSED)).isEqualTo(PASSED);
  }
  @Test void transition_shouldAllowTheRemainingLegalSteps() {
    assertThat(CalibrationStateMachine.transition(QUEUED, CANCELLED)).isEqualTo(CANCELLED);
    assertThat(CalibrationStateMachine.transition(RUNNING, FAILED)).isEqualTo(FAILED);
    assertThat(CalibrationStateMachine.transition(RUNNING, CANCELLED)).isEqualTo(CANCELLED);
  }
  @Test void transition_shouldRejectTerminalStateChanges() {
    assertThatThrownBy(() -> CalibrationStateMachine.transition(FAILED, RUNNING)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> CalibrationStateMachine.transition(QUEUED, PASSED)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> CalibrationStateMachine.transition(PASSED, PASSED)).isInstanceOf(IllegalStateException.class);
  }
  @Test void canActivate_shouldOnlyAllowPassedRuns() {
    assertThat(CalibrationStateMachine.canActivate(PASSED)).isTrue();
    assertThat(CalibrationStateMachine.canActivate(RUNNING)).isFalse();
    assertThat(CalibrationStateMachine.canActivate(QUEUED)).isFalse();
    assertThat(CalibrationStateMachine.canActivate(FAILED)).isFalse();
  }
  @Test void resume_shouldRequireValidCalibrationAndQueuedEvaluation() {
    assertThat(CalibrationStateMachine.resume(EvaluationState.QUEUED, true)).isEqualTo(EvaluationState.RUNNING);
    assertThatThrownBy(() -> CalibrationStateMachine.resume(EvaluationState.QUEUED, false)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> CalibrationStateMachine.resume(EvaluationState.RUNNING, true)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> CalibrationStateMachine.resume(EvaluationState.COMPLETED, true)).isInstanceOf(IllegalStateException.class);
  }
  @Test void mustQueue_shouldOnlyQueueNewEvaluationWithoutCalibration() {
    assertThat(CalibrationStateMachine.mustQueue(EvaluationState.QUEUED, false)).isTrue();
    assertThat(CalibrationStateMachine.mustQueue(EvaluationState.QUEUED, true)).isFalse();
    assertThat(CalibrationStateMachine.mustQueue(EvaluationState.RUNNING, false)).isFalse();
  }
}