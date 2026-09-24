package ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.validation;

import ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.AgentMentionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Validador de rol de remitente para menciones al agente en el chat (LLM-S18-H02 / T1).
 * <p>
 * Reglas de negocio:
 * - Si sender_role es "student" o "teacher": autoriza la ejecución (CA1).
 * - Si sender_role es "bot", "system", nulo, cadena vacía o rol desconocido: corta inmediatamente
 *   el flujo y retorna HTTP 204 No Content (drop silencioso), sin registrar fallo ni llamar
 *   a modelos aguas abajo (CA2, CA5).
 */
@Component
public class MentionSenderValidator {

    private static final Logger log = LoggerFactory.getLogger(MentionSenderValidator.class);

    private static final Set<String> ALLOWED_ROLES = Set.of("student", "teacher");

    /**
     * Evalúa si el rol del remitente está autorizado a interactuar con el agente.
     *
     * @param senderRole rol enviado por chat-service ("student", "teacher", "bot", "system", etc.)
     * @return true si es student o teacher, false en caso contrario
     */
    public boolean isAuthorized(String senderRole) {
        if (senderRole == null || senderRole.isBlank()) {
            return false;
        }
        return ALLOWED_ROLES.contains(senderRole.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Alias compatible con interfaces de validación de remitente.
     */
    public boolean isValidSender(String senderRole) {
        return isAuthorized(senderRole);
    }

    /**
     * Evalúa si una solicitud debe ser descartada silenciosamente.
     *
     * @param senderRole rol enviado
     * @return true si debe descartarse (bot, system, nulo, vacío, desconocido)
     */
    public boolean shouldDrop(String senderRole) {
        return !isAuthorized(senderRole);
    }

    /**
     * Retorna la respuesta canónica de descarte silencioso: HTTP 204 No Content.
     */
    public ResponseEntity<Void> silentDropResponse() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Valida el rol del remitente. Si no está autorizado, retorna un Optional con HTTP 204 No Content.
     * Si está autorizado, retorna un Optional vacío para permitir continuar la ejecución.
     *
     * @param senderRole rol recibido
     * @return Optional con 204 No Content si se debe descartar silenciosamente, o Optional.empty() si es válido
     */
    public Optional<ResponseEntity<Void>> validate(String senderRole) {
        if (!isAuthorized(senderRole)) {
            // Drop silencioso: no se registra como error ni advertencia de fallo (CA2, CA5)
            log.debug("Silent drop de mención por sender_role: '{}'", senderRole);
            return Optional.of(silentDropResponse());
        }
        return Optional.empty();
    }

    /**
     * Valida una solicitud completa de mención al agente.
     *
     * @param request payload de la mención
     * @return Optional con 204 No Content si se debe descartar silenciosamente, o Optional.empty() si es válido
     */
    public Optional<ResponseEntity<Void>> validate(AgentMentionRequest request) {
        if (request == null) {
            return Optional.of(silentDropResponse());
        }
        return validate(request.senderRole());
    }

    public Optional<ResponseEntity<Void>> validate(ar.edu.utn.frc.tup.piv.llm.adapter.in.web.agent.dto.AgentMentionRequest request) {
        if (request == null) {
            return Optional.of(silentDropResponse());
        }
        return validate(request.senderRole());
    }
}
