package ar.edu.utn.frc.tup.piv.llm.moderation.api;

import ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.ModerationIncidentPageResponse;
import ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.ModerationIncidentResponse;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationIncidentSummary;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.PagedResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.port.ModerationIncidentQueryUseCase;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para consultar la bandeja de incidentes de moderación docente (LLM-S12-H02 / T1 / CA1 / CA2).
 * Expone GET /moderation/v1/incidents y ${app.api.private-path}/moderation/v1/incidents con paginación y preview acotado.
 */
@RestController
public class ModerationIncidentQueryController {

    private final ModerationCourseAuthorization courseAuthorization;
    private final ModerationIncidentQueryUseCase queryUseCase;

    public ModerationIncidentQueryController(ModerationCourseAuthorization courseAuthorization,
                                             ModerationIncidentQueryUseCase queryUseCase) {
        this.courseAuthorization = courseAuthorization;
        this.queryUseCase = queryUseCase;
    }

    @GetMapping({"/moderation/v1/incidents", "${app.api.private-path:/api/llm}/moderation/v1/incidents"})
    public ResponseEntity<ModerationIncidentPageResponse> getIncidents(
            @RequestParam(name = "course_id") String courseId,
            @RequestParam(name = "status", required = false, defaultValue = "PENDING_REVIEW") String status,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            @RequestHeader(required = false) HttpHeaders headers) {

        // 1. Validar autorización de pertenencia al curso (CA_negativo_1 -> 403 Forbidden)
        courseAuthorization.requireTeacherCourse(courseId, headers);

        // 2. Consultar incidentes con preview seguro truncado a <= 200 chars (CA1, CA2)
        PagedResult<ModerationIncidentSummary> result = queryUseCase.getIncidents(courseId, status, page, size);

        // 3. Mapear a DTO de respuesta
        List<ModerationIncidentResponse> content = result.content().stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new ModerationIncidentPageResponse(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        ));
    }

    private ModerationIncidentResponse toResponse(ModerationIncidentSummary summary) {
        return new ModerationIncidentResponse(
                summary.incidentId(),
                summary.messagePreview(),
                summary.reasonCode(),
                summary.decision(),
                summary.createdAt(),
                summary.hasAppeal(),
                summary.appealReason(),
                summary.courseId(),
                summary.status()
        );
    }

    @GetMapping({"/moderation/v1/incidents/{incidentId}", "${app.api.private-path:/api/llm}/moderation/v1/incidents/{incidentId}"})
    public ResponseEntity<ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.ModerationIncidentDetailResponse> getIncidentById(
            @org.springframework.web.bind.annotation.PathVariable("incidentId") java.util.UUID incidentId,
            @RequestHeader(required = false) HttpHeaders headers) {

        var detail = queryUseCase.getIncidentById(incidentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Incidente no encontrado"));

        if (detail.courseId() != null) {
            courseAuthorization.requireTeacherCourse(detail.courseId(), headers);
        }

        return ResponseEntity.ok(new ar.edu.utn.frc.tup.piv.llm.moderation.api.dto.ModerationIncidentDetailResponse(
                detail.incidentId(),
                detail.reasonCode(),
                detail.resolution(),
                detail.createdAt(),
                detail.content(),
                detail.purgedAt(),
                detail.courseId(),
                detail.status(),
                detail.hasAppeal(),
                detail.appealReason()
        ));
    }
}
