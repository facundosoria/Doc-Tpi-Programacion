package ar.edu.utn.frc.tup.piv.evaluacionllm.repository;

import ar.edu.utn.frc.tup.piv.evaluacionllm.entity.ConversacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversacionRepository extends JpaRepository<ConversacionEntity, UUID> {

    List<ConversacionEntity> findByCursoCohorteIdAndUsuarioRef(String cursoCohorteId, String usuarioRef);

    List<ConversacionEntity> findByCursoCohorteId(String cursoCohorteId);
}
