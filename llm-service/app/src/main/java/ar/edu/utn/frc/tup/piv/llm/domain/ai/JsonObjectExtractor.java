package ar.edu.utn.frc.tup.piv.llm.domain.ai;

/** Saca el objeto JSON de una respuesta de modelo que lo trae envuelto: entre marcas de código
 * (```json ... ```) o con texto antes y después. Los proveedores reales suelen hacerlo aunque el
 * prompt pida "únicamente un JSON"; sin esto, el schema estricto del evaluador
 * ({@link ModelResponseSchema}) descartaría respuestas perfectamente usables.
 *
 * <p>No corrige JSON inválido: si no hay un objeto con llaves balanceadas, devuelve el texto tal
 * cual y la validación posterior falla como siempre. */
public final class JsonObjectExtractor {
  private JsonObjectExtractor() {}

  public static String extract(String text) {
    if (text == null || text.isBlank()) {
      return text;
    }
    int start = text.indexOf('{');
    while (start >= 0) {
      int end = matchingBrace(text, start);
      if (end > 0) {
        return text.substring(start, end + 1);
      }
      start = text.indexOf('{', start + 1);
    }
    return text;
  }

  /** Índice de la llave que cierra la de {@code start}, o -1. Ignora llaves dentro de strings. */
  private static int matchingBrace(String text, int start) {
    int depth = 0;
    boolean inString = false;
    for (int i = start; i < text.length(); i++) {
      char c = text.charAt(i);
      if (inString) {
        if (c == '\\') {
          i++;
        } else if (c == '"') {
          inString = false;
        }
      } else if (c == '"') {
        inString = true;
      } else if (c == '{') {
        depth++;
      } else if (c == '}' && --depth == 0) {
        return i;
      }
    }
    return -1;
  }
}
