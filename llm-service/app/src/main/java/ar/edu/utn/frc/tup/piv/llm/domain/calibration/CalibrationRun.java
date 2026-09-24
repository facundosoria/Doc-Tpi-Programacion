package ar.edu.utn.frc.tup.piv.llm.domain.calibration;

import java.time.Instant;
import java.util.UUID;

public record CalibrationRun(
    UUID id,
    String state,
    int progress,
    UUID rubricVersionId,
    UUID goldenSetVersionId,
    UUID modelDeploymentId,
    Double maeFinal,
    Integer maxIndividualError,
    String reason,
    Instant createdAt,
    Instant finishedAt) {

  public CalibrationRun(UUID id, String state, int progress) {
    this(id, state, progress, null, null, null, null, null, "MANUAL", Instant.now(), null);
  }
}
