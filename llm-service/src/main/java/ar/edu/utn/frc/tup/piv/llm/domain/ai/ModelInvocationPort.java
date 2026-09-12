package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** El puerto que pide `LLM-S01-H10`: invocar un modelo sin que la función de IA conozca el
 * proveedor concreto. No importa ningún SDK de proveedor (regla del ADR, H01) — vive en
 * `domain`, sin dependencias de Spring. */
public interface ModelInvocationPort {
  ModelInvocationResult invoke(ModelInvocationRequest request);

  String provider();

  String model();
}
