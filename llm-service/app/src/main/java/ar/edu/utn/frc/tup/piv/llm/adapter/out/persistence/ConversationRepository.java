package ar.edu.utn.frc.tup.piv.llm.adapter.out.persistence;

import ar.edu.utn.frc.tup.piv.llm.domain.tutor.Conversation;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistencia JDBC directa de {@link Conversation}, sin puerto — mismo estilo que el resto de
 * los repositorios del servicio ({@link FunctionModelConfigRepository}, {@link AuditRepository}):
 * no hay JPA en `llm-service`. Portado de `demoLLMSpringAi/.../repository/ConversacionRepository.java`
 * (EP-05, ver `docs/estado-implementacion/ep-05/conversations.md`). */
@Repository
public class ConversationRepository implements ar.edu.utn.frc.tup.piv.llm.domain.tutor.ConversationRepository {
  private final JdbcTemplate jdbc;

  public ConversationRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Conversation save(Conversation conversation) {
    jdbc.update(
        "insert into llm.conversations (id, course_cohort_id, learner_id, challenge_id, titulo, estado, created_at) "
            + "values (?, ?, ?, ?, ?, ?, ?)",
        conversation.id(), conversation.courseCohortId(), conversation.learnerId(), conversation.challengeId(),
        conversation.titulo(), conversation.estado(), conversation.createdAt());
    return conversation;
  }

  public Optional<Conversation> findById(UUID id) {
    return jdbc
        .query("select id, course_cohort_id, learner_id, challenge_id, titulo, estado, created_at "
            + "from llm.conversations where id = ?", (rs, row) -> new Conversation(
                rs.getObject("id", UUID.class), rs.getObject("course_cohort_id", UUID.class),
                rs.getObject("learner_id", UUID.class), rs.getObject("challenge_id", UUID.class),
                rs.getString("titulo"), rs.getString("estado"), rs.getObject("created_at", OffsetDateTime.class)),
            id)
        .stream()
        .findFirst();
  }

  @Override
  public List<Conversation> find(UUID learnerId, UUID courseCohortId) {
    return find(learnerId, courseCohortId, null);
  }

  @Override
  public List<Conversation> find(UUID learnerId, UUID courseCohortId, UUID challengeId) {
    StringBuilder sql = new StringBuilder(
        "select id, course_cohort_id, learner_id, challenge_id, titulo, estado, created_at from llm.conversations where 1 = 1");
    List<Object> params = new ArrayList<>();
    if (learnerId != null) {
      sql.append(" and learner_id = ?");
      params.add(learnerId);
    }
    if (courseCohortId != null) {
      sql.append(" and course_cohort_id = ?");
      params.add(courseCohortId);
    }
    if (challengeId != null) {
      sql.append(" and challenge_id = ?");
      params.add(challengeId);
    }
    sql.append(" order by created_at desc");
    return jdbc.query(sql.toString(), (rs, row) -> new Conversation(
        rs.getObject("id", UUID.class), rs.getObject("course_cohort_id", UUID.class),
        rs.getObject("learner_id", UUID.class), rs.getObject("challenge_id", UUID.class),
        rs.getString("titulo"), rs.getString("estado"), rs.getObject("created_at", OffsetDateTime.class)),
        params.toArray());
  }
}
