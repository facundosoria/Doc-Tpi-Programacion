package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** El circuit breaker del proveedor está abierto: se falla rápido sin llamarlo. */
public class ProviderUnavailableException extends RuntimeException {
  public ProviderUnavailableException(String message) { super(message); }
}
