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
  // Inline snippets are single-line; a fenced block within MAX_CODE_LINES is allowed by the
  // block check above and must not be re-flagged by the inline matcher.
  private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`[^`\n]{2,}`");
  private static final Pattern CODE_LINE_PATTERN = Pattern.compile(
      "(?m)^\\s*(?:public|private|protected|class|interface|record|def|function|fn|let|const|var|if|for|while|return|import|package)\\b|[{};]\\s*$");
  private static final int MAX_CODE_LINES = 8;

  public boolean containsLeak(String response, String expectedSolution) {
    return revealsExpectedSolution(response, expectedSolution) || looksLikeCode(response);
  }

  /**
   * Coincidencia con la solución esperada. Corre en <b>todos</b> los niveles de riesgo: que el
   * alumno declare `low` no puede habilitar que le devolvamos la solución del desafío. Decidido en
   * la integración main↔dev del 2026-09-21, donde `main` filtraba siempre y `dev` salteaba `low`.
   */
  public boolean revealsExpectedSolution(String response, String expectedSolution) {
    if (response == null || response.isBlank()) return false;
    if (expectedSolution == null || expectedSolution.isBlank()) return false;
    return normalized(response).contains(normalized(expectedSolution));
  }

  /**
   * Heurística de forma de código (bloques largos, snippets inline, líneas con forma de código).
   * Solo se aplica en `high`/`medium`: en `low` sobre-bloquea respuestas legítimas, según la adenda
   * SSE y {@code docs/.../ep-05/interactions.md}.
   */
  public boolean looksLikeCode(String response) {
    if (response == null || response.isBlank()) return false;

    var matcher = CODE_BLOCK_PATTERN.matcher(response);
    while (matcher.find()) {
      if (matcher.group().lines().count() > MAX_CODE_LINES) return true;
    }

    // The product policy is code-free tutoring. Do not rely on the model following its prompt:
    // reject inline snippets and code-shaped lines before any response reaches the learner.
    return INLINE_CODE_PATTERN.matcher(response).find() || CODE_LINE_PATTERN.matcher(response).find();
  }

  private String normalized(String value) {
    return value.toLowerCase().replaceAll("\\s+", " ").trim();
  }
}
