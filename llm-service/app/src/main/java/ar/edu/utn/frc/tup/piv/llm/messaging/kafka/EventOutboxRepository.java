package ar.edu.utn.frc.tup.piv.llm.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * DAO del outbox transaccional (patrón outbox, {@code .skill-hub/kafka-event-contract-rules.md}).
 * Sigue el estilo JdbcTemplate ya usado por {@code AuditRepository}/{@code IdempotencyRepository}.
 */
@Repository
public class EventOutboxRepository {

  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;

  public EventOutboxRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
    this.jdbc = jdbc;
    this.mapper = mapper;
  }

  /** Inserta la fila del outbox. Debe llamarse dentro de la misma transacción que el cambio de estado que origina el evento. */
  public void insert(EventEnvelope<?> envelope, String topic, String messageKey, String requestId, String traceparent) {
    jdbc.update(
        "insert into llm.event_outbox "
            + "(event_id, topic, message_key, event_type, producer, occurred_at, payload, request_id, traceparent) "
            + "values (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)",
        envelope.eventId(), topic, messageKey, envelope.eventType(),
        envelope.producer(), Timestamp.from(envelope.timestamp().toInstant()), toJson(envelope.payload()),
        requestId, traceparent);
  }

  public List<OutboxRow> findUnpublished(int limit) {
    return jdbc.query(
        "select event_id, topic, message_key, event_type, producer, occurred_at, payload, "
            + "request_id, traceparent, attempts "
            + "from llm.event_outbox where published_at is null order by created_at asc limit ?",
        (rs, rowNum) -> new OutboxRow(
            (UUID) rs.getObject("event_id"),
            rs.getString("topic"),
            rs.getString("message_key"),
            rs.getString("event_type"),
            rs.getString("producer"),
            rs.getTimestamp("occurred_at").toInstant().atOffset(java.time.ZoneOffset.UTC),
            rs.getString("payload"),
            rs.getString("request_id"),
            rs.getString("traceparent"),
            rs.getInt("attempts")),
        limit);
  }

  public void markPublished(UUID eventId) {
    jdbc.update("update llm.event_outbox set published_at = now() where event_id = ?", eventId);
  }

  public void recordAttemptFailure(UUID eventId) {
    jdbc.update("update llm.event_outbox set attempts = attempts + 1 where event_id = ?", eventId);
  }

  private String toJson(Object payload) {
    try {
      return mapper.writeValueAsString(payload);
    } catch (Exception exception) {
      throw new IllegalStateException("No se pudo serializar el payload del evento", exception);
    }
  }

  /** Fila cruda del outbox, ya lista para que el relay arme el {@link EventEnvelope} y publique. */
  public record OutboxRow(UUID eventId, String topic, String messageKey, String eventType,
      String producer, OffsetDateTime occurredAt, String payloadJson, String requestId, String traceparent,
      int attempts) {
  }
}
