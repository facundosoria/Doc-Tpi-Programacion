package ar.edu.utn.frc.tup.piv.llm.domain.evaluation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PendingEvaluation(
    UUID id,
    UUID attemptId,
    UUID assignmentId,
    UUID calibrationRunId,
    String state,
    String reason,
    OffsetDateTime queuedAt) {}
