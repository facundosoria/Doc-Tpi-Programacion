package ar.edu.utn.frc.tup.piv.llm.domain.tutor;

import java.util.List;
import java.util.UUID;

/** Puerto de repositorio de dominio para los mensajes de una conversación. */
public interface MessageRepository {

  Message save(Message message);

  List<Message> findByConversationId(UUID conversationId);
}
