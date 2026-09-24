package ar.edu.utn.frc.tup.piv.llm.moderation.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data JPA para la entidad de incidentes de moderación.
 */
@Repository
public interface SpringDataModerationIncidentRepository extends JpaRepository<ModerationIncidentEntity, UUID> {

    @Query("""
        SELECT i FROM ModerationIncidentEntity i
        WHERE i.courseId = :courseId
        AND (
            (:status = 'PENDING_REVIEW' AND (i.status = 'PENDING_REVIEW' OR i.status = 'BLOCK'))
            OR (:status != 'PENDING_REVIEW' AND i.status = :status)
            OR (:status IS NULL)
        )
        ORDER BY i.createdAt DESC
    """)
    Page<ModerationIncidentEntity> findByCourseIdAndStatusFilter(
            @Param("courseId") String courseId,
            @Param("status") String status,
            Pageable pageable);

    @Query("""
        SELECT COUNT(i) FROM ModerationIncidentEntity i
        WHERE i.courseId = :courseId
        AND (
            (:status = 'PENDING_REVIEW' AND (i.status = 'PENDING_REVIEW' OR i.status = 'BLOCK'))
            OR (:status != 'PENDING_REVIEW' AND i.status = :status)
            OR (:status IS NULL)
        )
    """)
    long countByCourseIdAndStatusFilter(
            @Param("courseId") String courseId,
            @Param("status") String status);

    @Query("""
        SELECT i FROM ModerationIncidentEntity i
        WHERE i.purgedAt IS NULL
        AND i.createdAt <= :cutoff
        AND (:status IS NULL OR i.status = :status)
    """)
    java.util.List<ModerationIncidentEntity> findUnpurgedBefore(
            @Param("cutoff") java.time.OffsetDateTime cutoff,
            @Param("status") String status);

    @Query("""
        SELECT i FROM ModerationIncidentEntity i
        WHERE i.purgedAt IS NULL
        AND i.createdAt <= :cutoff
    """)
    java.util.List<ModerationIncidentEntity> findAllUnpurgedBefore(
            @Param("cutoff") java.time.OffsetDateTime cutoff);

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        UPDATE ModerationIncidentEntity i
        SET i.messagePreview = NULL, i.purgedAt = :purgedAt
        WHERE i.id IN :incidentIds
    """)
    int purgeIncidentsByIds(
            @Param("incidentIds") java.util.Collection<UUID> incidentIds,
            @Param("purgedAt") java.time.OffsetDateTime purgedAt);
}
