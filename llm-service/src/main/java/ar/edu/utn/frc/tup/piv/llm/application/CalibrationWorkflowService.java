package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationStateMachine;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalibrationWorkflowService {
  private final CalibrationWorkflowStore store;
  public CalibrationWorkflowService(CalibrationWorkflowStore store) { this.store = store; }
  @Transactional
  public void start(UUID runId) { transition(runId, CalibrationStateMachine.CalibrationState.QUEUED, CalibrationStateMachine.CalibrationState.RUNNING); }
  @Transactional
  public void finish(UUID runId, boolean passed) { transition(runId, CalibrationStateMachine.CalibrationState.RUNNING, passed ? CalibrationStateMachine.CalibrationState.PASSED : CalibrationStateMachine.CalibrationState.FAILED); }
  @Transactional
  public void activate(UUID courseId, UUID runId, CallerIdentity actor) {
    if (!store.isPassed(courseId, runId)) throw new IllegalStateException("Only a passed calibration can be activated");
    store.activate(courseId, runId, actor);
  }
  @Transactional
  public void queue(UUID attemptId, UUID challengeId, UUID key) { if (!store.hasValidCalibration(challengeId)) store.enqueue(attemptId, challengeId, key); }
  @Transactional
  public boolean resumeNext() { return store.claimNextQueued().map(item -> store.hasValidCalibration(item.challengeId()) && store.markRunning(item.id())).orElse(false); }
  private void transition(UUID runId, CalibrationStateMachine.CalibrationState from, CalibrationStateMachine.CalibrationState to) { if (!store.transition(runId, from, to)) throw new IllegalStateException("Calibration transition rejected"); }
  public interface CalibrationWorkflowStore {
    boolean transition(UUID runId, CalibrationStateMachine.CalibrationState from, CalibrationStateMachine.CalibrationState to);
    boolean isPassed(UUID runId); boolean isPassed(UUID courseId, UUID runId); void activate(UUID courseId, UUID runId, CallerIdentity actor);
    boolean hasValidCalibration(UUID challengeId); void enqueue(UUID attemptId, UUID challengeId, UUID key);
    java.util.Optional<QueuedEvaluation> claimNextQueued(); boolean markRunning(UUID pendingId);
  }
  /** The run id is snapshotted when an attempt is queued and must never be recalculated. */
  public record QueuedEvaluation(UUID id, UUID challengeId, UUID calibrationRunId) {}
}
