package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.domain.CalibrationMetrics.Dimension;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** El prompt del evaluador armado desde una rúbrica y el parseo de su respuesta. Lo comparten la
 * calibración ({@link CalibrationEvaluationRunner}) y el shadow del evaluador (E-31), para que
 * ambos midan exactamente lo mismo. */
public final class EvaluatorPrompt {
  private EvaluatorPrompt() {}

  /** El prompt de sistema y los pesos enteros por dimensión (ambos salen de la misma lista). */
  public record Rendered(String systemPrompt, Map<Dimension, Integer> weights) {}

  public static Rendered render(List<RubricDraftService.DimensionInput> dimensions) {
    Map<Dimension, Integer> weights = new EnumMap<>(Dimension.class);
    StringBuilder prompt = new StringBuilder(
        "Eres un evaluador que puntúa una transcripción de tutoría en cinco dimensiones, cada una de 0 a 100. "
            + "Respondé únicamente un JSON con las cinco claves en minúscula: autonomy, clarity, progression, compliance, efficiency.\n\n");
    for (var dimension : dimensions) {
      weights.put(dimension.key(), dimension.weight().setScale(0, RoundingMode.HALF_UP).intValueExact());
      prompt.append("- ").append(dimension.key().name().toLowerCase(Locale.ROOT)).append(" (peso ")
          .append(dimension.weight()).append("): ").append(dimension.criterion());
      if (dimension.anchors() != null) {
        prompt.append(" Anclas: ").append(dimension.anchors());
      }
      prompt.append('\n');
    }
    return new Rendered(prompt.toString(), weights);
  }

  public static String userPrompt(JsonNode challengeContext, JsonNode transcript) {
    return "CONTEXTO DEL DESAFÍO:\n" + challengeContext + "\n\nTRANSCRIPCIÓN:\n" + transcript;
  }

  /** Las cinco dimensiones enteras de un JSON de puntajes; falla si falta alguna. Acepta las claves en
   * minúscula (respuesta del evaluador) y en mayúscula (notas humanas guardadas en el golden set). */
  public static Map<Dimension, Integer> scoresFrom(JsonNode node) {
    Map<Dimension, Integer> scores = new EnumMap<>(Dimension.class);
    for (Dimension dimension : Dimension.values()) {
      JsonNode value = node.get(dimension.name().toLowerCase(Locale.ROOT));
      if (value == null) {
        value = node.get(dimension.name());
      }
      if (value == null || !value.isIntegralNumber()) {
        throw new IllegalStateException("Falta la dimensión " + dimension + " en los puntajes");
      }
      scores.put(dimension, value.intValue());
    }
    return scores;
  }
}
