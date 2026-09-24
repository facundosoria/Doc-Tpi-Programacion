package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationResolution;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationResolutionRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de persistencia que implementa {@link ModerationResolutionRepositoryPort} delegando en Spring Data JPA.
 */
@Repository
public class ModerationResolutionRepository implements ModerationResolutionRepositoryPort {

    private final SpringDataModerationResolutionRepository springDataRepository;

    public ModerationResolutionRepository(SpringDataModerationResolutionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public ModerationResolution save(ModerationResolution resolution) {
        ModerationResolutionEntity entity = toEntity(resolution);
        ModerationResolutionEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ModerationResolution> findByIncidentId(UUID incidentId) {
        return springDataRepository.findByIncidentId(incidentId).map(this::toDomain);
    }

    @Override
    public boolean existsByIncidentId(UUID incidentId) {
        return springDataRepository.existsByIncidentId(incidentId);
    }

    private ModerationResolutionEntity toEntity(ModerationResolution domain) {
        return new ModerationResolutionEntity(
                domain.getId(),
                domain.getIncidentId(),
                domain.getResolvedBy(),
                domain.getResolution(),
                domain.getResolutionReason(),
                domain.getResolvedAt()
        );
    }

    private ModerationResolution toDomain(ModerationResolutionEntity entity) {
        return new ModerationResolution(
                entity.getId(),
                entity.getIncidentId(),
                entity.getResolvedBy(),
                entity.getResolution(),
                entity.getResolutionReason(),
                entity.getResolvedAt()
        );
    }
}
