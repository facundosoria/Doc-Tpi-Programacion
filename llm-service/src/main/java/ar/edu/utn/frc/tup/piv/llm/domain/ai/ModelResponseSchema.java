package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** Validador de schema estricto de salida (H10·CA4). Hoy solo sabe validar la respuesta de
 * {@link ModelFunction#TUTOR} (texto no vacío, dentro de un largo razonable); las demás funciones
 * no tienen adaptador todavía, así que no tienen schema definido. */
public final class ModelResponseSchema {
  private static final int MAX_LENGTH = 4000;

  public void validate(ModelFunction function, String text) {
    if (function != ModelFunction.TUTOR) {
      throw new UnsupportedOperationException("La función " + function + " todavía no tiene adaptador ni schema definido");
    }
    if (text == null || text.isBlank()) {
      throw new InvalidModelResponseException("La respuesta del modelo llegó vacía");
    }
    if (text.length() > MAX_LENGTH) {
      throw new InvalidModelResponseException("La respuesta del modelo supera el límite de " + MAX_LENGTH + " caracteres");
    }
  }
}
