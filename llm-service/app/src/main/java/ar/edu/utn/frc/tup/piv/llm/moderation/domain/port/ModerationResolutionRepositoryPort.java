package ar.edu.utn.frc.tup.piv.llm.moderation.domain.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolution;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia en el dominio para el ciclo de vida y auditoría de resoluciones (ADR-019).
 */
public interface ModerationResolutionRepositoryPort {

    ModerationResolution save(ModerationResolution resolution);

    Optional<ModerationResolution> findByIncidentId(UUID incidentId);

    boolean existsByIncidentId(UUID incidentId);
}
