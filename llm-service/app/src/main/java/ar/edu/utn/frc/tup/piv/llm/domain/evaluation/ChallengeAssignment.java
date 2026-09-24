package ar.edu.utn.frc.tup.piv.llm.domain.evaluation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ChallengeAssignment(UUID challengeId, UUID calibrationRunId, OffsetDateTime lockedAt) {
  public boolean locked() {
    return lockedAt != null;
  }
}
