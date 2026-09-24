package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** Respuesta cruda del adaptador, antes de cualquier guardarraíl de salida. `inputTokens` y
 * `outputTokens` son los que reportó el proveedor, o {@code null} si no los informó (el gateway
 * los estima en ese caso). */
public record ModelInvocationResult(String text, String provider, String model, Integer inputTokens,
    Integer outputTokens) {

  public ModelInvocationResult(String text, String provider, String model) {
    this(text, provider, model, null, null);
  }
}
