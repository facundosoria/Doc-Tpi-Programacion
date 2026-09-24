package ar.edu.utn.frc.tup.piv.llm.moderation.application.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationIncidentSummary;
import ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.PagedResult;

/**
 * Caso de uso para consultar incidentes de moderación con paginación y preview acotado (LLM-S12-H02 / T1).
 */
public interface ModerationIncidentQueryUseCase {

    PagedResult<ModerationIncidentSummary> getIncidents(String courseId, String status, int page, int size);

    java.util.Optional<ar.edu.utn.frc.tup.piv.llm.moderation.application.dto.ModerationIncidentDetail> getIncidentById(java.util.UUID incidentId);
}
