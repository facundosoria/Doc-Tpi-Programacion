package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ConversationRepositoryTest {

  @Test
  @DisplayName("save: ejecuta el INSERT con todos los campos de la conversación")
  void saveInsertsAllFields() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    ConversationRepository repository = new ConversationRepository(jdbc);

    UUID id = UUID.randomUUID();
    UUID courseCohortId = UUID.randomUUID();
    UUID learnerId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();
    OffsetDateTime now = OffsetDateTime.now();
    Conversation conversation = new Conversation(id, courseCohortId, learnerId, challengeId, "Mi consulta", "ABIERTA", now);

    when(jdbc.update(any(String.class), any(Object[].class))).thenReturn(1);

    Conversation saved = repository.save(conversation);

    assertThat(saved).isEqualTo(conversation);
    verify(jdbc).update(
        contains("insert into llm.conversations"),
        eq(id), eq(courseCohortId), eq(learnerId), eq(challengeId), eq("Mi consulta"), eq("ABIERTA"), eq(now));
  }

  @Test
  @DisplayName("findById: consulta la tabla llm.conversations por id")
  @SuppressWarnings("unchecked")
  void findByIdQueriesById() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    ConversationRepository repository = new ConversationRepository(jdbc);

    UUID id = UUID.randomUUID();
    Conversation expected = Conversation.nueva(UUID.randomUUID(), UUID.randomUUID(), null, "Test");

    when(jdbc.query(contains("where id = ?"), any(RowMapper.class), eq(id)))
        .thenReturn(List.of(expected));

    Optional<Conversation> found = repository.findById(id);

    assertThat(found).contains(expected);
    verify(jdbc).query(contains("where id = ?"), any(RowMapper.class), eq(id));
  }

  @Test
  @DisplayName("find: filtra por learnerId, courseCohortId y challengeId")
  @SuppressWarnings("unchecked")
  void findFiltersByAllParameters() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    ConversationRepository repository = new ConversationRepository(jdbc);

    UUID learnerId = UUID.randomUUID();
    UUID courseCohortId = UUID.randomUUID();
    UUID challengeId = UUID.randomUUID();

    when(jdbc.query(any(String.class), any(RowMapper.class), any(Object[].class)))
        .thenReturn(List.of());

    repository.find(learnerId, courseCohortId, challengeId);

    verify(jdbc).query(
        org.mockito.ArgumentMatchers.argThat((String sql) ->
            sql.contains("learner_id = ?") && sql.contains("course_cohort_id = ?")
                && sql.contains("challenge_id = ?") && sql.contains("order by created_at desc")),
        any(RowMapper.class),
        eq(learnerId), eq(courseCohortId), eq(challengeId));
  }

  @Test
  @DisplayName("find: filtra solo por learnerId si los demás son nulos")
  @SuppressWarnings("unchecked")
  void findFiltersByLearnerOnly() {
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    ConversationRepository repository = new ConversationRepository(jdbc);

    UUID learnerId = UUID.randomUUID();

    when(jdbc.query(any(String.class), any(RowMapper.class), any(Object[].class)))
        .thenReturn(List.of());

    repository.find(learnerId, null, null);

    verify(jdbc).query(
        org.mockito.ArgumentMatchers.argThat((String sql) ->
            sql.contains("learner_id = ?") && !sql.contains("course_cohort_id = ?")
                && !sql.contains("challenge_id = ?") && sql.contains("order by created_at desc")),
        any(RowMapper.class),
        eq(learnerId));
  }
}
