package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Guardarraíl de entrada (RF-IA-05/06/07/10, EP-05). Portado de
 * `codigo-ejemplo/ms-evaluacion-llm/.../guard/InputGuard.java` (ver
 * [[no-tocar-codigo-ajeno]] y `docs/estado-implementacion/codigo-ejemplo/ms-evaluacion-llm.md`) —
 * reescrito en `domain/` sin `@Component` porque este paquete no depende de Spring (doc 37 §1).
 * Detecta jailbreak normalizando diacríticos antes de comparar, así "actúa como" y "actua como"
 * matchean igual. */
public final class InputGuard {

  /** Respuesta fija cuando se detecta un intento de jailbreak — nunca se llama al modelo. */
  public static final String SAFE_REDIRECT =
      "No puedo procesar esa consulta de esa forma. ¿Hay algo específico del tema que no "
      + "entiendas? Contame qué es lo que se te complica y podemos avanzar juntos.";

  private static final List<String> JAILBREAK_KEYWORDS = List.of(
      "ignora tus restricciones", "ignora tus instrucciones", "ignore previous instructions",
      "ignora", "olvida tus reglas", "bypass", "pretend", "act as", "actua como", "actúa como",
      "exploit", "vulnerabilidad", "haz mi tarea", "dame el codigo", "dame el código",
      "dame la solucion", "dame la solución", "resuelve por mi", "resuelve por mí",
      "hazme la tarea", "codigo resuelto", "código resuelto", "descuida tus reglas",
      "solucion completa", "solución completa", "escribe todo el codigo", "escribe todo el código");

  private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  public boolean isJailbreak(String text) {
    if (text == null || text.isBlank()) return false;
    String normalized = normalize(text);
    return JAILBREAK_KEYWORDS.stream().map(this::normalize).anyMatch(normalized::contains);
  }

  private String normalize(String input) {
    String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
    String withoutAccents = DIACRITICS_PATTERN.matcher(nfd).replaceAll("");
    return withoutAccents.toLowerCase(Locale.ROOT).trim();
  }
}
