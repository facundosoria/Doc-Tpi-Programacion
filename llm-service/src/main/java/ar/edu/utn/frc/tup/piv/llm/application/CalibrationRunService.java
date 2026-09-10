package ar.edu.utn.frc.tup.piv.llm.application;

import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CalibrationReproducibilityRepository;
import ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalibrationRunService {
  private final CalibrationRunRepository runs;
  private final CalibrationReproducibilityRepository artifacts;
  private final AuditRepository audit;

  public CalibrationRunService(CalibrationRunRepository runs,
      CalibrationReproducibilityRepository artifacts, AuditRepository audit) {
    this.runs = runs;
    this.artifacts = artifacts;
    this.audit = audit;
  }

  @Transactional
  public CalibrationRunRepository.Run enqueue(UUID courseId, UUID rubricVersionId, UUID goldenSetVersionId,
      UUID modelDeploymentId, UUID idempotencyKey, CallerIdentity actor) {
    var run = runs.create(courseId, rubricVersionId, goldenSetVersionId, modelDeploymentId,
        idempotencyKey, actor.delegatedUserId());
    artifacts.snapshot(run.id(), JsonNodeFactory.instance.objectNode(), "");
    audit.record("calibration.queued", "calibration-run", run.id(), actor,
        "{\"courseId\":\"" + courseId + "\",\"rubricVersionId\":\"" + rubricVersionId
            + "\",\"goldenSetVersionId\":\"" + goldenSetVersionId
            + "\",\"modelDeploymentId\":\"" + modelDeploymentId + "\"}");
    return run;
  }

  @Transactional(readOnly = true)
  public CalibrationRunRepository.Run get(UUID courseId, UUID runId) {
    return runs.find(courseId, runId)
        .orElseThrow(() -> new IllegalStateException("La calibración no existe en el curso"));
  }

  @Transactional(readOnly = true)
  public java.util.List<CalibrationRunRepository.Run> list(UUID courseId) {
    return runs.list(courseId);
  }
}
