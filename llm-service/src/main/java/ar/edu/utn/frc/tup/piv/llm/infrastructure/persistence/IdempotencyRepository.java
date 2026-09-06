package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class IdempotencyRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  public IdempotencyRepository(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }
  public Optional<JsonNode> replay(String operation, CallerIdentity actor, UUID key, String hash) {
    try {
      jdbc.update("insert into llm.idempotency_requests (operation, caller_service, delegated_user_id, idempotency_key, request_hash) values (?, ?, ?, ?, ?)", operation, actor.serviceId(), actor.delegatedUserId(), key, hash);
      return Optional.empty();
    } catch (DuplicateKeyException exception) {
      return jdbc.query("select request_hash, response_body from llm.idempotency_requests where operation = ? and caller_service = ? and delegated_user_id = ? and idempotency_key = ?", rs -> {
        if (!rs.next()) throw exception;
        if (!hash.equals(rs.getString("request_hash"))) throw new IllegalArgumentException("La Idempotency-Key fue usada con otro payload");
        String response = rs.getString("response_body");
        if (response == null) throw new IllegalStateException("La solicitud original sigue en curso");
        return Optional.of(parse(response));
      }, operation, actor.serviceId(), actor.delegatedUserId(), key);
    }
  }
  private JsonNode parse(String response) {
    try { return mapper.readTree(response); }
    catch (Exception exception) { throw new IllegalStateException("No se pudo leer la solicitud idempotente", exception); }
  }
  public void complete(String operation, CallerIdentity actor, UUID key, UUID resourceId, JsonNode response) {
    jdbc.update("update llm.idempotency_requests set resource_id = ?, response_status = 201, response_body = ?::jsonb, completed_at = now() where operation = ? and caller_service = ? and delegated_user_id = ? and idempotency_key = ?", resourceId, response.toString(), operation, actor.serviceId(), actor.delegatedUserId(), key);
  }
}
