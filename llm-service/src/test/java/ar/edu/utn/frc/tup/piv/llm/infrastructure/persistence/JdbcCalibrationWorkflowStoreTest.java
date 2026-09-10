package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcCalibrationWorkflowStoreTest {
  @Test
  void snapshotsTheAssignedCalibrationWhenQueueingAnEvaluation() {
    var jdbc = mock(JdbcTemplate.class);
    var audit = mock(AuditRepository.class);
    UUID attemptId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();
    when(jdbc.update(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.<Object>any())).thenReturn(1);

    new JdbcCalibrationWorkflowStore(jdbc, audit).enqueue(attemptId, challengeId, idempotencyKey);

    verify(jdbc).update(contains("calibration_run_id"), eq(attemptId), eq(idempotencyKey), eq(challengeId));
  }
}
