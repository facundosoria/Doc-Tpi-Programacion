package ar.edu.utn.frc.tup.piv.llm.domain.tutor;

import java.util.UUID;

/**
 * Puerto para consultar si un desafío está abierto, cerrado o no existe (H03-T1).
 */
public interface ChallengeStatusPort {

  ChallengeStatus consultar(UUID challengeId);
}
