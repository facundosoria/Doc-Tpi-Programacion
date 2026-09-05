package ar.edu.utn.frc.tup.piv.llm.infrastructure.persistence;

import ar.edu.utn.frc.tup.piv.llm.security.CallerIdentity;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditRepository {
  private final JdbcTemplate jdbc;
  public AuditRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public void record(String action, String type, UUID resourceId, CallerIdentity actor, String details) {
    jdbc.update("insert into llm.audit_events (action, actor_service, actor_user_id, resource_type, resource_id, request_id, traceparent, details) values (?, ?, ?, ?, ?, ?, ?, ?::jsonb)", action, actor.serviceId(), actor.delegatedUserId(), type, resourceId, actor.requestId(), actor.traceparent(), details);
  }
}
