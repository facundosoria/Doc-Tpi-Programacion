package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data JPA para la política de retención de evidencia de moderación (LLM-S13-H02 / T1).
 */
@Repository
public interface SpringDataModerationRetentionPolicyRepository extends JpaRepository<ModerationRetentionPolicyEntity, UUID> {

    Optional<ModerationRetentionPolicyEntity> findByIncidentTypeAndSeverity(String incidentType, String severity);

    Optional<ModerationRetentionPolicyEntity> findByIncidentType(String incidentType);
}
