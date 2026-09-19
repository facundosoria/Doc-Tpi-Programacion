-- LLM-EP01-H07 · Esqueleto de mensajería Kafka: outbox transaccional para productores.
-- Un evento se inserta en la misma transacción que el cambio de estado que lo origina (patrón
-- Transactional Outbox); un relay asíncrono (EventOutboxRelay) lo publica a Kafka y marca
-- published_at. Evita el "dual write" (guardar en DB y publicar en Kafka sin atomicidad).

CREATE TABLE event_outbox (
  event_id UUID PRIMARY KEY,
  topic VARCHAR(120) NOT NULL,
  message_key VARCHAR(200) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  event_version SMALLINT NOT NULL,
  producer VARCHAR(120) NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  payload JSONB NOT NULL,
  request_id VARCHAR(200),
  traceparent VARCHAR(200),
  attempts SMALLINT NOT NULL DEFAULT 0,
  published_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- El relay hace polling de filas no publicadas ordenadas por antigüedad.
CREATE INDEX event_outbox_unpublished_idx ON event_outbox (created_at) WHERE published_at IS NULL;
