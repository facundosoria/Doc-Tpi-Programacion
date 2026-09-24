package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.ChallengeCalibrationAssignmentRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalibrationActivationServiceCoverageTest {
  @Test void propagatesAnInvalidConfirmationTokenWithoutActivating() {
    var workflow = mock(CalibrationWorkflowService.class);
    var assignments = mock(ChallengeCalibrationAssignmentRepository.class);
    var audit = mock(AuditRepository.class);
    var confirmations = new CalibrationMigrationConfirmation();
    UUID courseId = UUID.randomUUID(), runId = UUID.randomUUID();
    CallerIdentity actor = new CallerIdentity("courses-service", UUID.randomUUID(), "req", "trace");

    assertThatThrownBy(() -> new CalibrationActivationService(workflow, assignments, confirmations, audit)
        .activateAndMigrate(courseId, runId, actor, "wrong-token", Set.of(UUID.randomUUID())))
        .isInstanceOf(IllegalStateException.class);
    verify(workflow, never()).activate(eq(courseId), eq(runId), eq(actor));
  }

  @Test void auditsTheMigratedChallengeCount() {
    var workflow = mock(CalibrationWorkflowService.class);
    var assignments = mock(ChallengeCalibrationAssignmentRepository.class);
    var audit = mock(AuditRepository.class);
    var confirmations = new CalibrationMigrationConfirmation();
    UUID courseId = UUID.randomUUID(), runId = UUID.randomUUID();
    UUID first = UUID.randomUUID(), second = UUID.randomUUID();
    CallerIdentity actor = new CallerIdentity("courses-service", UUID.randomUUID(), "req-2", "trace-2");
    String token = confirmations.issue(courseId, runId, Set.of(first, second));
    when(assignments.migrate(courseId, runId, Set.of(first, second))).thenReturn(2);

    new CalibrationActivationService(workflow, assignments, confirmations, audit)
        .activateAndMigrate(courseId, runId, actor, token, Set.of(first, second));

    verify(workflow).activate(courseId, runId, actor);
    verify(audit).record(eq("calibration.challenges-migrated"), eq("calibration-run"), eq(runId),
        eq(actor), anyString());
  }
}