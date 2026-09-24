package ar.edu.utn.frc.tup.piv.llm.domain.tutor;

import java.util.UUID;

/**
 * Se lanza cuando el desafío asociado a una conversación no existe o ya fue cerrado (H03-T5).
 * El controller la mapea a 404 Not Found con cuerpo RFC 7807.
 *
 * <p>El mensaje es IDÉNTICO tanto para "no existe" como para "está cerrado" — la historia exige
 * no distinguir ambos casos en la respuesta para no revelar información del sistema (CA6).
 */
public class ChallengeNotActiveException extends RuntimeException {

  public ChallengeNotActiveException(UUID challengeId) {
    super("Desafío no encontrado o no disponible: " + challengeId);
  }
}
