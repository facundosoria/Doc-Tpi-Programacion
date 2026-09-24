package ar.edu.utn.frc.tup.piv.llm.messaging.kafka.consumer;

import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * DAO de deduplicación del lado consumidor (CA2/CA3 de LLM-EP01-H07). Reservar el {@code eventId}
 * ANTES de aplicar el efecto: si ya existe, el insert falla por PK y el llamador sabe que es un
 * duplicado sin necesidad de reprocesar nada.
 */
@Repository
public class KafkaConsumedEventsRepository {

  private final JdbcTemplate jdbc;

  public KafkaConsumedEventsRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** @return true si el eventId era nuevo y quedó reservado; false si ya estaba procesado (duplicado). */
  public boolean tryReserve(UUID eventId, String topic, String eventType, String consumerGroup) {
    try {
      jdbc.update(
          "insert into llm.kafka_consumed_events (event_id, topic, event_type, consumer_group) values (?, ?, ?, ?)",
          eventId, topic, eventType, consumerGroup);
      return true;
    } catch (DuplicateKeyException alreadyProcessed) {
      return false;
    }
  }
}
