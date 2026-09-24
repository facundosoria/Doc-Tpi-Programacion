package ar.edu.utn.frc.tup.piv.llm.application.exception;

/**
 * El recurso pedido no existe (o no pertenece al curso indicado). Se responde 404; el 409 queda
 * reservado para conflictos de estado reales (versión ya publicada, calibración activa duplicada).
 */
public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
