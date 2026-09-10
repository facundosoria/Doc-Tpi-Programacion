package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ChallengeCalibrationAssignmentRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalibrationActivationServiceTest {
  @Test
  void auditsTheConfirmedChallengeMigrationWithTheOriginalActor() {
    var workflow = mock(CalibrationWorkflowService.class);
    var assignments = mock(ChallengeCalibrationAssignmentRepository.class);
    var audit = mock(AuditRepository.class);
    var confirmations = new CalibrationMigrationConfirmation();
    UUID courseId = UUID.randomUUID();
    UUID runId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    CallerIdentity actor = new CallerIdentity("courses-service", UUID.randomUUID(), "req-17", "trace-17");
    String token = confirmations.issue(courseId, runId, Set.of(challengeId));
    when(assignments.migrate(courseId, runId, Set.of(challengeId))).thenReturn(1);

    new CalibrationActivationService(workflow, assignments, confirmations, audit)
        .activateAndMigrate(courseId, runId, actor, token, Set.of(challengeId));

    verify(workflow).activate(courseId, runId, actor);
    verify(assignments).migrate(courseId, runId, Set.of(challengeId));
    verify(audit).record(eq("calibration.challenges-migrated"), eq("calibration-run"), eq(runId),
        eq(actor), anyString());
  }
}
