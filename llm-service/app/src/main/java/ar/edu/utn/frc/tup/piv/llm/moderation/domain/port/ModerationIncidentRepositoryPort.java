package ar.edu.utn.frc.tup.piv.llm.moderation.domain.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia en el dominio para consulta y registro de incidentes de moderación.
 */
public interface ModerationIncidentRepositoryPort {

    Optional<ModerationIncident> findById(UUID id);

    ModerationIncident save(ModerationIncident incident);

    java.util.List<ModerationIncident> findByCourseIdAndStatus(String courseId, String status, int offset, int limit);

    long countByCourseIdAndStatus(String courseId, String status);
}
