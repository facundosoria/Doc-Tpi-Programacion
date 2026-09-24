package ar.edu.utn.frc.tup.piv.llm.infrastructure.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentSelfMentionSanitizer Tests (LLM-S18-H02 / T2)")
class AgentSelfMentionSanitizerTest {

    private AgentSelfMentionSanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new AgentSelfMentionSanitizer();
    }

    @Test
    @DisplayName("Debe detectar @agente y @agent en distintas variantes de mayúsculas")
    void testDetectionVariants() {
        assertTrue(sanitizer.containsSelfMention("Hola @agente ¿cómo estás?"));
        assertTrue(sanitizer.containsSelfMention("Hola @Agente, responde"));
        assertTrue(sanitizer.containsSelfMention("Consulta para @AGENT en el canal"));
        assertTrue(sanitizer.containsSelfMention("Mensaje con @agent al final"));

        assertFalse(sanitizer.containsSelfMention("Hola alumno, responde sin mención"));
        assertFalse(sanitizer.containsSelfMention(""));
        assertFalse(sanitizer.containsSelfMention(null));
        assertFalse(sanitizer.containsSelfMention("contacto@agenteempresa.com"));
    }

    @Test
    @DisplayName("Debe sanitizar texto reemplazando @agente por agente neutro")
    void testSanitizeText() {
        String input = "Citando al usuario: '@agente explica punteros'. Aquí la respuesta:";
        String sanitized = sanitizer.sanitizeText(input);

        assertFalse(sanitized.contains("@agente"));
        assertTrue(sanitized.contains("agente explica punteros"));
        assertNull(sanitizer.sanitizeText(null));
    }

    @Test
    @DisplayName("Debe adjuntar suppress_reply_events: true cuando se detecta auto-mención (CA3)")
    void testSanitizeWithSuppressionFlag() {
        String text = "Recordá que @agente no puede responder fuera del material del curso.";
        var result = sanitizer.sanitize(text);

        assertTrue(result.selfMentionDetected());
        assertTrue(result.suppressReplyEvents());
        assertEquals("Recordá que agente no puede responder fuera del material del curso.", result.sanitizedText());
        assertEquals(Boolean.TRUE, result.metadata().get(AgentSelfMentionSanitizer.METADATA_KEY_SUPPRESS_REPLY_EVENTS));
    }

    @Test
    @DisplayName("Debe mantener texto y flag false cuando no hay auto-mención")
    void testSanitizeWithoutSelfMention() {
        String text = "Un puntero en C almacena la dirección de memoria de una variable.";
        Map<String, Object> initialMetadata = Map.of("cohortId", "c-101");

        var result = sanitizer.sanitize(text, initialMetadata);

        assertFalse(result.selfMentionDetected());
        assertFalse(result.suppressReplyEvents());
        assertEquals(text, result.sanitizedText());
        assertEquals("c-101", result.metadata().get("cohortId"));
        assertNull(result.metadata().get(AgentSelfMentionSanitizer.METADATA_KEY_SUPPRESS_REPLY_EVENTS));
    }

    @Test
    @DisplayName("Debe manejar nulo de forma segura")
    void testSanitizeNull() {
        var result = sanitizer.sanitize(null);
        assertFalse(result.selfMentionDetected());
        assertFalse(result.suppressReplyEvents());
        assertNull(result.sanitizedText());
    }
}
