package ar.edu.utn.frc.tup.piv.llm.messaging.kafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Deja en dead-letter (CA4 de LLM-EP01-H07) los eventos sin {@code eventId} o mal formados, para que
 * el consumo de la partición siga sin bloquearse. Los grupos no pueden crear tópicos
 * (KAFKA_EVENT_STANDARD.md §4), así que el dead-letter no es un tópico {@code <topic>.dlt}: es la tabla
 * {@code event_dead_letter}, de solo escritura, que se inspecciona con SQL.
 *
 * <p>Se llama dentro de la transacción del consumidor: el registro se confirma junto con la reserva del
 * {@code eventId}.
 */
@Component
public class DeadLetterPublisher {

  private static final Logger log = LoggerFactory.getLogger(DeadLetterPublisher.class);

  private final JdbcTemplate jdbc;

  public DeadLetterPublisher(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void send(String sourceTopic, String key, String rawValue, String reason) {
    log.warn("Evento enviado a dead-letter [topic={}, reason={}]", sourceTopic, reason);
    jdbc.update(
        "insert into llm.event_dead_letter (source_topic, message_key, raw_value, reason) values (?, ?, ?, ?)",
        sourceTopic, key, rawValue, reason);
  }
}
