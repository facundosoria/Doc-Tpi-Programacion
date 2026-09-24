package ar.edu.utn.frc.tup.piv.llm.domain.tutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de repositorio de dominio para la raíz de agregado Conversation. */
public interface ConversationRepository {

  Conversation save(Conversation conversation);

  Optional<Conversation> findById(UUID id);

  default List<Conversation> find(UUID learnerId, UUID courseCohortId) {
    return find(learnerId, courseCohortId, null);
  }

  List<Conversation> find(UUID learnerId, UUID courseCohortId, UUID challengeId);
}
