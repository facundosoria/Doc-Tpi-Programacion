package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data JPA para la entidad de resoluciones de moderación.
 */
@Repository
public interface SpringDataModerationResolutionRepository extends JpaRepository<ModerationResolutionEntity, UUID> {

    Optional<ModerationResolutionEntity> findByIncidentId(UUID incidentId);

    boolean existsByIncidentId(UUID incidentId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        UPDATE ModerationResolutionEntity r
        SET r.resolutionReason = NULL
        WHERE r.incidentId IN :incidentIds
    """)
    int purgeResolutionsByIncidentIds(
            @org.springframework.data.repository.query.Param("incidentIds") java.util.Collection<UUID> incidentIds);
}
