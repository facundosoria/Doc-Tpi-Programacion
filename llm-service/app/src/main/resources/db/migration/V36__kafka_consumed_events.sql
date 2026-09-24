-- LLM-EP01-H07 · Esqueleto de mensajería Kafka: deduplicación del lado consumidor.
-- Todo consumidor Kafka reserva el eventId acá ANTES de procesar el efecto (CA2); si el insert
-- viola la PK (eventId repetido), el evento se reconoce como duplicado y no se reprocesa (CA3).
-- Append-only, mismo patrón que audit_events/idempotency_requests (V1).

CREATE TABLE kafka_consumed_events (
  event_id UUID PRIMARY KEY,
  topic VARCHAR(120) NOT NULL,
  event_type VARCHAR(100),
  consumer_group VARCHAR(120) NOT NULL,
  processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TRIGGER kafka_consumed_events_append_only
  BEFORE UPDATE OR DELETE ON kafka_consumed_events
  FOR EACH ROW EXECUTE FUNCTION prevent_mutation();
