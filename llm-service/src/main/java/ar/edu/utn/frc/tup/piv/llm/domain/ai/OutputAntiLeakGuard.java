package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import java.util.regex.Pattern;

/** Guardarraíl de salida anti-fuga (RF-IA-20, EP-05). Portado de
 * `codigo-ejemplo/ms-evaluacion-llm/.../guard/OutputAntiLeakGuard.java` — reescrito en `domain/`
 * sin `@Component` (doc 37 §1). La solución esperada, cuando exista, se le entrega solo a este
 * guardarraíl, jamás al prompt del modelo (doc 36 §5). Esta es la versión síncrona/sin streaming:
 * el Buffer Interceptor con retención de bloques token a token (PAR-11,
 * `docs/contracts/llm-service-v1-tutor-sse-adenda.md`) queda fuera de esta pasada. */
public final class OutputAntiLeakGuard {

  /** Reemplazo cuando la respuesta del modelo filtró código o coincidió con la solución esperada. */
  public static final String SAFE_REPLACEMENT =
      "¿Podrías intentar explicar cómo estructurarías el algoritmo con tus propias palabras "
      + "antes de que revisemos más detalles?";

  private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[a-zA-Z]*\\n[\\s\\S]*?```");
  private static final int MAX_CODE_LINES = 8;

  public boolean containsLeak(String response, String expectedSolution) {
    if (response == null || response.isBlank()) return false;

    var matcher = CODE_BLOCK_PATTERN.matcher(response);
    while (matcher.find()) {
      if (matcher.group().lines().count() > MAX_CODE_LINES) return true;
    }

    if (expectedSolution != null && !expectedSolution.isBlank()) {
      return response.toLowerCase().contains(expectedSolution.toLowerCase().trim());
    }
    return false;
  }
}
