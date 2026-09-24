package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationAppeal;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationAppealRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de persistencia que implementa {@link ModerationAppealRepositoryPort} delegando en Spring Data JPA.
 */
@Repository
public class ModerationAppealRepository implements ModerationAppealRepositoryPort {

    private final SpringDataModerationAppealRepository springDataRepository;

    public ModerationAppealRepository(SpringDataModerationAppealRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public ModerationAppeal save(ModerationAppeal appeal) {
        ModerationAppealEntity entity = toEntity(appeal);
        ModerationAppealEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ModerationAppeal> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ModerationAppeal> findByIncidentId(UUID incidentId) {
        return springDataRepository.findByIncidentId(incidentId).map(this::toDomain);
    }

    @Override
    public boolean existsByIncidentId(UUID incidentId) {
        return springDataRepository.existsByIncidentId(incidentId);
    }

    private ModerationAppealEntity toEntity(ModerationAppeal domain) {
        return new ModerationAppealEntity(
                domain.getId(),
                domain.getIncidentId(),
                domain.getUserId(),
                domain.getAppealReason(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    private ModerationAppeal toDomain(ModerationAppealEntity entity) {
        return new ModerationAppeal(
                entity.getId(),
                entity.getIncidentId(),
                entity.getUserId(),
                entity.getAppealReason(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
