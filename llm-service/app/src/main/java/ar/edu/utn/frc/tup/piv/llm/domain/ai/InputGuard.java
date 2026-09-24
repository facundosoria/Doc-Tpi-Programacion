package ar.edu.utn.frc.tup.piv.llm.domain.ai;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Base64;
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

  /** Largo máximo aceptado para un mensaje del alumno. Un mensaje más largo se trata como anómalo
   * (relleno para diluir instrucciones o para agotar el contexto). */
  public static final int MAX_INPUT_LENGTH = 4000;

  /** Delimitadores de roles/plantillas de chat que un alumno legítimo no escribe: etiquetas
   * `<system>`, tokens `<|im_start|>`, `[INST]`, `<<SYS>>`, encabezados `### system` y líneas que
   * arrancan con un rol (`system:`, `assistant:`, `tutor:`...) para falsear turnos previos. */
  private static final List<Pattern> FAKE_DELIMITERS = List.of(
      Pattern.compile("<\\|[^>|\\s]{1,40}\\|>"),
      Pattern.compile("\\[/?\\s*INST\\s*\\]", Pattern.CASE_INSENSITIVE),
      Pattern.compile("<<\\s*/?\\s*SYS\\s*>>", Pattern.CASE_INSENSITIVE),
      Pattern.compile("</?\\s*(system|assistant|developer|instructions?|" + UntrustedText.STUDENT_TAG + "|"
          + UntrustedText.HISTORY_TAG + "|" + UntrustedText.TURN_TAG + ")\\b[^>]*>", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^\\s*#{2,}\\s*(system|instrucciones?|instructions?)\\b",
          Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
      Pattern.compile("^\\s*(system|assistant|developer|tutor|alumno)\\s*:",
          Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));

  /** Candidatos a base64: corridas largas del alfabeto base64 que no forman parte de una palabra. */
  private static final Pattern BASE64_CANDIDATE =
      Pattern.compile("(?<![A-Za-z0-9+/=])[A-Za-z0-9+/]{24,}={0,2}(?![A-Za-z0-9+/=])");

  public boolean isJailbreak(String text) {
    if (text == null || text.isBlank()) return false;
    String normalized = normalize(text);
    return JAILBREAK_KEYWORDS.stream().map(this::normalize).anyMatch(normalized::contains);
  }

  /** Capa 1 completa: {@link #isJailbreak} más los patrones burdos que pide
   * `docs/05-seguridad.md` (delimitadores falsos, base64 sospechoso, largo anómalo). Es un
   * filtro barato; no atrapa a un atacante que reformule, para eso están las demás capas. */
  public boolean isSuspicious(String text) {
    if (text == null || text.isBlank()) return false;
    return text.length() > MAX_INPUT_LENGTH
        || isJailbreak(text)
        || hasFakeDelimiters(text)
        || hasEncodedPayload(text);
  }

  boolean hasFakeDelimiters(String text) {
    return FAKE_DELIMITERS.stream().anyMatch(pattern -> pattern.matcher(text).find());
  }

  /** Base64 que decodifica a texto legible: en un chat de tutoría no hay motivo legítimo para
   * mandarlo, y es la forma clásica de esconder instrucciones de los filtros de palabras clave. Un
   * identificador o hash largo decodifica a bytes basura y no se marca. */
  boolean hasEncodedPayload(String text) {
    var matcher = BASE64_CANDIDATE.matcher(text);
    while (matcher.find()) {
      try {
        byte[] decoded = Base64.getDecoder().decode(matcher.group());
        if (decoded.length >= 12 && isReadableText(new String(decoded, StandardCharsets.UTF_8))) return true;
      } catch (IllegalArgumentException notBase64) {
        // No decodifica: es una palabra larga cualquiera, se ignora.
      }
    }
    return false;
  }

  private boolean isReadableText(String decoded) {
    long readable = decoded.chars()
        .filter(c -> c != '\uFFFD' && (!Character.isISOControl(c) || c == '\n' || c == '\r' || c == '\t'))
        .count();
    return readable >= decoded.length() * 0.9;
  }

  private String normalize(String input) {
    String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
    String withoutAccents = DIACRITICS_PATTERN.matcher(nfd).replaceAll("");
    return withoutAccents.toLowerCase(Locale.ROOT).trim();
  }
}
