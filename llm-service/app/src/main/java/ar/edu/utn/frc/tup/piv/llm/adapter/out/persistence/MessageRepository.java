package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Message;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistencia JDBC directa de {@link Message}, sin puerto — ver {@link ConversationRepository}.
 * Portado de `demoLLMSpringAi/.../repository/MensajeRepository.java`. */
@Repository
public class MessageRepository implements ar.edu.utn.frc.tup.piv.llm.domain.tutor.MessageRepository {
  private final JdbcTemplate jdbc;

  public MessageRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Message save(Message message) {
    jdbc.update("insert into llm.messages (id, conversation_id, rol, contenido, created_at) values (?, ?, ?, ?, ?)",
        message.id(), message.conversationId(), message.rol(), message.contenido(), message.timestamp());
    return message;
  }

  public List<Message> findByConversationId(UUID conversationId) {
    return jdbc.query(
        "select id, conversation_id, rol, contenido, created_at from llm.messages "
            + "where conversation_id = ? order by created_at asc",
        (rs, row) -> new Message(rs.getObject("id", UUID.class), rs.getObject("conversation_id", UUID.class),
            rs.getString("rol"), rs.getString("contenido"), rs.getObject("created_at", OffsetDateTime.class)),
        conversationId);
  }
}
