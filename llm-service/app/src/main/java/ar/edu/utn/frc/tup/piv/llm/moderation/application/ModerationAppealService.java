package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.CreateModerationAppealCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.exception.AppealAlreadyExistsException;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.port.ModerationAppealUseCase;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationAppealRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio de aplicación que implementa el ciclo de vida y las reglas de negocio de las apelaciones (LLM-S12-H01).
 * Implementa control de acceso Anti-IDOR, validación de estado BLOCK e idempotencia (HTTP 409).
 */
@Service
public class ModerationAppealService implements ModerationAppealUseCase {

    private final ModerationAppealRepositoryPort appealRepository;
    private final ModerationIncidentRepositoryPort incidentRepository;

    public ModerationAppealService(ModerationAppealRepositoryPort appealRepository,
                                   ModerationIncidentRepositoryPort incidentRepository) {
        this.appealRepository = appealRepository;
        this.incidentRepository = incidentRepository;
    }

    @Override
    @Transactional
    public ModerationAppeal createAppeal(CreateModerationAppealCommand command) {
        Objects.requireNonNull(command, "command no puede ser nulo.");
        if (command.incidentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incident_id es obligatorio");
        }
        if (command.userId() == null || command.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        if (command.appealReason() == null
                || command.appealReason().trim().length() < 20
                || command.appealReason().trim().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appeal_reason debe tener entre 20 y 1000 caracteres");
        }

        // 1. Verificar idempotencia / rechazar apelación duplicada (CA_negativo_1 -> 409 Conflict)
        Optional<ModerationAppeal> existingAppeal = appealRepository.findByIncidentId(command.incidentId());
        if (existingAppeal.isPresent()) {
            throw new AppealAlreadyExistsException(existingAppeal.get().getId());
        }

        // 2. Verificar existencia del incidente (CA5 -> 404 Not Found)
        ModerationIncident incident = incidentRepository.findById(command.incidentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente no encontrado: " + command.incidentId()));

        // 3. Verificar estado BLOCK del incidente (CA5 -> 422 Unprocessable Entity)
        if (!incident.isBlock()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Solo se pueden apelar incidentes en estado BLOCK");
        }

        // 4. Validación de propiedad Anti-IDOR (CA4 -> 403 Forbidden)
        if (!incident.isOwnedBy(command.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El incidente no pertenece al usuario autenticado");
        }

        // 5. Crear y persistir la apelación en estado PENDING_REVIEW (CA1, CA2)
        ModerationAppeal appeal = ModerationAppeal.create(
                command.incidentId(),
                command.userId().trim(),
                command.appealReason().trim()
        );

        return appealRepository.save(appeal);
    }

    @Override
    @Transactional(readOnly = true)
    public ModerationAppeal getAppeal(UUID appealId, String authenticatedUserId) {
        if (appealId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appeal_id es obligatorio");
        }
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        // 1. Recuperar apelación (CA5 -> 404 Not Found)
        ModerationAppeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Apelación no encontrada: " + appealId));

        // 2. Validación de pertenencia Anti-IDOR (CA3 / T4 -> 403 Forbidden)
        if (!appeal.isOwnedBy(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La apelación no pertenece al usuario autenticado");
        }

        return appeal;
    }
}
