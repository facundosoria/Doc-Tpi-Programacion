package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.moderation.domain.ModerationIncident;
import ar.edu.utn.frc.tup.piv.llm.moderation.domain.port.ModerationIncidentRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Adaptador de persistencia que implementa {@link ModerationIncidentRepositoryPort} delegando en Spring Data JPA.
 */
@Repository
public class ModerationIncidentRepository implements ModerationIncidentRepositoryPort {

    private final SpringDataModerationIncidentRepository springDataRepository;

    public ModerationIncidentRepository(SpringDataModerationIncidentRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<ModerationIncident> findById(UUID id) {
        return springDataRepository.findById(id).map(this::toDomain);
    }

    @Override
    public ModerationIncident save(ModerationIncident incident) {
        ModerationIncidentEntity entity = toEntity(incident);
        ModerationIncidentEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public java.util.List<ModerationIncident> findByCourseIdAndStatus(String courseId, String status, int offset, int limit) {
        int pageSize = limit > 0 ? limit : 20;
        int pageNumber = offset > 0 ? offset / pageSize : 0;
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(
                pageNumber,
                pageSize,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );
        return springDataRepository.findByCourseIdAndStatusFilter(courseId, status, pageRequest)
                .map(this::toDomain)
                .getContent();
    }

    @Override
    public long countByCourseIdAndStatus(String courseId, String status) {
        return springDataRepository.countByCourseIdAndStatusFilter(courseId, status);
    }

    private ModerationIncidentEntity toEntity(ModerationIncident domain) {
        return new ModerationIncidentEntity(
                domain.getId(),
                domain.getMessageId(),
                domain.getUserId(),
                domain.getCourseId(),
                domain.getStatus(),
                domain.getReasonCode(),
                domain.getMessagePreview(),
                domain.getCreatedAt(),
                domain.getPurgedAt()
        );
    }

    private ModerationIncident toDomain(ModerationIncidentEntity entity) {
        return new ModerationIncident(
                entity.getId(),
                entity.getMessageId(),
                entity.getUserId(),
                entity.getCourseId(),
                entity.getStatus(),
                entity.getReasonCode(),
                entity.getMessagePreview(),
                entity.getCreatedAt(),
                entity.getPurgedAt()
        );
    }
}
