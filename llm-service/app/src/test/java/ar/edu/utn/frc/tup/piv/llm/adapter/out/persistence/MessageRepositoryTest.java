package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Message;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class MessageRepositoryTest {

  @Test
  @DisplayName("save: ejecuta el INSERT con todos los campos del mensaje")
  void saveInsertsAllFields() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    MessageRepository repository = new MessageRepository(jdbc);

    UUID id = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();
    Message message = new Message(id, conversationId, "alumno", "¿Cómo resuelvo este problema?", now);

    when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);

    Message saved = repository.save(message);

    assertThat(saved).isEqualTo(message);
    verify(jdbc).update(
        contains("insert into llm.messages"),
        eq(id), eq(conversationId), eq("alumno"), eq("¿Cómo resuelvo este problema?"), eq(now));
  }

  @Test
  @DisplayName("findByConversationId: consulta la tabla llm.messages ordenado cronológicamente")
  @SuppressWarnings("unchecked")
  void findByConversationIdQueriesChronologically() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    MessageRepository repository = new MessageRepository(jdbc);

    UUID conversationId = UUID.randomUUID();
    Message m1 = new Message(UUID.randomUUID(), conversationId, "alumno", "hola", OffsetDateTime.now());
    Message m2 = new Message(UUID.randomUUID(), conversationId, "tutor", "¡Hola! ¿En qué te ayudo?", OffsetDateTime.now());

    when(jdbc.query(contains("where conversation_id = ? order by created_at asc"), any(RowMapper.class), eq(conversationId)))
        .thenReturn(List.of(m1, m2));

    List<Message> messages = repository.findByConversationId(conversationId);

    assertThat(messages).containsExactly(m1, m2);
    verify(jdbc).query(
        contains("where conversation_id = ? order by created_at asc"),
        any(RowMapper.class),
        eq(conversationId));
  }
}
