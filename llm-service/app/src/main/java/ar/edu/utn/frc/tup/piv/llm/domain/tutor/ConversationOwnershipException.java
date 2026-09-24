package ar.edu.utn.frc.tup.piv.llm.domain.tutor;

import java.util.UUID;

/**
 * Se lanza cuando el alumno que hace el pedido no es el dueño registrado de la conversación
 * (H03-T2 / H03-T6). El controller la mapea a 403 Forbidden con cuerpo RFC 7807.
 *
 * <p>El mensaje NO revela si la conversación existe ni a quién pertenece — mismo texto en todos
 * los casos para evitar enumeración (principio del CA6: "no revelar el motivo").
 */
public class ConversationOwnershipException extends RuntimeException {

  public ConversationOwnershipException(UUID conversationId) {
    super("Conversación no encontrada: " + conversationId);
  }
}
