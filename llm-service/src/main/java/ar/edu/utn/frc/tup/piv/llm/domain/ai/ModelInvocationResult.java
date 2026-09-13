package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** Respuesta cruda del adaptador, antes de cualquier guardarraíl de salida. */
public record ModelInvocationResult(String text, String provider, String model) {}
