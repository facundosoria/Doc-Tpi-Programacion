package ar.edu.utn.frc.tup.piv.llm.application;
import java.util.UUID; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
/** Gates new evaluations; historical completed evaluations are never changed. */
@Service public class EvaluationAvailabilityService {private final CalibrationWorkflowService.CalibrationWorkflowStore store;public EvaluationAvailabilityService(CalibrationWorkflowService.CalibrationWorkflowStore s){store=s;}@Transactional public boolean queueWhenUnavailable(UUID attemptId,UUID challengeId,UUID idempotencyKey){if(store.hasValidCalibration(challengeId))return false;store.enqueue(attemptId,challengeId,idempotencyKey);return true;}}
