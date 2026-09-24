package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.ModerationResolutionResponse;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.ResolveModerationIncidentRequest;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationResolutionResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ResolveIncidentCommand;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.port.ModerationIncidentResolveUseCase;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controlador REST para la resolución de incidentes de moderación por parte del docente (LLM-S12-H02 / T2 / CA3 / CA4).
 * Expone POST /moderation/v1/incidents/{incidentId}/resolve y ${app.api.private-path}/moderation/v1/incidents/{incidentId}/resolve.
 */
@RestController
public class ModerationIncidentResolveController {

    private final ModerationCourseAuthorization courseAuthorization;
    private final ModerationIncidentRepositoryPort incidentRepository;
    private final ModerationIncidentResolveUseCase resolveUseCase;

    public ModerationIncidentResolveController(ModerationCourseAuthorization courseAuthorization,
                                               ModerationIncidentRepositoryPort incidentRepository,
                                               ModerationIncidentResolveUseCase resolveUseCase) {
        this.courseAuthorization = courseAuthorization;
        this.incidentRepository = incidentRepository;
        this.resolveUseCase = resolveUseCase;
    }

    @PostMapping({"/moderation/v1/incidents/{incidentId}/resolve", "${app.api.private-path:/api/llm}/moderation/v1/incidents/{incidentId}/resolve"})
    public ResponseEntity<ModerationResolutionResponse> resolveIncident(
            @PathVariable UUID incidentId,
            @Valid @RequestBody ResolveModerationIncidentRequest request,
            @RequestHeader(required = false) HttpHeaders headers) {

        // 1. Resolver identidad del docente (401 si no autenticado)
        String teacherUserId = courseAuthorization.resolveUserId(headers);
        if (teacherUserId == null || teacherUserId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        // 2. Validación de motivo obligatorio (20 a 500 caracteres, CA_negativo_2)
        String reason = request.effectiveReason();
        if (reason == null || reason.trim().length() < 20 || reason.trim().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "must be between 20 and 500 characters");
        }

        // 3. Buscar incidente para validar pertenencia al curso
        ModerationIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente no encontrado: " + incidentId));

        // 4. Validar autorización de pertenencia al curso (CA_negativo_1 -> 403 Forbidden)
        courseAuthorization.requireTeacherCourse(incident.getCourseId(), headers);

        // 5. Ejecutar caso de uso de resolución con auditoría inmutable
        ResolveIncidentCommand command = new ResolveIncidentCommand(
                incidentId,
                request.resolution(),
                reason.trim(),
                teacherUserId
        );
        ModerationResolutionResult result = resolveUseCase.resolve(command);

        return ResponseEntity.ok(new ModerationResolutionResponse(
                result.incidentId(),
                result.resolution(),
                result.resolvedBy(),
                result.resolutionReason(),
                result.resolvedAt(),
                result.status()
        ));
    }
}
