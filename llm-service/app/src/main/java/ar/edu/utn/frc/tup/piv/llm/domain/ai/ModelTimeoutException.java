package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** El adaptador superó el timeout configurado (H10·CA5) — se corta sin colgar el hilo que
 * esperaba la respuesta. */
public class ModelTimeoutException extends RuntimeException {
  public ModelTimeoutException(String message) { super(message); }
}
