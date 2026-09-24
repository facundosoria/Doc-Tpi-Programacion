package ar.edu.utn.frc.tup.piv.llm.domain.tutor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Un turno de una {@link Conversation}. Portado de `demoLLMSpringAi/.../model/Mensaje.java`. */
public record Message(UUID id, UUID conversationId, String rol, String contenido, OffsetDateTime timestamp) {

  public static final String ROL_ALUMNO = "alumno";
  public static final String ROL_TUTOR = "tutor";

  public static Message de(UUID conversationId, String rol, String contenido) {
    return new Message(UUID.randomUUID(), conversationId, rol, contenido, OffsetDateTime.now());
  }
}
