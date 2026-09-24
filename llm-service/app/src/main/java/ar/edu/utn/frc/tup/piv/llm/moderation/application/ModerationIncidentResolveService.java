package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationResolutionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ResolveIncidentCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.port.ModerationIncidentResolveUseCase;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolution;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionDomainEvent;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolutionType;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationAppealRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationEventPublisherPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationResolutionRepositoryPort;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio de aplicación que implementa el caso de uso de resolución de incidentes por el docente (LLM-S12-H02 / T2 / T4).
 * Garantiza auditoría inmutable (409 Conflict ante re-resolución), validación de motivo (20-500 chars)
 * y emisión de eventos de dominio ante falsos positivos (REVERSED).
 */
@Service
public class ModerationIncidentResolveService implements ModerationIncidentResolveUseCase {

    private final ModerationIncidentRepositoryPort incidentRepository;
    private final ModerationResolutionRepositoryPort resolutionRepository;
    private final ModerationAppealRepositoryPort appealRepository;
    private final ModerationEventPublisherPort eventPublisher;

    public ModerationIncidentResolveService(ModerationIncidentRepositoryPort incidentRepository,
                                            ModerationResolutionRepositoryPort resolutionRepository,
                                            ModerationAppealRepositoryPort appealRepository,
                                            ModerationEventPublisherPort eventPublisher) {
        this.incidentRepository = incidentRepository;
        this.resolutionRepository = resolutionRepository;
        this.appealRepository = appealRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ModerationResolutionResult resolve(ResolveIncidentCommand command) {
        Objects.requireNonNull(command, "command no puede ser nulo.");

        if (command.incidentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incident_id es obligatorio");
        }
        if (command.resolvedBy() == null || command.resolvedBy().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        // 1. Validación de motivo de resolución (20 a 500 caracteres, CA_negativo_2)
        if (command.resolutionReason() == null
                || command.resolutionReason().trim().length() < 20
                || command.resolutionReason().trim().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "must be between 20 and 500 characters");
        }

        // 2. Validación de tipo de resolución (CONFIRMED o REVERSED)
        if (command.resolution() == null || command.resolution().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resolution es obligatorio (CONFIRMED o REVERSED)");
        }
        ModerationResolutionType resolutionType;
        try {
            resolutionType = ModerationResolutionType.valueOf(command.resolution().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resolution debe ser CONFIRMED o REVERSED");
        }

        // 3. Verificación de existencia del incidente
        ModerationIncident incident = incidentRepository.findById(command.incidentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente no encontrado: " + command.incidentId()));

        // 4. Verificación de inmutabilidad / Anti-re-resolución (409 Conflict, T3)
        if (incident.isResolved() || resolutionRepository.existsByIncidentId(command.incidentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El incidente ya fue resuelto previamente y es inmutable");
        }

        // 5. Persistencia de la resolución en auditoría inmutable
        ModerationResolution resolution = ModerationResolution.create(
                command.incidentId(),
                command.resolvedBy().trim(),
                resolutionType,
                command.resolutionReason().trim()
        );
        resolutionRepository.save(resolution);

        // 6. Actualización del estado del incidente
        ModerationIncident updatedIncident = incident.resolve(resolutionType.name());
        incidentRepository.save(updatedIncident);

        // 7. Actualización del estado de la apelación si existe
        Optional<ModerationAppeal> appealOpt = appealRepository.findByIncidentId(command.incidentId());
        if (appealOpt.isPresent()) {
            ModerationAppeal appeal = appealOpt.get();
            if (resolutionType == ModerationResolutionType.CONFIRMED) {
                appealRepository.save(appeal.confirm());
            } else {
                appealRepository.save(appeal.reverse());
            }
        }

        // 8. Si es REVERSED (falso positivo), publicar evento de dominio para chat-service y notifications-service (CA5 / T4)
        if (resolutionType == ModerationResolutionType.REVERSED) {
            ModerationResolutionDomainEvent event = ModerationResolutionDomainEvent.ofReversed(
                    incident.getMessageId(),
                    incident.getId(),
                    incident.getCourseId(),
                    incident.getUserId(),
                    command.resolvedBy().trim(),
                    command.resolutionReason().trim()
            );
            eventPublisher.publishMessageUnblocked(event);
        }

        return new ModerationResolutionResult(
                resolution.getIncidentId(),
                resolution.getResolution().name(),
                resolution.getResolvedBy(),
                resolution.getResolutionReason(),
                resolution.getResolvedAt(),
                updatedIncident.getStatus()
        );
    }
}
