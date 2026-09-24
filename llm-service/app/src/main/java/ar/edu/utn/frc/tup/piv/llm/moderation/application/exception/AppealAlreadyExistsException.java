package ar.edu.utn.frc.tup.piv.llm.moderation.application.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Excepción de negocio lanzada ante un segundo intento de apelar el mismo incidente (HTTP 409 Conflict).
 */
public class AppealAlreadyExistsException extends ResponseStatusException {

    private final UUID existingAppealId;

    public AppealAlreadyExistsException(UUID existingAppealId) {
        super(HttpStatus.CONFLICT, "appeal_already_exists");
        this.existingAppealId = existingAppealId;
    }

    public UUID getExistingAppealId() {
        return existingAppealId;
    }
}
