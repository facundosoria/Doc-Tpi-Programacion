package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/** Validador de schema estricto de salida (H10·CA4). {@link ModelFunction#TUTOR} espera texto
 * libre (prosa socrática, dentro de un largo razonable); {@link ModelFunction#EVALUATOR} espera
 * un JSON con las cinco dimensiones de la rúbrica (mismo shape que valida `valid_reference_scores`
 * en Postgres, para que `calibration_case_results.model_scores` nunca reciba algo que la base
 * rechazaría). Las demás funciones no tienen adaptador todavía, así que no tienen schema definido. */
public final class ModelResponseSchema {
  private static final int MAX_LENGTH = 4000;
  private static final List<String> SCORE_KEYS =
      List.of("autonomy", "clarity", "compliance", "efficiency", "progression");
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public void validate(ModelFunction function, String text) {
    switch (function) {
      case TUTOR -> validateProse(text);
      case EVALUATOR -> validateScores(text);
      default -> throw new UnsupportedOperationException(
          "La función " + function + " todavía no tiene adaptador ni schema definido");
    }
  }

  private void validateProse(String text) {
    if (text == null || text.isBlank()) {
      throw new InvalidModelResponseException("La respuesta del modelo llegó vacía");
    }
    if (text.length() > MAX_LENGTH) {
      throw new InvalidModelResponseException("La respuesta del modelo supera el límite de " + MAX_LENGTH + " caracteres");
    }
  }

  private void validateScores(String text) {
    JsonNode node = parse(text);
    if (!node.isObject() || node.size() != SCORE_KEYS.size()) {
      throw new InvalidModelResponseException("La respuesta del evaluador debe tener exactamente las cinco dimensiones");
    }
    for (String key : SCORE_KEYS) {
      JsonNode value = node.get(key);
      if (value == null || !value.isIntegralNumber() || value.intValue() < 0 || value.intValue() > 100) {
        throw new InvalidModelResponseException("La dimensión '" + key + "' del evaluador debe ser un entero entre 0 y 100");
      }
    }
  }

  private JsonNode parse(String text) {
    if (text == null || text.isBlank()) {
      throw new InvalidModelResponseException("La respuesta del evaluador llegó vacía");
    }
    try {
      return MAPPER.readTree(text);
    } catch (Exception exception) {
      throw new InvalidModelResponseException("La respuesta del evaluador no es JSON válido");
    }
  }
}
