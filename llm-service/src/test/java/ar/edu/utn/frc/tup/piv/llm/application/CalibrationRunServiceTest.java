package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CalibrationReproducibilityRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalibrationRunServiceTest {
  @Test
  void auditsTheQueuedCalibrationWithItsRequestIdentity() {
    var runs = mock(CalibrationRunRepository.class);
    var artifacts = mock(CalibrationReproducibilityRepository.class);
    var audit = mock(AuditRepository.class);
    UUID courseId = UUID.randomUUID();
    UUID rubricVersionId = UUID.randomUUID();
    UUID goldenSetVersionId = UUID.randomUUID();
    UUID modelDeploymentId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();
    UUID runId = UUID.randomUUID();
    CallerIdentity actor = new CallerIdentity("courses-service", UUID.randomUUID(), "req-16", "trace-16");
    when(runs.create(courseId, rubricVersionId, goldenSetVersionId, modelDeploymentId,
        idempotencyKey, actor.delegatedUserId())).thenReturn(new CalibrationRunRepository.Run(runId, "QUEUED", 0));

    new CalibrationRunService(runs, artifacts, audit).enqueue(courseId, rubricVersionId,
        goldenSetVersionId, modelDeploymentId, idempotencyKey, actor);

    verify(artifacts).snapshot(eq(runId), any(), eq(""));
    verify(audit).record(eq("calibration.queued"), eq("calibration-run"), eq(runId), eq(actor), anyString());
  }
}
