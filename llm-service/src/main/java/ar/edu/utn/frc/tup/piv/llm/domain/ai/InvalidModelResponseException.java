package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** La respuesta del adaptador no cumple el schema estricto de salida (H10·CA4) — nunca se
 * propaga como si fuera válida. */
public class InvalidModelResponseException extends RuntimeException {
  public InvalidModelResponseException(String message) { super(message); }
}
