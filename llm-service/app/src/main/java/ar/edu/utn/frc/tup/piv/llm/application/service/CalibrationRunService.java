package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.application.exception.ResourceNotFoundException;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.AuditRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationReproducibilityRepository;
import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CalibrationRunRepository;
import ar.edu.utn.frc.tup.piv.llm.application.model.CallerIdentity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence.CourseGoldenSetRepository;

@Service
public class CalibrationRunService {
  private final CalibrationRunRepository runs;
  private final CalibrationReproducibilityRepository artifacts;
  private final AuditRepository audit;
  private final ObjectMapper json;
  private final CourseGoldenSetRepository goldenSets;
  private final SecureRandom random = new SecureRandom();

  public static class IncompleteGoldenSetException extends RuntimeException {
    public IncompleteGoldenSetException(String message) { super(message); }
  }

  @Autowired
  public CalibrationRunService(CalibrationRunRepository runs,
      CalibrationReproducibilityRepository artifacts, AuditRepository audit, ObjectMapper json,
      CourseGoldenSetRepository goldenSets) {
    this.runs = runs;
    this.artifacts = artifacts;
    this.audit = audit;
    this.json = json;
    this.goldenSets = goldenSets;
  }

  public CalibrationRunService(CalibrationRunRepository runs,
      CalibrationReproducibilityRepository artifacts, AuditRepository audit, ObjectMapper json) {
    this(runs, artifacts, audit, json, null);
  }

  /** Test/backwards-compatible constructor. Spring uses the ObjectMapper-aware constructor. */
  public CalibrationRunService(CalibrationRunRepository runs,
      CalibrationReproducibilityRepository artifacts, AuditRepository audit) {
    this(runs, artifacts, audit, new ObjectMapper(), null);
  }

  @Transactional
  public CalibrationRunRepository.Run enqueue(UUID courseId, UUID rubricVersionId, UUID goldenSetVersionId,
      UUID modelDeploymentId, UUID idempotencyKey, CallerIdentity actor) {
    if (goldenSets != null && goldenSets.countCases(goldenSetVersionId) < 3) {
      throw new IncompleteGoldenSetException("El Golden Set debe tener al menos tres casos");
    }
    // Provider SDKs accept a 32-bit seed (Gemini converts it to int internally).
    // Persist only that portable range so a calibration never fails before its first invocation.
    var created = runs.createStability(courseId, rubricVersionId, goldenSetVersionId, modelDeploymentId,
        idempotencyKey, actor.delegatedUserId(), List.of(
            (long) random.nextInt(Integer.MAX_VALUE),
            (long) random.nextInt(Integer.MAX_VALUE),
            (long) random.nextInt(Integer.MAX_VALUE)));
    var run = created.getFirst();
    for (var item : created) artifacts.snapshot(item.id(), json.valueToTree(java.util.Map.of("policyVersion", "v1", "seed", item.id().equals(run.id()) ? "stored" : "stored")), "");
    audit.record("calibration.queued", "calibration-run", run.id(), actor,
        "{\"courseId\":\"" + courseId + "\",\"rubricVersionId\":\"" + rubricVersionId
            + "\",\"goldenSetVersionId\":\"" + goldenSetVersionId
            + "\",\"modelDeploymentId\":\"" + modelDeploymentId + "\"}");
    return run;
  }

  @Transactional(readOnly = true)
  public CalibrationRunRepository.Run get(UUID courseId, UUID runId) {
    return runs.find(courseId, runId)
        .orElseThrow(() -> new ResourceNotFoundException("La calibración no existe en el curso"));
  }

  @Transactional(readOnly = true)
  public java.util.List<CalibrationRunRepository.Run> list(UUID courseId) {
    return runs.list(courseId);
  }
  @Transactional(readOnly = true) public java.util.List<CalibrationRunRepository.StabilityGroup> stabilityGroups(UUID courseId) { return runs.stabilityGroups(courseId); }
}
