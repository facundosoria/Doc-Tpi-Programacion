package ar.edu.utn.frc.tup.piv.llm.moderation.application;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationIncidentSummary;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.PagedResult;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.port.ModerationIncidentQueryUseCase;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationAppealRepositoryPort;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio de aplicación para la consulta paginada de incidentes por parte del docente (LLM-S12-H02 / T1).
 * Implementa la minimización estricta de datos (preview <= 200 chars, sin historial completo).
 */
@Service
public class ModerationIncidentQueryService implements ModerationIncidentQueryUseCase {

    private final ModerationIncidentRepositoryPort incidentRepository;
    private final ModerationAppealRepositoryPort appealRepository;
    private final ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationResolutionRepositoryPort resolutionRepository;

    public ModerationIncidentQueryService(ModerationIncidentRepositoryPort incidentRepository,
                                          ModerationAppealRepositoryPort appealRepository) {
        this(incidentRepository, appealRepository, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ModerationIncidentQueryService(ModerationIncidentRepositoryPort incidentRepository,
                                          ModerationAppealRepositoryPort appealRepository,
                                          ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationResolutionRepositoryPort resolutionRepository) {
        this.incidentRepository = incidentRepository;
        this.appealRepository = appealRepository;
        this.resolutionRepository = resolutionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<ModerationIncidentSummary> getIncidents(String courseId, String status, int page, int size) {
        if (courseId == null || courseId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "course_id es obligatorio");
        }

        int effectivePage = Math.max(0, page);
        int effectiveSize = size > 0 ? Math.min(size, 50) : 20; // máx. 50 según CA1
        String effectiveStatus = (status != null && !status.isBlank()) ? status.trim() : "PENDING_REVIEW";

        int offset = effectivePage * effectiveSize;
        List<ModerationIncident> incidents = incidentRepository.findByCourseIdAndStatus(courseId.trim(), effectiveStatus, offset, effectiveSize);
        long total = incidentRepository.countByCourseIdAndStatus(courseId.trim(), effectiveStatus);

        List<ModerationIncidentSummary> summaries = new ArrayList<>();
        for (ModerationIncident incident : incidents) {
            Optional<ModerationAppeal> appealOpt = appealRepository.findByIncidentId(incident.getId());
            boolean hasAppeal = appealOpt.isPresent();
            String appealReason = appealOpt.map(ModerationAppeal::getAppealReason).orElse(null);

            summaries.add(ModerationIncidentSummary.of(
                    incident.getId(),
                    incident.getMessagePreview(),
                    incident.getReasonCode(),
                    incident.getStatus(),
                    incident.getCreatedAt(),
                    hasAppeal,
                    appealReason,
                    incident.getCourseId(),
                    incident.isPurged()
            ));
        }

        return PagedResult.of(summaries, effectivePage, effectiveSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationIncidentDetail> getIncidentById(UUID incidentId) {
        if (incidentId == null) {
            return Optional.empty();
        }
        Optional<ModerationIncident> incidentOpt = incidentRepository.findById(incidentId);
        if (incidentOpt.isEmpty()) {
            return Optional.empty();
        }
        ModerationIncident incident = incidentOpt.get();
        Optional<ModerationAppeal> appealOpt = appealRepository.findByIncidentId(incident.getId());
        boolean hasAppeal = appealOpt.isPresent();
        String appealReason = appealOpt.map(ModerationAppeal::getAppealReason).orElse(null);

        String resolutionStr = null;
        if (resolutionRepository != null) {
            resolutionStr = resolutionRepository.findByIncidentId(incident.getId())
                    .map(r -> r.getResolution().name())
                    .orElse(null);
        }
        if (resolutionStr == null) {
            resolutionStr = incident.getStatus();
        }

        String content = incident.isPurged() ? "PURGED" : incident.getMessagePreview();

        return Optional.of(new ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationIncidentDetail(
                incident.getId(),
                incident.getMessageId(),
                incident.getUserId(),
                incident.getCourseId(),
                incident.getStatus(),
                incident.getReasonCode(),
                content,
                incident.getCreatedAt(),
                incident.getPurgedAt(),
                resolutionStr,
                hasAppeal,
                appealReason
        ));
    }
}
