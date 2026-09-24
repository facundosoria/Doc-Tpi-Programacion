package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data JPA para la entidad de apelaciones de moderación.
 */
@Repository
public interface SpringDataModerationAppealRepository extends JpaRepository<ModerationAppealEntity, UUID> {

    Optional<ModerationAppealEntity> findByIncidentId(UUID incidentId);

    boolean existsByIncidentId(UUID incidentId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("""
        UPDATE ModerationAppealEntity a
        SET a.appealReason = NULL, a.updatedAt = :now
        WHERE a.incidentId IN :incidentIds
    """)
    int purgeAppealsByIncidentIds(
            @org.springframework.data.repository.query.Param("incidentIds") java.util.Collection<UUID> incidentIds,
            @org.springframework.data.repository.query.Param("now") java.time.OffsetDateTime now);
}
