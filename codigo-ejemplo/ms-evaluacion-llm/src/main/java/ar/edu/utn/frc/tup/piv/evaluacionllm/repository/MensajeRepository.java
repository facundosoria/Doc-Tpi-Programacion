package ar.edu.utn.frc.tup.piv.evaluacionllm.repository;

import ar.edu.utn.frc.tup.piv.evaluacionllm.entity.MensajeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MensajeRepository extends JpaRepository<MensajeEntity, UUID> {

    List<MensajeEntity> findByConversacionIdOrderByTimestampAsc(UUID conversacionId);

    long countByConversacionId(UUID conversacionId);
}
