package ar.edu.utn.frc.tup.piv.llm.domain.evaluation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ActiveCalibration(UUID courseId, UUID calibrationRunId, OffsetDateTime activatedAt) {}
