package ar.edu.utn.frc.tup.piv.llm.infrastructure.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interceptor/Sanitizador de auto-menciones del agente (LLM-S18-H02 / T2).
 * <p>
 * Regla de negocio (CA3):
 * Si la respuesta generada por el LLM contiene menciones textuales a `@agente` o `@agent`
 * (por ejemplo, al citar un mensaje previo del usuario o del chat), sanitiza la etiqueta
 * neutralizando la mención activa y adjunta el flag `suppress_reply_events: true` en los metadatos
 * para garantizar que chat-service no interprete la respuesta como un nuevo disparador.
 */
@Component
public class AgentSelfMentionSanitizer {

    private static final Logger log = LoggerFactory.getLogger(AgentSelfMentionSanitizer.class);

    public static final String METADATA_KEY_SUPPRESS_REPLY_EVENTS = "suppress_reply_events";

    private static final Pattern SELF_MENTION_PATTERN = Pattern.compile(
            "(?i)@(agente|agent)(\\b|(?=\\W|$))"
    );

    /**
     * Resultado inmutable del proceso de sanitización.
     */
    public record SanitizationResult(
            String sanitizedText,
            boolean suppressReplyEvents,
            boolean selfMentionDetected,
            Map<String, Object> metadata
    ) {
    }

    /**
     * Comprueba si el texto contiene menciones textuales al agente (@agente o @agent).
     *
     * @param text texto a inspeccionar
     * @return true si contiene auto-menciones
     */
    public boolean containsSelfMention(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return SELF_MENTION_PATTERN.matcher(text).find();
    }

    /**
     * Sanitiza el texto reemplazando la mención activa `@agente` / `@agent` por su forma
     * neutra sin el prefijo `@` para que no sea parseada como mención de chat.
     *
     * @param text texto original
     * @return texto sanitizado
     */
    public String sanitizeText(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = SELF_MENTION_PATTERN.matcher(text);
        // Reemplaza '@agente' -> 'agente' y '@agent' -> 'agent'
        return matcher.replaceAll("$1");
    }

    /**
     * Inspecciona y procesa la salida de texto, retornando el resultado sanitizado
     * y el flag de supresión.
     *
     * @param text texto generado por el LLM
     * @return SanitizationResult con texto sanitizado y flags correspondientes
     */
    public SanitizationResult sanitize(String text) {
        return sanitize(text, Collections.emptyMap());
    }

    /**
     * Inspecciona y procesa la salida de texto junto con metadatos existentes.
     *
     * @param text             texto generado por el LLM
     * @param existingMetadata metadatos existentes a enriquecer
     * @return SanitizationResult con texto sanitizado, metadatos enriquecidos y flags
     */
    public SanitizationResult sanitize(String text, Map<String, Object> existingMetadata) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (existingMetadata != null) {
            metadata.putAll(existingMetadata);
        }

        if (text == null) {
            return new SanitizationResult(null, false, false, Collections.unmodifiableMap(metadata));
        }

        boolean detected = containsSelfMention(text);
        if (detected) {
            log.debug("Auto-mención detectada en salida de LLM. Aplicando sanitización y supresión de eventos.");
            String cleanText = sanitizeText(text);
            metadata.put(METADATA_KEY_SUPPRESS_REPLY_EVENTS, true);
            return new SanitizationResult(cleanText, true, true, Collections.unmodifiableMap(metadata));
        }

        return new SanitizationResult(text, false, false, Collections.unmodifiableMap(metadata));
    }
}
