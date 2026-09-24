package ar.edu.utn.frc.tup.piv.llm.domain.evaluation;

import java.util.List;
import java.util.UUID;

public record CalibrationMigrationPreview(List<UUID> migrable, List<UUID> locked) {}
