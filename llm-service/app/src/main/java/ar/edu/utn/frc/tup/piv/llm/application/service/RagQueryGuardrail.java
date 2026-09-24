package ar.edu.utn.frc.tup.piv.llm.application.service;

import ar.edu.utn.frc.tup.piv.llm.domain.ai.InputGuard;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Guardarraíles del flujo RAG que {@link InputGuard}/{@link ar.edu.utn.frc.tup.piv.llm.domain.ai.OutputAntiLeakGuard}
 * no cubren: cooldown anti-flood, caché en memoria de respuestas idénticas, filtro de
 * profanidad, spam por caracteres repetidos y límites de longitud. Portado de
 * `demoLLMSpringAi/.../security/GuardrailService.java` — a propósito NO duplica la detección de
 * jailbreak/prompt injection (delega en {@link InputGuard}, unificando las listas de keywords que
 * antes existían por separado en `TutorSocraticoService`, `GuardrailService` e `InputGuard`).
 * Vive en `application`, no en `domain`, porque el cooldown y la caché son estado mutable por
 * proceso — no es una regla de negocio pura. */
@Component
public class RagQueryGuardrail {
  private static final long COOLDOWN_MS = 1200;
  private static final int MAX_CACHE_SIZE = 300;

  private final Map<String, Instant> lastRequestTimes = new ConcurrentHashMap<>();
  private final Map<String, RagChatService.Response> queryCache = Collections.synchronizedMap(
      new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, RagChatService.Response> eldest) {
          return size() > MAX_CACHE_SIZE;
        }
      });

  private final InputGuard inputGuard = new InputGuard();

  private static final List<String> PROFANITIES = List.of(
      "mierda", "puto", "puta", "putita", "hdp", "hijo de puta", "hija de puta",
      "carajo", "concha", "conchudo", "conchuda", "pendejo", "pendeja", "cabron", "cabrona",
      "pelotudo", "pelotuda", "boludo", "boluda", "forro", "forra", "gil", "chucha",
      "verga", "chinga", "chingar", "chingada", "culiao", "culiada", "maricon", "marica",
      "malparido", "malparida", "tarado", "tarada", "estupido", "estupida", "idiota",
      "imbecil", "zorra", "bastardo", "bastarda", "coño", "hostia", "me cago", "jodete",
      "maldito", "maldita", "cornudo", "cornuda",
      "fuck", "fucking", "fucker", "shit", "bitch", "asshole", "bastard", "cunt",
      "dick", "pussy", "motherfucker", "cock", "whore", "slut", "nigger", "faggot");

  private static final Pattern REPETITIVE_CHARS_PATTERN = Pattern.compile("(.)\\1{5,}");

  public record ValidationResult(boolean valid, String status, String userMessage) {
    public static ValidationResult ok() {
      return new ValidationResult(true, "OK", null);
    }

    public static ValidationResult blocked(String status, String userMessage) {
      return new ValidationResult(false, status, userMessage);
    }
  }

  public ValidationResult validate(String pregunta, String sessionKey) {
    if (pregunta == null || pregunta.trim().isEmpty()) {
      return ValidationResult.blocked("BLOCKED_EMPTY",
          "La pregunta no puede estar vacía. Por favor escribe una consulta válida sobre el documento.");
    }

    String trimmed = pregunta.trim();
    if (trimmed.length() < 4) {
      return ValidationResult.blocked("BLOCKED_TOO_SHORT",
          "Tu pregunta es demasiado corta. Escribe al menos 4 caracteres para poder ayudarte con precisión.");
    }
    if (trimmed.length() > 600) {
      return ValidationResult.blocked("BLOCKED_TOO_LONG",
          "Tu pregunta excede el límite de 600 caracteres. Para optimizar la respuesta, sé más específico y conciso.");
    }
    if (REPETITIVE_CHARS_PATTERN.matcher(trimmed).find()) {
      return ValidationResult.blocked("BLOCKED_SPAM",
          "Se detectó texto repetitivo o sin sentido. Por favor formula una pregunta académica clara.");
    }

    String key = sessionKey != null ? sessionKey : "default_user";
    Instant now = Instant.now();
    Instant lastTime = lastRequestTimes.get(key);
    if (lastTime != null && now.toEpochMilli() - lastTime.toEpochMilli() < COOLDOWN_MS) {
      return ValidationResult.blocked("BLOCKED_RATE_LIMIT",
          "Estás enviando consultas demasiado rápido. Espera un segundo antes de formular otra pregunta.");
    }
    lastRequestTimes.put(key, now);

    String normalized = normalizeForProfanity(trimmed);
    if (containsProfanity(normalized)) {
      return ValidationResult.blocked("BLOCKED_PROFANITY",
          "Consulta no procesada: se detectó lenguaje inapropiado. Para mantener un entorno de aprendizaje "
              + "respetuoso y optimizar los recursos, por favor formula tu duda con respeto.");
    }

    if (inputGuard.isJailbreak(trimmed)) {
      return ValidationResult.blocked("BLOCKED_INJECTION",
          "Intento de manipulación bloqueado: mi rol como tutor pedagógico es inalterable y no está permitido "
              + "modificar las reglas del sistema. ¿En qué concepto del documento puedo ayudarte?");
    }

    return ValidationResult.ok();
  }

  public Optional<RagChatService.Response> getCachedResponse(String cacheKey, String pregunta) {
    return Optional.ofNullable(queryCache.get(cacheKey + ":" + normalizeForCache(pregunta)));
  }

  public void cacheResponse(String cacheKey, String pregunta, RagChatService.Response response) {
    if (response != null && "OK".equals(response.estado())) {
      queryCache.put(cacheKey + ":" + normalizeForCache(pregunta), response);
    }
  }

  /** Normalización de la pregunta para la clave de caché (#679): minúsculas, acentos plegados y
   * espacios colapsados. Deliberadamente NO usa {@link #normalizeForProfanity}: ese normalizador
   * sustituye dígitos por letras (4→a, 3→e, 1→i, 0→o, 5→s) para desarmar el leetspeak, y usarlo
   * como clave hacía colisionar preguntas distintas — "¿qué dice el capítulo 3?" y "que dice el
   * capitulo e" compartían slot, sirviendo una respuesta ajena a la pregunta.
   *
   * <p>Plegar mayúsculas y acentos sí es deseable: "¿Qué es RAG?" y "que es rag" son la misma
   * pregunta y el costo de acertar de más es servir una respuesta correcta sin gastar tokens. La
   * puntuación se conserva: "¿cómo no funciona?" no es "como no funciona". */
  private String normalizeForCache(String pregunta) {
    if (pregunta == null) return "";
    String decomposed = Normalizer.normalize(pregunta.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
    return decomposed.replaceAll("\\p{M}", "").replaceAll("\\s+", " ");
  }

  private boolean containsProfanity(String normalizedText) {
    for (String badWord : PROFANITIES) {
      if (normalizedText.contains(badWord)) return true;
    }
    return false;
  }

  private String normalizeForProfanity(String input) {
    if (input == null) return "";
    String decomposed = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
    String noAccents = decomposed.replaceAll("\\p{M}", "");
    return noAccents
        .replace('@', 'a').replace('4', 'a').replace('3', 'e').replace('1', 'i')
        .replace('0', 'o').replace('5', 's').replace('$', 's')
        .replaceAll("[^a-z0-9\\s]", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }
}
