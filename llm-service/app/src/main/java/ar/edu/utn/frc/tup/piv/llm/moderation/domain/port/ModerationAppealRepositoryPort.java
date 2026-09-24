package ar.edu.utn.frc.tup.piv.llm.moderation.domain.port;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de persistencia en el dominio para el ciclo de vida de apelaciones (ADR-019).
 */
public interface ModerationAppealRepositoryPort {

    ModerationAppeal save(ModerationAppeal appeal);

    Optional<ModerationAppeal> findById(UUID id);

    Optional<ModerationAppeal> findByIncidentId(UUID incidentId);

    boolean existsByIncidentId(UUID incidentId);
}
