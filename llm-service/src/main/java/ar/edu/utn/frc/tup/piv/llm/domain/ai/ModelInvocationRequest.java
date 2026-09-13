package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import java.time.Duration;

/** Pedido al {@link ModelInvocationPort}. El prompt de usuario es siempre dato, nunca instrucción
 * de sistema (doc 36 §2) — quien arma el prompt es responsabilidad de la capa de aplicación, no
 * de este record. */
public record ModelInvocationRequest(ModelFunction function, String systemPrompt, String userPrompt, Duration timeout) {
  public ModelInvocationRequest {
    if (function == null) throw new IllegalArgumentException("function es obligatoria");
    if (userPrompt == null || userPrompt.isBlank()) throw new IllegalArgumentException("userPrompt es obligatorio");
    if (timeout == null || timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout debe ser positivo");
  }
}
