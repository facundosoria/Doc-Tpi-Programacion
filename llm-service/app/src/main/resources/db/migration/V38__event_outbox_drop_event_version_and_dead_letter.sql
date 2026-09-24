-- ADR-020 · Estándar Kafka del PDF KAFKA.pdf (docsV2/contracts/KAFKA_EVENT_STANDARD.md).
--
-- 1) El envelope del PDF no tiene eventVersion: se quita la columna del outbox. Los eventos ya
--    publicados no se reescriben; los pendientes de publicar salen con el envelope nuevo.
-- 2) Los grupos no pueden crear tópicos, así que el dead-letter deja de ser un tópico <topic>.dlt:
--    los mensajes rechazados por el consumidor (JSON roto, sin eventId, payload incompleto) quedan
--    acá, con el motivo. Append-only, mismo patrón que kafka_consumed_events (V30).

ALTER TABLE event_outbox DROP COLUMN event_version;

CREATE TABLE event_dead_letter (
  id BIGSERIAL PRIMARY KEY,
  source_topic VARCHAR(120) NOT NULL,
  message_key VARCHAR(200),
  raw_value TEXT,
  reason TEXT NOT NULL,
  received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER event_dead_letter_append_only
  BEFORE UPDATE OR DELETE ON event_dead_letter
  FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
