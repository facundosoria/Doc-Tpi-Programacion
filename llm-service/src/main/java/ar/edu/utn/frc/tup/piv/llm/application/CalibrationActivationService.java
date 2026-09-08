package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.ChallengeCalibrationAssignmentRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalibrationActivationService {
  private final CalibrationWorkflowService workflow;
  private final ChallengeCalibrationAssignmentRepository assignments;
  private final CalibrationMigrationConfirmation confirmations;
  private final AuditRepository audit;

  public CalibrationActivationService(CalibrationWorkflowService workflow,
      ChallengeCalibrationAssignmentRepository assignments,
      CalibrationMigrationConfirmation confirmations, AuditRepository audit) {
    this.workflow = workflow;
    this.assignments = assignments;
    this.confirmations = confirmations;
    this.audit = audit;
  }

  @Transactional
  public void activateAndMigrate(UUID courseId, UUID runId, CallerIdentity actor,
      String confirmationToken, Set<UUID> selectedChallenges) {
    Set<UUID> approvedChallenges = confirmations.consume(confirmationToken, courseId, runId,
        selectedChallenges);
    workflow.activate(courseId, runId, actor);
    int migrated = assignments.migrate(courseId, runId, approvedChallenges);
    audit.record("calibration.challenges-migrated", "calibration-run", runId, actor,
        "{\"courseId\":\"" + courseId + "\",\"requestedCount\":"
            + approvedChallenges.size() + ",\"migratedCount\":" + migrated + "}");
  }
}
